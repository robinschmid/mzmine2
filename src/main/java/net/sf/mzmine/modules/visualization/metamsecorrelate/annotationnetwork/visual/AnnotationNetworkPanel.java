package net.sf.mzmine.modules.visualization.metamsecorrelate.annotationnetwork.visual;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.JPanel;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class AnnotationNetworkPanel extends JPanel {
  private static final Logger LOG = Logger.getLogger(AnnotationNetworkPanel.class.getName());

  protected String styleSheet =
      "node {" + "   fill-color: black;" + "}" + "node.marked {" + "   fill-color: red;" + "}";

  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();

  // visual
  private Graph graph;
  private Viewer viewer;
  private ViewPanel view;
  private double viewPercent = 1;
  // data
  private PeakList pkl;

  private PeakListRow[] rows;

  /**
   * Create the panel.
   */
  public AnnotationNetworkPanel() {
    this.setLayout(new BorderLayout());

    graph = new MultiGraph("Annotation networks");
    // graph.addAttribute("ui.quality");
    // graph.addAttribute("ui.antialias");
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

    // listener
    view.addMouseListener(new MouseAdapter() {
      private Point last;

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
          viewPercent = 1;
          view.getCamera().resetView();
          e.consume();
        } else if (e.getClickCount() == 1)
          setCenter(e.getX(), e.getY());

      }

      @Override
      public void mouseReleased(MouseEvent e) {
        last = null;
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (last == null)
          last = e.getPoint();
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        if (last != null) {
          // translate
          translate(e.getX() - last.getX(), e.getY() - last.getY());
        }
        last = e.getPoint();
      }
    });
    view.addMouseWheelListener(event -> zoom(event.getWheelRotation() < 0));
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
      graph.clear();
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
      graph.clear();
    }
  }

  public void createNewGraph(PeakListRow[] rows) {
    LOG.info("Adding all annotations to a network");
    graph.clear();

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
                  String edge = node1 + node2;
                  graph.addEdge(edge, node1, node2);
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

      LOG.info("Added " + added + " connections");
    }
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


  public void zoom(boolean zoomOut) {
    viewPercent += viewPercent * 0.1 * (zoomOut ? -1 : 1);
    view.getCamera().setViewPercent(viewPercent);
  }

  public void translate(double dx, double dy) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p0 = view.getCamera().transformPxToGu(0, 0);
    Point3 p = view.getCamera().transformPxToGu(dx, dy);

    view.getCamera().setViewCenter(c.x + p.x - p0.x, c.y + p.y + p0.y, c.z);
  }

  public void setCenter(int x, int y) {
    Point3 c = view.getCamera().getViewCenter();
    Point3 p = view.getCamera().transformPxToGu(x, y);
    view.getCamera().setViewCenter(p.x, p.y, c.z);
  }

}