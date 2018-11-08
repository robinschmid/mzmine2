package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
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
    for (PeakIdentity id : row.getPeakIdentities()) {
      if (id instanceof ESIAdductIdentity) {
        ESIAdductIdentity esi = (ESIAdductIdentity) id;
        int compare = compareRows(best, esi, g);
        if (compare < 0)
          best = esi;
      }
    }
    return best;
  }


  /**
   * 
   * @param row
   * @param g can be null. can be used to limit the number of links
   * @return
   */
  public static boolean hasIonAnnotation(PeakListRow row) {
    for (PeakIdentity id : row.getPeakIdentities()) {
      if (id instanceof ESIAdductIdentity) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param best
   * @param esi
   * @return -1 if esi is better than best 1 if opposite
   */
  public static int compareRows(ESIAdductIdentity best, ESIAdductIdentity esi, PKLRowGroup g) {
    if (best == null || best.getA().equals(ESIAdductType.M_UNMODIFIED))
      return -1;
    else if (esi.getA().equals(ESIAdductType.M_UNMODIFIED))
      return 1;
    // size of network (ions pointing to the same neutral mass)
    else if (esi.getNetwork() != null
        && (best.getNetwork() == null || esi.getNetwork().size() > best.getNetwork().size()))
      return -1;
    // keep if has M>1 and was identified by MSMS
    else if (compareMSMSMolIdentity(esi, best))
      return 1;
    // always if M>1 backed by MSMS
    else if (compareMSMSMolIdentity(best, esi))
      return -1;
    // keep if insource fragment verified by MSMS
    else if (compareMSMSNeutralLossIdentity(esi, best))
      return 1;
    // keep if insource fragment verified by MSMS
    else if (compareMSMSNeutralLossIdentity(best, esi))
      return -1;

    int esiLinks = getLinksTo(esi, g);
    int bestLinks = getLinksTo(best, g);
    if (esiLinks == bestLinks
        && (compareCharge(best, esi) || (isTheUnmodified(best) && !isTheUnmodified(esi)))) {
      return -1;
    } else if (esiLinks > bestLinks) {
      return -1;
    }
    return 1;
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
   * @param groups
   * 
   * @param rows
   * @return
   */
  public static List<AnnotationNetwork> createAnnotationNetworks(PeakList pkl,
      MZTolerance mzTolerance, boolean useGrouping) {
    return createAnnotationNetworks(pkl.getRows(), mzTolerance, useGrouping);
  }

  /**
   * Create list of AnnotationNetworks and set net ID Method 1 ALl edges
   * 
   * @param rows
   * @return
   */
  public static List<AnnotationNetwork> createAnnotationNetworksOld(PeakListRow[] rows,
      boolean addNetworkNumber, MZTolerance mzTolerance) {
    List<AnnotationNetwork> nets = new ArrayList<>();

    if (rows != null) {
      // sort by rt
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

      AnnotationNetwork current = new AnnotationNetwork(mzTolerance, nets.size());
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
          current = new AnnotationNetwork(mzTolerance, nets.size());
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
  public static List<AnnotationNetwork> createAnnotationNetworks(PeakListRow[] rows,
      MZTolerance mzTolerance, boolean useGrouping) {

    // bin neutral masses to annotation networks
    List<AnnotationNetwork> nets = new ArrayList<>(binNeutralMassToNetworks(rows, mzTolerance));

    // split by groups
    if (useGrouping) {
      splitByGroups(nets);
    }

    // add network to all identities
    setNetworksToAllAnnotations(nets);

    // fill in neutral losses [M-H2O] is not iserted yet
    // they might be if [M+H2O+X]+ was also annotated by another link
    fillInNeutralLosses(rows, nets, mzTolerance);

    resetNetworkIDs(nets);
    return nets;
  }


  public static void resetNetworkIDs(List<AnnotationNetwork> nets) {
    for (int i = 0; i < nets.size(); i++) {
      nets.get(i).setID(i);
    }
  }

  /**
   * Need to reset networks to annotations afterwards
   * 
   * @param nets
   */
  private static void splitByGroups(List<AnnotationNetwork> nets) {
    int size = nets.size();
    for (int i = 0; i < size; i++) {
      AnnotationNetwork net = nets.get(i);
      if (!net.allSameCorrGroup()) {
        nets.addAll(splitByGroup(net));
        nets.remove(i);
        i--;
        size--;
      }
    }
  }

  /**
   * Split network into correlation groups. need to reset network to ids afterwards
   * 
   * @param net
   * @return
   */
  private static Collection<AnnotationNetwork> splitByGroup(AnnotationNetwork net) {
    Map<Integer, AnnotationNetwork> map = new HashMap<>();
    for (Entry<PeakListRow, ESIAdductIdentity> e : net.entrySet()) {
      Integer id = PKLRowGroup.idFrom(e.getKey());
      if (id != -1) {
        AnnotationNetwork nnet = map.get(id);
        if (nnet == null) {
          // new network for group
          nnet = new AnnotationNetwork(net.getMZTolerance(), -1);
          map.put(id, nnet);
        }
        nnet.put(e.getKey(), e.getValue());
      } else {
        // delete id if no corr group
        e.getValue().delete(e.getKey());
      }
    }
    return map.values();
  }

  /**
   * fill in neutral losses [M-H2O] is not iserted yet. they might be if [M+H2O+X]+ was also
   * annotated by another link
   * 
   * @param rows
   * @param nets
   */
  private static void fillInNeutralLosses(PeakListRow[] rows, Collection<AnnotationNetwork> nets,
      MZTolerance mzTolerance) {
    for (PeakListRow row : rows) {
      for (PeakIdentity pi : row.getPeakIdentities()) {
        // identity by ms annotation module
        if (pi instanceof ESIAdductIdentity) {
          ESIAdductIdentity neutral = (ESIAdductIdentity) pi;
          // only if charged (neutral losses do not point to the real neutral mass)
          if (neutral.getA().getAbsCharge() != 0
              && !neutral.getA().equals(ESIAdductType.M_UNMODIFIED))
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
              AnnotationNetwork newNet = new AnnotationNetwork(mzTolerance, nets.size());
              nets.add(newNet);
              newNet.put(row, neutral);
              newNet.put(partner, ESIAdductIdentity.getIdentityOf(partner, row));
              newNet.setNetworkToAllRows();
            } else {
              // add neutral loss to nets
              // do not if its already in this network (e.g. as adduct)
              Arrays.stream(partnerNets).filter(pnet -> !pnet.containsKey(row)).forEach(pnet -> {
                // try to find real annotation
                ESIAdductType pid = pnet.get(partner).getA();
                // modified
                pid = pid.createModified(neutral.getA());

                ESIAdductIdentity realID = neutral;
                if (pnet.checkForAnnotation(row, pid)) {
                  // create new
                  realID = new ESIAdductIdentity(pid);
                  row.addPeakIdentity(realID, false);
                  realID.setNetwork(pnet);
                  // set partners
                  pnet.addAllLinksTo(row, realID);
                }

                // put
                pnet.put(row, realID);
              });
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
    return MSAnnotationNetworkLogic.getAllAnnotations(row).stream()
        .map(ESIAdductIdentity::getNetwork).filter(Objects::nonNull).distinct()
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
  private static Collection<AnnotationNetwork> binNeutralMassToNetworks(PeakListRow[] rows,
      MZTolerance mzTolerance) {
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
            net = new AnnotationNetwork(mzTolerance, map.size());
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
   * 
   * @param row
   * @return list of annotations or an empty list
   */
  public static List<ESIAdductIdentity> getAllAnnotationsSorted(PeakListRow row) {
    List<ESIAdductIdentity> ident = new ArrayList<>();
    for (PeakIdentity pi : row.getPeakIdentities()) {
      if (pi instanceof ESIAdductIdentity)
        ident.add((ESIAdductIdentity) pi);
    }
    ident.sort(new Comparator<ESIAdductIdentity>() {
      @Override
      public int compare(ESIAdductIdentity a, ESIAdductIdentity b) {
        return compareRows(a, b, (PKLRowGroup) null);
      }
    });
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

  public static void recalcAllAnnotationNetworks(List<AnnotationNetwork> nets,
      boolean removeEmpty) {
    if (removeEmpty) {
      for (int i = 0; i < nets.size(); i++) {
        if (nets.get(i).size() < 2) {
          nets.remove(i);
          i--;
        }
      }
    }
    // recalc
    nets.stream().forEach(net -> {
      net.recalcConnections();
    });
  }

  /**
   * Best network of group (all rows)
   * 
   * @param g
   * @return
   */
  public static AnnotationNetwork getBestNetwork(PKLRowGroup g) {
    AnnotationNetwork best = null;
    for (PeakListRow r : g) {
      ESIAdductIdentity id = getMostLikelyAnnotation(r, g);
      AnnotationNetwork net = id != null ? id.getNetwork() : null;
      if (net != null && (best == null || best.size() < net.size()))
        best = net;
    }
    return best;
  }

}
