package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductType;
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
   * @param useGroup searches for a correlation group or null
   * @return Most likely annotation or null if none present
   */
  public static ESIAdductIdentity getMostLikelyAnnotation(PeakListRow row, boolean useGroup) {
    return getMostLikelyAnnotation(row, useGroup ? PKLRowGroup.from(row) : null);
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
        if (best == null || best.getA().equals(ESIAdductType.M_UNMODIFIED))
          best = esi;
        else if (esi.getA().equals(ESIAdductType.M_UNMODIFIED))
          continue;
        // keep if has M>1 and was identified by MSMS
        else if (compareMSMSMolIdentity(esi, best))
          continue;
        // always if M>1 backed by MSMS
        else if (compareMSMSMolIdentity(best, esi))
          best = esi;
        // keep if insource fragment verified by MSMS
        else if (compareMSMSNeutralLossIdentity(esi, best))
          continue;
        // keep if insource fragment verified by MSMS
        else if (compareMSMSNeutralLossIdentity(best, esi))
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
   * @param best
   * @param esi
   * @return onyl true if best was not verified by MSMS and and esi is
   */
  private static boolean compareMSMSNeutralLossIdentity(ESIAdductIdentity best,
      ESIAdductIdentity esi) {
    if (best.getMSMSModVerify() == 0 && esi.getMSMSModVerify() > 0)
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
    return createAnnotationNetworksOld(pkl.getRows(), addNetworkNumber);
  }

  /**
   * Create list of AnnotationNetworks and set net ID Method 1 ALl edges
   * 
   * @param rows
   * @return
   */
  public static List<AnnotationNetwork> createAnnotationNetworksOld(PeakListRow[] rows,
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
            for (Iterator iterator = current.keySet().iterator();; iterator.hasNext()) {
              PeakListRow r = (PeakListRow) iterator.next();
              for (PeakIdentity pi : r.getPeakIdentities()) {
                // identity by ms annotation module
                if (pi instanceof ESIAdductIdentity) {
                  ESIAdductIdentity adduct = (ESIAdductIdentity) pi;
                  adduct.setNetwork(current);
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
   * Method 2: all that point to the same molecule (even without edge)
   * 
   * @param rows
   * @return
   */
  public static List<AnnotationNetwork> createAnnotationNetworks(PeakListRow[] rows) {

    // bin neutral masses to annotation networks
    Collection<AnnotationNetwork> nets = binNeutralMassToNetworks(rows);

    // add network to all identities
    setNetworksToAllAnnotations(nets);

    // fill in neutral losses [M-H2O] is not iserted yet
    // they might be if [M+H2O+X]+ was also annotated by another link
    fillInNeutralLosses(rows, nets);

    return new ArrayList<AnnotationNetwork>(nets);
  }


  /**
   * fill in neutral losses [M-H2O] is not iserted yet. they might be if [M+H2O+X]+ was also
   * annotated by another link
   * 
   * @param rows
   * @param nets
   */
  private static void fillInNeutralLosses(PeakListRow[] rows, Collection<AnnotationNetwork> nets) {
    for (PeakListRow row : rows) {
      for (PeakIdentity pi : row.getPeakIdentities()) {
        // identity by ms annotation module
        if (pi instanceof ESIAdductIdentity) {
          ESIAdductIdentity neutral = (ESIAdductIdentity) pi;
          // only if charged (neutral losses do not point to the real neutral mass)
          if (neutral.getA().getAbsCharge() != 0)
            continue;

          // all partners
          int[] partnerIDs = neutral.getPartnerRowsID();
          for (int p : partnerIDs) {
            PeakListRow partner = findRowByID(p, rows);
            if (partner == null)
              continue;

            AnnotationNetwork[] partnerNets = MSAnnotationNetworkLogic.getAllNetworks(partner);
            // create new net if partner was in no network
            if (partnerNets == null || partnerNets.length == 0) {
              // create new and put both
              AnnotationNetwork newNet = new AnnotationNetwork(nets.size());
              nets.add(newNet);
              newNet.put(row, neutral);
              newNet.put(partner, ESIAdductIdentity.getIdentityOf(partner, row));
              newNet.setNetworkToAllRows();
            } else {
              // add neutral loss to nets
              // do not if its already in this network (e.g. as adduct)
              Arrays.stream(partnerNets).filter(pnet -> !pnet.containsKey(row))
                  .forEach(pnet -> pnet.put(row, neutral));
            }
          }
        }
      }
    }
  }

  /**
   * All annotation networks of all annotations of row
   * 
   * @param row
   * @return
   */
  public static AnnotationNetwork[] getAllNetworks(PeakListRow row) {
    return MSAnnotationNetworkLogic.getAllAnnotations(row).stream().map(id -> id.getNetwork())
        .toArray(AnnotationNetwork[]::new);
  }

  /**
   * Set the network to all its children rows
   * 
   * @param nets
   */
  public static void setNetworksToAllAnnotations(Collection<AnnotationNetwork> nets) {
    nets.stream().forEach(n -> n.setNetworkToAllRows());
  }

  /**
   * Binning of all neutral masses described by all annotations of rows with 0.1 Da binning width
   * (masses should be very different)
   * 
   * @param rows
   * @return AnnotationNetworks
   */
  private static Collection<AnnotationNetwork> binNeutralMassToNetworks(PeakListRow[] rows) {
    Map<Integer, AnnotationNetwork> map = new HashMap<>();
    for (PeakListRow row : rows) {
      for (PeakIdentity pi : row.getPeakIdentities()) {
        // identity by ms annotation module
        if (pi instanceof ESIAdductIdentity) {
          ESIAdductIdentity adduct = (ESIAdductIdentity) pi;
          // only if charged (neutral losses do not point to the real neutral mass)
          if (adduct.getA().getAbsCharge() == 0)
            continue;

          double mass = adduct.getA().getMass(row.getAverageMZ());
          // bin to 0.1
          Integer nmass = (int) Math.round(mass * 10.0);

          AnnotationNetwork net = map.get(nmass);
          if (net == null) {
            // create new
            net = new AnnotationNetwork(map.size());
            map.put(nmass, net);
          }
          // add row and id to network
          net.put(row, adduct);
        }
      }
    }
    return map.values();
  }

  /**
   * Neutral mass of AnnotationNetwork entry (ion and peaklistrow)
   * 
   * @param e
   * @return
   */
  public static double calcMass(Entry<PeakListRow, ESIAdductIdentity> e) {
    return e.getValue().getA().getMass(e.getKey().getAverageMZ());
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
          current.put(row, adduct);

        // add all connection for ids>rowID
        int[] ids = adduct.getPartnerRowsID();
        for (int id : ids) {
          if (id != masterID) {
            if (id > masterID) {
              PeakListRow row2 = findRowByID(id, rows);
              ESIAdductIdentity adduct2 = ESIAdductIdentity.getIdentityOf(row2, row);
              // new row found?
              if (row2 != null && !current.containsKey(row2)) {
                current.put(row2, adduct2);
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
