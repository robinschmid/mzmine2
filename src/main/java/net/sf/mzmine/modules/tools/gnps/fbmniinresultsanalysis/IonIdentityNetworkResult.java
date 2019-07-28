package net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import org.graphstream.graph.Node;
import net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsIdentity;
import net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis.GNPSResultsAnalysisTask.NodeAtt;

public class IonIdentityNetworkResult extends ArrayList<Node> {

  /**
   * Count the nodes with MS/MS which are reduced by this network to 1 neutral node
   * 
   * @param msmsData
   * @param minSignals
   * @return nodes with MS/MS - 1 (for the neutral node) (min=0)
   */
  public int getReducedNumber(Map<Integer, Integer> msmsData, int minSignals) {
    if (!hasMSMS(msmsData, minSignals))
      return 0;

    // count with MS/MS -1
    return (int) Math.max(0, stream().filter(n -> hasMSMS(n, msmsData, minSignals)).count() - 1);
  }

  /**
   * Count the nodes with MS/MS which are reduced by this network to 1 neutral node
   * 
   * @param msmsData
   * @param minSignals
   * @param matches
   * @return count of nodes which were not identified but are connected to an identity via IIN
   */
  public int countPossibleNewLibraryEntries(Map<Integer, Integer> msmsData, int minSignals,
      Map<Integer, GNPSResultsIdentity> matches) {
    return (int) streamPossibleNewLibraryEntries(msmsData, minSignals, matches).count();
  }

  /**
   * Stream the nodes with MS/MS which are reduced by this network to 1 neutral node
   * 
   * @param msmsData
   * @param minSignals
   * @param matches
   * @return nodes which were not identified but are connected to an identity via IIN
   */
  public Stream<Node> streamPossibleNewLibraryEntries(Map<Integer, Integer> msmsData,
      int minSignals, Map<Integer, GNPSResultsIdentity> matches) {
    if (!hasMSMS(msmsData, minSignals) || !hasLibraryMatch(matches))
      return Stream.empty();

    // count all with MS/MS which are not already identified with a library match
    return stream()
        .filter(n -> hasMSMS(n, msmsData, minSignals) && matches.get(toIndex(n)) == null);
  }

  /**
   * Has any library match
   * 
   * @param matches
   * @return
   */
  private boolean hasLibraryMatch(Map<Integer, GNPSResultsIdentity> matches) {
    return stream().map(n -> toIndex(n)).anyMatch(id -> matches.get(id) != null);
  }

  public int countIdentified(Map<Integer, GNPSResultsIdentity> matches) {
    return (int) streamIdentified(matches).count();
  }

  public Stream<String> streamIdentifiedIonStrings(Map<Integer, GNPSResultsIdentity> matches) {
    return streamIdentified(matches).map(n -> getIonString(n));
  }

  public Stream<Node> streamIdentified(Map<Integer, GNPSResultsIdentity> matches) {
    return stream().filter(n -> matches.get(toIndex(n)) != null);
  }

  public Stream<Node> streamWithMSMS(Map<Integer, Integer> msmsData, int minSignals) {
    return stream().filter(n -> hasMSMS(n, msmsData, minSignals));
  }

  public int countWithMSMS(Map<Integer, Integer> msmsData, int minSignals) {
    return (int) streamWithMSMS(msmsData, minSignals).count();
  }

  /**
   * Ion identity name (M+H ...)
   * 
   * @param n
   * @return
   */
  public static String getIonString(Node n) {
    return n.getAttribute(NodeAtt.IIN_ADDUCT.key).toString();
  }

  /**
   * Node index was peak list row index
   * 
   * @param n
   * @return
   */
  public static Integer toIndex(Node n) {
    return Integer.parseInt(n.getId());
  }

  public static boolean hasMSMS(Node n, Map<Integer, Integer> msmsData, int minSignals) {
    Integer signals = msmsData.get(Integer.parseInt(n.getId()));
    return signals != null && signals >= minSignals;
  }

  /**
   * Any node with MS/MS?
   * 
   * @param msmsData
   * @param minSignals
   * @return
   */
  public boolean hasMSMS(Map<Integer, Integer> msmsData, int minSignals) {
    return stream().anyMatch(n -> hasMSMS(n, msmsData, minSignals));
  }

  /**
   * Network id or null
   * 
   * @param n
   * @return
   */
  public Integer getIonNetID() {
    Map<Integer, Integer> map = new HashMap<>();

    stream().forEach(n -> {
      Object o = n.getAttribute(NodeAtt.NET_ID.key);
      if (o != null) {
        try {
          double d = Double.valueOf(o.toString());
          Integer netID = Integer.valueOf((int) d);
          Integer count = map.get(netID);
          map.put(netID, count == null ? 1 : count + 1);
        } catch (Exception e) {
        }
      }
    });

    // return netID that most nodes aggree on
    Integer max = 0;
    Integer netID = 0;
    for (Entry<Integer, Integer> e : map.entrySet()) {
      if (e.getValue() > max) {
        netID = e.getKey();
        max = e.getValue();
      }
    }

    return netID;
  }


}
