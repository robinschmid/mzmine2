package net.sf.mzmine.framework.networks;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

public class NetworkPanel extends JPanel {
  private static final Logger LOG = Logger.getLogger(NetworkPanel.class.getName());

  public static final String STYLE_SHEET =
      "edge {fill-color: rgb(25,85,25); stroke-color: rgb(50,100,50); stroke-width: 1px;}  node {fill-color: black; size: 10px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 1px;} "
          + "node.important{fill-color: red;} node.big{size: 15px;} node.MOL{fill-color: cyan; size: 15px;}";

  protected String styleSheet;

  // visual
  protected Graph graph;
  protected Viewer viewer;
  protected ViewPanel view;
  protected double viewPercent = 1;
  // selected node
  private List<Node> selectedNodes;

  private JLabel lbTitle;

  /**
   * Create the panel.
   */
  public NetworkPanel(String title, boolean showTitle) {
    this(title, STYLE_SHEET, showTitle);
  }

  public NetworkPanel(String title, String styleSheet, boolean showTitle) {
    this.styleSheet = styleSheet;
    this.setLayout(new BorderLayout());
    // add title
    lbTitle = new JLabel(title);
    JPanel pn = new JPanel();
    pn.add(lbTitle);
    this.add(pn, BorderLayout.NORTH);
    setShowTitle(showTitle);

    selectedNodes = new ArrayList<Node>();

    graph = new MultiGraph(title);
    setStyleSheet(styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);

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
          resetZoom();
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

  public void setShowTitle(boolean showTitle) {
    lbTitle.setVisible(showTitle);
  }

  public void setTitle(String title) {
    lbTitle.setText(title);
  }

  public void showAllLabels(boolean show) {
    for (Node node : graph) {
      if (show)
        node.addAttribute("ui.label", node.getId());
      else
        node.removeAttribute("ui.label");
    }
  }

  /**
   * 
   * @param styleSheet
   */
  public void setStyleSheet(String styleSheet) {
    this.styleSheet = styleSheet;
    graph.addAttribute("ui.stylesheet", styleSheet);
    graph.addAttribute("ui.quality", 3);
    graph.addAttribute("ui.antialias");
  }

  public void clear() {
    graph.clear();
    setStyleSheet(styleSheet);
  }

  public Graph getGraph() {
    return graph;
  }

  public ViewPanel getView() {
    return view;
  }

  public Viewer getViewer() {
    return viewer;
  }

  /**
   * Combines clear and add selection
   * 
   * @param node
   */
  public void setSelectedNode(Node node) {
    clearSelections();
    addSelection(node);
  }

  public void addSelection(Node node) {
    if (node != null) {
      node.addAttribute("ui.class", "big, important");
      selectedNodes.add(node);
    }
  }

  public void clearSelections() {
    for (Node n : selectedNodes)
      n.removeAttribute("ui.class");
    selectedNodes.clear();
  }

  public void addNewEdge(String node1, String node2) {
    String edge = node1 + node2;
    graph.addEdge(edge, node1, node2);
  }

  public void addNewEdge(String node1, String node2, Object edgeLabel) {
    String edge = node1 + node2;
    graph.addEdge(edge, node1, node2);
    graph.getEdge(edge).addAttribute("ui.label", edgeLabel);
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

  public void resetZoom() {
    viewPercent = 1;
    view.getCamera().resetView();
  }

}
