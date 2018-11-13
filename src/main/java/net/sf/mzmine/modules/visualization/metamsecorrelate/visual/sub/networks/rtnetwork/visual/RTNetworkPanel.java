package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.rtnetwork.visual;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.logging.Logger;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.IonIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter.OverlapResult;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;

public class RTNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(RTNetworkPanel.class.getName());

  private NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();

  // data
  private PeakList pkl;
  private PeakListRow[] rows;

  private RTTolerance rtTolerance;
  private boolean useMinFFilter;
  private MinimumFeatureFilter minFFilter;

  private RawDataFile[] raw;

  private MZmineProject project;

  /**
   * Create the panel.
   */
  public RTNetworkPanel() {
    this(false);
  }

  public RTNetworkPanel(boolean showTitle) {
    super("Retention time networks", showTitle);
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setPeakList(PeakList pkl, boolean update) {
    this.pkl = pkl;
    if (pkl != null && update) {
      createNewGraph(pkl.getRows());
    } else
      clear();
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setPeakListRows(PeakListRow[] pkl) {
    this.rows = pkl;
    if (pkl != null) {
      createNewGraph(pkl);
    } else
      clear();
  }

  public void createNewGraph(PeakListRow[] rows) {
    LOG.info("Adding all rows in rt tolerance " + rtTolerance + " to a network");
    clear();

    // add all connections
    int added = 0;
    for (int i = 0; i < rows.length - 1; i++) {
      for (int x = i + 1; x < rows.length; x++) {
        if (filter(rows[i], rows[x])) {
          String node1 = toNodeName(rows[i]);
          String node2 = toNodeName(rows[x]);
          double drt = calcDRT(rows[i], rows[x]);
          addNewEdge(node1, node2, drt);
          added++;
        }
      }
    }

    // add id name
    showAllLabels(true);
    clearSelections();

    LOG.info("Added " + added + " connections");
  }

  /**
   * 
   * @param a
   * @param b
   * @return Always positive delta (-1 if no overlap in any RawDataFile)
   */
  private double calcDRT(PeakListRow a, PeakListRow b) {
    if (!useMinFFilter || minFFilter == null)
      return b.getAverageRT() - a.getAverageRT();

    double drt = 0;
    int c = 0;
    for (RawDataFile f : raw) {
      Feature x = a.getPeak(f);
      Feature y = b.getPeak(f);
      if (x != null && y != null) {
        drt += Math.abs(x.getRT() - y.getRT());
        c++;
      }
    }
    return c > 0 ? drt / c : -1;
  }

  private boolean filter(PeakListRow a, PeakListRow b) {
    return useMinFFilter && minFFilter != null ? //
        minFFilter.filterMinFeaturesOverlap(raw, a, b, rtTolerance).equals(OverlapResult.TRUE)
        : rtTolerance.checkWithinTolerance(a.getAverageRT(), b.getAverageRT());
  }

  private void addNewEdge(String node1, String node2, double drt) {
    addNewEdge(node1, node2, "\u0394 " + rtForm.format(drt));
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
      if (pid instanceof IonIdentity) {
        IonIdentity esi = (IonIdentity) pid;
        id = esi.getAdduct() + " by n=" + esi.getPartnerRowsID().length;

        if (esi.getNetID() != -1)
          id += " (Net" + esi.getNetIDString() + ")";
      }
    }
    return MessageFormat.format("{0} (rt={1}, mz={2}, n={3}) {4}", row.getID(),
        rtForm.format(row.getAverageRT()), mzForm.format(row.getAverageMZ()),
        row.getNumberOfPeaks(), id);
  }

  public void setSelectedRow(PeakListRow row) {
    String node = toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
  }

  public void setAll(MZmineProject project2, PeakList peakList, RTTolerance rtTolerance,
      boolean useMinFFilter, MinimumFeatureFilter minFFilter, boolean update) {
    raw = peakList.getRawDataFiles();
    project = project2;
    setRTTolerance(rtTolerance);
    setUseMinFFilter(useMinFFilter);
    setMinFFilter(minFFilter);
    setPeakList(peakList, update);
  }

  public void setRTTolerance(RTTolerance rtTolerance) {
    this.rtTolerance = rtTolerance;
  }

  public void setUseMinFFilter(boolean useMinFFilter) {
    this.useMinFFilter = useMinFFilter;
  }

  public void setMinFFilter(MinimumFeatureFilter minFFilter) {
    this.minFFilter = minFFilter;
  }
}
