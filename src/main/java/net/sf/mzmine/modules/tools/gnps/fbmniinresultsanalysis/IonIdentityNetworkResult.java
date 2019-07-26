package net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.graphstream.graph.Node;
import net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis.GNPSResultsAnalysisTask.NodeAtt;

public class IonIdentityNetworkResult extends ArrayList<Node> {

  /**
   * Count the nodes with MS/MS which are reduced by this network to 1 neutral node
   * 
   * @param msmsData
   * @param minSignals
   * @return nodes with MS/MS - 1 (for the neutral node) (min=0)
   */
  public int getReducedNumber(HashMap<Integer, Integer> msmsData, int minSignals) {
    if (!hasMSMS(msmsData, minSignals))
      return 0;

    // count with MS/MS -1
    return (int) Math.max(0, stream().filter(n -> hasMSMS(n, msmsData, minSignals)).count() - 1);
  }

  public boolean hasMSMS(Node n, HashMap<Integer, Integer> msmsData, int minSignals) {
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
  public boolean hasMSMS(HashMap<Integer, Integer> msmsData, int minSignals) {
    return stream().anyMatch(n -> hasMSMS(n, msmsData, minSignals));
  }

  /**
   * Network id or null
   * 
   * @param n
   * @return
   */
  private Integer getIonNetworkID() {
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
