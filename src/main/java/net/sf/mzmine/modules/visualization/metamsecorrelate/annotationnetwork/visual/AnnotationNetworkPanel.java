package net.sf.mzmine.modules.visualization.metamsecorrelate.annotationnetwork.visual;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class AnnotationNetworkPanel extends JPanel {
  private static final Logger LOG = Logger.getLogger(MetaMSEcorrelateTask.class.getName());

  protected String styleSheet =
      "node {" + "   fill-color: black;" + "}" + "node.marked {" + "   fill-color: red;" + "}";

  // visual
  private Graph graph;
  private Viewer viewer;
  private ViewPanel view;
  // data
  private PeakList pkl;
  private int rowID = 0;

  /**
   * Create the panel.
   */
  public AnnotationNetworkPanel() {
    this.setLayout(new BorderLayout());

    graph = new SingleGraph("Annotation networks");
    graph.addAttribute("ui.stylesheet", styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);

    for (Node node : graph) {
      node.addAttribute("ui.label", node.getId());
    }

    viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
    viewer.enableAutoLayout();
    view = viewer.addDefaultView(false); // false indicates "no JFrame".
    this.add(view, BorderLayout.CENTER);
  }

  public void setPeakList(PeakList pkl) {
    this.pkl = pkl;
    if (pkl != null) {
      createNewGraph();
    }
  }

  public void createNewGraph() {
    LOG.info("Adding all annotations to a network");
    graph.clear();

    if (pkl != null) {
      // sort by rt
      PeakListRow[] rows = pkl.getRows();
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
                PeakListRow row2 = findRowByID(id);
                String node1 = toNodeName(row);
                String node2 = toNodeName(row2);
                String edge = node1 + node2;
                graph.addEdge(edge, node1, node2);
                added++;
              }
            }
          }
        }
      }
      LOG.info("Added " + added + " connections");
    }
  }

  private PeakListRow findRowByID(int id) {
    if (pkl == null)
      return null;
    else {
      for (PeakListRow r : pkl.getRows())
        if (r.getID() == id)
          return r;

      return null;
    }
  }

  private String toNodeName(PeakListRow row) {
    return row.getID() + " (mz=" + row.getAverageMZ() + ")";
  }

}
