package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.corrnetwork.visual;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;

public class CorrNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(CorrNetworkPanel.class.getName());

  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat percForm = new DecimalFormat("0.000");

  // data
  private PeakList pkl;
  private PeakListRow[] rows;
  private double minR;
  private R2RCorrMap map;

  /**
   * Create the panel.
   */
  public CorrNetworkPanel() {
    this(false);
  }

  public CorrNetworkPanel(boolean showTitle) {
    super("Correlation networks", showTitle);
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setPeakList(PeakList pkl) {
    this.pkl = pkl;
    if (pkl != null) {
      R2RCorrMap map = null;
      if (pkl instanceof MSEGroupedPeakList) {
        map = ((MSEGroupedPeakList) pkl).getCorrelationMap();
        createNewGraph(pkl, map);
      }
    } else
      clear();
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setPeakList(PeakList pkl, R2RCorrMap map) {
    this.pkl = pkl;
    this.map = map;
    if (pkl != null && map != null) {
      createNewGraph(pkl, map);
    } else
      clear();
  }

  public void createNewGraph(PeakList pkl, R2RCorrMap map) {
    LOG.info("Adding all corr >" + minR + " to a network");
    clear();

    // add all connections
    int added = 0;
    for (Entry<String, R2RCorrelationData> e : map.entrySet()) {
      R2RCorrelationData r2r = e.getValue();
      if (r2r != null && r2r.hasFeatureShapeCorrelation() && r2r.getAvgPeakShapeR() > minR) {
        int[] ids = R2RCorrMap.toKeyIDs(e.getKey());
        PeakListRow a = pkl.findRowByID(ids[0]);
        PeakListRow b = pkl.findRowByID(ids[1]);

        if (a != null && b != null) {
          String node1 = toNodeName(a);
          String node2 = toNodeName(b);
          addNewEdge(node1, node2, r2r.getAvgPeakShapeR());
          added++;
        }
      }
    }

    // add id name
    showAllLabels(true);
    clearSelections();

    LOG.info("Added " + added + " connections");
  }

  public void setPeakListRows(PeakListRow[] rows2, R2RCorrMap map) {
    this.rows = rows2;
    this.map = map;
    if (rows != null && map != null) {
      createNewGraph(rows, map);
    } else
      clear();
  }

  /**
   * Sub set of rows
   * 
   * @param rows
   * @param map
   */
  public void createNewGraph(PeakListRow[] rows, R2RCorrMap map) {
    LOG.info("Adding all corr >" + minR + " to a network");
    clear();

    // add all connections
    int added = 0;
    for (int i = 0; i < rows.length - 1; i++) {
      PeakListRow a = rows[i];
      for (int k = i + 1; k < rows.length; k++) {
        PeakListRow b = rows[k];
        R2RCorrelationData r2r = map.get(a, b);
        if (r2r != null && r2r.hasFeatureShapeCorrelation() && r2r.getAvgPeakShapeR() > minR) {
          String node1 = toNodeName(a);
          String node2 = toNodeName(b);
          addNewEdge(node1, node2, r2r.getAvgPeakShapeR());
          added++;
        }
      }
    }

    // add id name
    showAllLabels(true);
    clearSelections();

    LOG.info("Added " + added + " connections");
  }

  private void addNewEdge(String node1, String node2, double corr) {
    addNewEdge(node1, node2, "r=" + percForm.format(corr));
  }

  private PeakListRow findRowByID(int id, PeakListRow[] rows) {
    if (rows == null)
      return null;
    else {
      for (PeakListRow r : rows)
        if (r.getID() == id)
          return r;

      return null;
    }
  }

  private String toNodeName(PeakListRow row) {
    PeakIdentity pid = row.getPreferredPeakIdentity();
    String id = "";
    if (pid != null) {
      id = pid.getName();
      if (pid instanceof ESIAdductIdentity) {
        ESIAdductIdentity esi = (ESIAdductIdentity) pid;
        id = esi.getAdduct() + " by n=" + esi.getPartnerRowsID().length;

        if (esi.getNetID() != -1)
          id += " (Net" + esi.getNetIDString() + ")";
      }
    }
    return MessageFormat.format("{0} (mz={1}) {2}", row.getID(), mzForm.format(row.getAverageMZ()),
        id);
  }

  public void setMinR(double minR) {
    this.minR = minR;
  }

  public double getMinR() {
    return minR;
  }


  public void setSelectedRow(PeakListRow row) {
    String node = toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
  }
}
