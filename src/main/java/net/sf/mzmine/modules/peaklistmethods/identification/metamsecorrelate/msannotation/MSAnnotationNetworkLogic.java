package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class MSAnnotationNetworkLogic {
  private static final Logger LOG = Logger.getLogger(MSAnnotationNetworkLogic.class.getName());


  /**
   * Show the annotation with the highest numbers of links. Prefers charge state.
   * 
   * @param mseGroupedPeakList
   * @param g can be null. can be used to limit the number of links
   */
  public static void showMostlikelyAnnotations(PeakList pkl) {
    for (PeakListRow row : pkl.getRows()) {
      ESIAdductIdentity best = getMostLikelyAnnotation(row, null);
      // set best
      if (best != null)
        row.setPreferredPeakIdentity(best);
    }
  }

  /**
   * Show the annotation with the highest numbers of links. Prefers charge state.
   * 
   * @param mseGroupedPeakList
   * @param g can be null. can be used to limit the number of links
   */
  public static void showMostlikelyAnnotations(MSEGroupedPeakList pkl, boolean useGroups) {
    for (PeakListRow row : pkl.getRows()) {
      ESIAdductIdentity best = getMostLikelyAnnotation(row, useGroups ? pkl.getGroup(row) : null);
      // set best
      if (best != null)
        row.setPreferredPeakIdentity(best);
    }
  }

  /**
   * 
   * @param row
   * @param g can be null. can be used to limit the number of links
   * @return
   */
  public static ESIAdductIdentity getMostLikelyAnnotation(PeakListRow row, PKLRowGroup g) {
    ESIAdductIdentity best = null;
    int maxLinks = 0;
    for (PeakIdentity id : row.getPeakIdentities()) {
      if (id instanceof ESIAdductIdentity) {
        ESIAdductIdentity esi = (ESIAdductIdentity) id;
        int links = getLinksTo(esi, g);
        if (best == null)
          best = esi;
        // keep if has M>1 and was identified by MSMS
        else if (compareMSMSMolIdentity(esi, best))
          continue;
        // always if M>1 backed by MSMS
        else if (compareMSMSMolIdentity(best, esi))
          best = esi;
        else if (links == maxLinks
            && (compareCharge(best, esi) || (isTheUnmodified(best) && !isTheUnmodified(esi)))) {
          best = esi;
        } else if (links > maxLinks) {
          maxLinks = links;
          best = esi;
        }
      }
    }
    return best;
  }

  /**
   * 
   * @param best
   * @param esi
   * @return onyl true if best so far was not verified by MSMS and esi was verified
   */
  private static boolean compareMSMSMolIdentity(ESIAdductIdentity best, ESIAdductIdentity esi) {
    if (best.getMSMSMultimerCount() == 0 && esi.getMSMSMultimerCount() > 0)
      return true;
    else
      return false;
  }

  /**
   * 
   * @param row
   * @param g can be null. can be used to limit the number of links
   * @return
   */
  public static int getLinksTo(ESIAdductIdentity esi, PKLRowGroup g) {
    // TODO change to real links after refinement
    if (g == null)
      return esi.getPartnerRowsID().length;
    else {
      int c = 0;
      for (int id : esi.getPartnerRowsID())
        if (g.contains(id))
          c++;

      return c;
    }
  }



  /**
   * Checks if this identity is the unmodified reference. e.g., for [M (unmodified)] --> [M+H]
   * 
   * @param a
   * @return
   */
  private static boolean isTheUnmodified(ESIAdductIdentity a) {
    return a.getA().equals(ESIAdductType.M_UNMODIFIED);
  }


  /**
   * 
   * @param a
   * @param b
   * @return True if b is a better choice
   */
  private static boolean compareCharge(ESIAdductIdentity a, ESIAdductIdentity b) {
    int ca = a.getA().getAbsCharge();
    int cb = b.getA().getAbsCharge();
    return cb != 0 // a is better if b is uncharged
        && ((ca == 0 && cb > 0) // b is better if charged and a uncharged
            || (ca > cb)); // b is better if charge is lower
  }


  /**
   * Create list of AnnotationNetworks and set net ID
   * 
   * @param rows
   * @return
   */
  public static List<AnnotationNetwork> createAnnotationNetworks(PeakList pkl,
      boolean addNetworkNumber) {
    return createAnnotationNetworks(pkl.getRows(), addNetworkNumber);
  }

  /**
   * Create list of AnnotationNetworks and set net ID
   * 
   * @param rows
   * @return
   */
  public static List<AnnotationNetwork> createAnnotationNetworks(PeakListRow[] rows,
      boolean addNetworkNumber) {
    List<AnnotationNetwork> nets = new ArrayList<>();

    if (rows != null) {
      // sort by rt
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

      AnnotationNetwork current = new AnnotationNetwork(nets.size());
      // add all connections
      for (PeakListRow row : rows) {
        current.clear();
        boolean isNewNet = addRow(current, row, rows, row.getID());
        if (isNewNet && current.size() > 1) {
          // LOG.info("Add network " + current.getID() + " with n=" + current.size());
          // add
          nets.add(current);
          // add network number to annotations
          if (addNetworkNumber) {
            for (Iterator iterator = current.iterator(); iterator.hasNext();) {
              PeakListRow r = (PeakListRow) iterator.next();
              for (PeakIdentity pi : r.getPeakIdentities()) {
                // identity by ms annotation module
                if (pi instanceof ESIAdductIdentity) {
                  ESIAdductIdentity adduct = (ESIAdductIdentity) pi;
                  adduct.setNetID(current.getID());
                }
              }
            }
          }
          // new
          current = new AnnotationNetwork(nets.size());
        }
      }
    }
    return nets;
  }

  /**
   * Add all rows of a network
   * 
   * @param current
   * @param row
   * @param rows
   * @return false if this network has already been created
   */
  private static boolean addRow(AnnotationNetwork current, PeakListRow row, PeakListRow[] rows,
      int masterID) {
    for (PeakIdentity pi : row.getPeakIdentities()) {
      // identity by ms annotation module
      if (pi instanceof ESIAdductIdentity) {
        ESIAdductIdentity adduct = (ESIAdductIdentity) pi;

        // try to add all
        if (current.isEmpty())
          current.add(row);

        // add all connection for ids>rowID
        int[] ids = adduct.getPartnerRowsID();
        for (int id : ids) {
          if (id != masterID) {
            if (id > masterID) {
              PeakListRow row2 = findRowByID(id, rows);
              // new row found?
              if (row2 != null && !current.contains(row2)) {
                current.add(row2);
                boolean isNewNet = addRow(current, row2, rows, masterID);
                if (!isNewNet)
                  return false;
              }
            } else {
              // id was smaller - trash this network, its already added
              return false;
            }
          }
        }
      }
    }
    // is new network
    return true;
  }

  public static PeakListRow findRowByID(int id, PeakListRow[] rows) {
    if (rows == null)
      return null;
    else {
      for (PeakListRow r : rows)
        if (r.getID() == id)
          return r;

      return null;
    }
  }

  /**
   * All MS annotation connections
   * 
   * @return
   */
  public static List<PeakListRow> findAllAnnotationConnections(PeakListRow[] rows,
      PeakListRow row) {
    List<PeakListRow> connections = new ArrayList<>();

    for (PeakIdentity pi : row.getPeakIdentities()) {
      // identity by ms annotation module
      if (pi instanceof ESIAdductIdentity) {
        ESIAdductIdentity adduct = (ESIAdductIdentity) pi;

        // add all connection
        int[] ids = adduct.getPartnerRowsID();
        for (int id : ids) {
          PeakListRow row2 = findRowByID(id, rows);
          connections.add(row2);
        }
      }
    }
    return connections;
  }

  /**
   * 
   * @param row
   * @return list of annotations or an empty list
   */
  public static List<ESIAdductIdentity> getAllAnnotations(PeakListRow row) {
    List<ESIAdductIdentity> ident = new ArrayList<>();
    for (PeakIdentity pi : row.getPeakIdentities()) {
      if (pi instanceof ESIAdductIdentity)
        ident.add((ESIAdductIdentity) pi);
    }
    return ident;
  }

  /**
   * apply operation for each id
   * 
   * @param row
   * @param op
   */
  public static void forEachAnnotation(PeakListRow row, Consumer<ESIAdductIdentity> op) {
    for (PeakIdentity pi : row.getPeakIdentities()) {
      if (pi instanceof ESIAdductIdentity)
        op.accept((ESIAdductIdentity) pi);
    }
  }

}
