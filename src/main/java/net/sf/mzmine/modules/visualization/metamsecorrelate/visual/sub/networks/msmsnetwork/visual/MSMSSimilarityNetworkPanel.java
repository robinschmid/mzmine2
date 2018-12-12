package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.msmsnetwork.visual;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2RMap;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.R2RMS2Similarity;

public class MSMSSimilarityNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(MSMSSimilarityNetworkPanel.class.getName());

  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat percForm = new DecimalFormat("0.000");

  // data
  private R2RMap<R2RMS2Similarity> map;
  private double minCosine;
  private int minOverlap;

  /**
   * Create the panel.
   */
  public MSMSSimilarityNetworkPanel() {
    this(false);
  }

  public MSMSSimilarityNetworkPanel(boolean showTitle) {
    super("MS/MS similarity networks", showTitle);
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setMap(R2RMap<R2RMS2Similarity> map) {
    this.map = map;
    if (map != null) {
      createNewGraph(map);
    } else
      clear();
  }

  public void createNewGraph(R2RMap<R2RMS2Similarity> map) {
    LOG.info(
        "Adding all cosine >" + minCosine + " and all overlap>" + minOverlap + " to a network");
    clear();

    // add all connections
    for (Entry<String, R2RMS2Similarity> e : map.entrySet()) {
      R2RMS2Similarity r2r = e.getValue();

      PeakListRow a = r2r.getA();
      PeakListRow b = r2r.getB();
      String node1 = toNodeName(a);
      String node2 = toNodeName(b);

      // spectral match
      if (r2r.getSpectralAvgCosine() >= minCosine)
        addNewEdge(node1, node2, r2r.getSpectralAvgCosine(), "specCos", "rgb(50,150,50)");
      if (r2r.getSpectralAvgOverlap() >= minOverlap)
        addNewEdge(node1, node2, r2r.getSpectralAvgOverlap(), "specShared", "rgb(150,50,50)");
      if (r2r.getDiffAvgCosine() >= minCosine)
        addNewEdge(node1, node2, r2r.getDiffAvgCosine(), "diffCos", "rgb(50,50,150)");
      if (r2r.getDiffAvgOverlap() >= minOverlap)
        addNewEdge(node1, node2, r2r.getDiffAvgOverlap(), "diffShared", "rgb(150,150,150)");
    }

    // add id name
    showNodeLabels(true);
    clearSelections();
  }

  private void addNewEdge(String node1, String node2, double corr, String edgeSuffix,
      String color) {
    String label = edgeSuffix + "=" + percForm.format(corr);
    String edge = node1 + node2 + edgeSuffix;
    graph.addEdge(edge, node1, node2);
    graph.getEdge(edge).setAttribute("ui.label", label);
  }

  private String toNodeName(PeakListRow row) {
    IonIdentity esi = row.getBestIonIdentity();
    String id = "";
    if (esi != null) {
      id = esi.getAdduct() + " by n=" + esi.getPartnerRowsID().length;

      if (esi.getNetID() != -1)
        id += " (Net" + esi.getNetIDString() + ")";
    }
    return MessageFormat.format("{0} (mz={1}) {2}", row.getID(), mzForm.format(row.getAverageMZ()),
        id);
  }

  public void setSelectedRow(PeakListRow row) {
    String node = toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
  }

  public void setMin(double minCosine, int minOverlap) {
    this.minCosine = minCosine;
    this.minOverlap = minOverlap;
  }

}
