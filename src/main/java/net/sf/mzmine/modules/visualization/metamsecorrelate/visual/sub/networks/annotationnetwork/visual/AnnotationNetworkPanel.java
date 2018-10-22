package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class AnnotationNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(AnnotationNetworkPanel.class.getName());

  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();

  // data
  private PeakList pkl;
  private PeakListRow[] rows;


  /**
   * Create the panel.
   */
  public AnnotationNetworkPanel() {
    this(false);
  }

  public AnnotationNetworkPanel(boolean showTitle) {
    super("Annotation networks", showTitle);
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setPeakList(PeakList pkl) {
    this.pkl = pkl;
    if (pkl != null) {
      createNewGraph(pkl.getRows());
    } else
      clear();
  }

  /**
   * Array of rows
   * 
   * @param rows
   */
  public void setPeakListRows(PeakListRow[] rows) {
    pkl = null;
    this.rows = rows;
    if (rows != null) {
      createNewGraph(rows);
    } else {
      clear();
    }
  }

  public void createNewGraph(PeakListRow[] rows) {
    LOG.info("Adding all annotations to a network");
    clear();

    if (rows != null) {
      // sort by rt
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

      int added = 0;
      // add all connections
      for (PeakListRow row : rows) {
        int rowID = row.getID();
        for (PeakIdentity pi : row.getPeakIdentities()) {
          // identity by ms annotation module
          if (pi instanceof ESIAdductIdentity) {
            ESIAdductIdentity adduct = (ESIAdductIdentity) pi;
            int[] ids = adduct.getPartnerRowsID();

            // add all connection for ids>rowID
            for (int id : ids) {
              if (id > rowID) {
                PeakListRow row2 = findRowByID(id, rows);
                if (row2 != null) {
                  String node1 = toNodeName(row);
                  String node2 = toNodeName(row2);
                  addNewEdge(node1, node2, Math.abs(row.getAverageMZ() - row2.getAverageMZ()));
                  added++;
                }
              }
            }
          }
        }
      }
      // add id name
      for (Node node : graph) {
        node.addAttribute("ui.label", node.getId());
      }
      clearSelections();

      LOG.info("Added " + added + " connections");
    }
  }

  public void setSelectedRow(PeakListRow row) {
    String node = toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
  }

  private void addNewEdge(String node1, String node2, double dmz) {
    super.addNewEdge(node1, node2, "\u0394 " + mzForm.format(dmz));
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
}
