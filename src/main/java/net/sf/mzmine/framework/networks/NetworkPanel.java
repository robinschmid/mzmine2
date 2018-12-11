package net.sf.mzmine.framework.networks;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSinkGraphML;
import org.graphstream.stream.file.FileSinkImages;
import org.graphstream.stream.file.FileSinkImages.LayoutPolicy;
import org.graphstream.stream.file.FileSinkImages.OutputType;
import org.graphstream.stream.file.FileSinkImages.Quality;
import org.graphstream.stream.file.FileSinkImages.Resolutions;
import org.graphstream.stream.file.FileSinkSVG;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.files.FileTypeFilter;

public class NetworkPanel extends JPanel {
  private static final Logger LOG = Logger.getLogger(NetworkPanel.class.getName());

  public static final String STYLE_SHEET =
      "edge {fill-color: rgb(50,100,50); stroke-color: rgb(50,100,50); stroke-width: 1px;}  node {text-size: 11; fill-color: black; size: 10px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 1px;} "
          + "node.important{fill-color: red;} node.big{size: 15px;} node.MOL{fill-color: cyan; size: 15px;}  node.NEUTRAL{fill-color: violet;} "
          + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 2.5px;}";

  public static final String EXPORT_STYLE_SHEET =
      "edge {fill-color: rgb(25,85,25); stroke-color: rgb(50,100,50); stroke-width: 2px;}  node {text-size: 16; fill-color: black; size: 16px; stroke-mode: plain; stroke-color: rgb(50,100,50); stroke-width: 2px;} "
          + "node.important{fill-color: red;} node.big{size: 20px;} node.MOL{fill-color: cyan; size: 20px;}  node.NEUTRAL{fill-color: violet; }"
          + "edge.medium{fill-color: rgb(50,100,200); stroke-color: rgb(50,100,200); stroke-width: 5px;}";

  protected String styleSheet;

  // save screenshot
  protected FileSinkGraphML saveGraphML = new FileSinkGraphML();
  protected FileSinkSVG saveSVG = new FileSinkSVG();
  protected FileSinkImages savePNG = new FileSinkImages(OutputType.PNG, Resolutions.HD1080);
  protected JFileChooser saveDialog = new JFileChooser();

  // visual
  protected Graph graph;
  protected Viewer viewer;
  protected ViewPanel view;
  protected double viewPercent = 1;
  // selected node
  private List<Node> selectedNodes;

  private JLabel lbTitle;

  private FileTypeFilter pngFilter;
  private FileTypeFilter svgFilter;
  private FileTypeFilter graphMLFilter;

  private JPanel pnSettings;

  /**
   * Create the panel.
   */
  public NetworkPanel(String title, boolean showTitle) {
    this(title, STYLE_SHEET, showTitle);
    System.setProperty("org.graphstream.ui.renderer",
        "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
  }

  /**
   * @wbp.parser.constructor
   */
  public NetworkPanel(String title, String styleSheet, boolean showTitle) {
    this.setLayout(new BorderLayout());


    savePNG.setLayoutPolicy(LayoutPolicy.COMPUTED_FULLY_AT_NEW_IMAGE);
    savePNG.setStyleSheet(EXPORT_STYLE_SHEET);
    savePNG.setQuality(Quality.HIGH);

    saveDialog.addChoosableFileFilter(pngFilter = new FileTypeFilter("png", "PNG image file"));
    saveDialog.addChoosableFileFilter(svgFilter = new FileTypeFilter("svg", "SVG image file"));
    saveDialog.addChoosableFileFilter(
        graphMLFilter = new FileTypeFilter("graphml", "Export graph to graphml"));
    saveDialog.setFileFilter(pngFilter);

    this.styleSheet = styleSheet;

    // add settings
    pnSettings = new JPanel();
    pnSettings.setVisible(false);
    this.add(pnSettings, BorderLayout.SOUTH);
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
        if (e.getButton() == MouseEvent.BUTTON1) {
          if (e.getClickCount() == 2) {
            resetZoom();
            e.consume();
          } else if (e.getClickCount() == 1)
            setCenter(e.getX(), e.getY());
        } else if (e.getButton() == MouseEvent.BUTTON3) {
          openSaveDialog();
          e.consume();
        }
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

  public void openSaveDialog() {
    if (graph != null && graph.getNodeCount() > 0
        && saveDialog.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      File f = saveDialog.getSelectedFile();
      if (saveDialog.getFileFilter() == pngFilter || FileAndPathUtil.getFormat(f).equals("png")) {
        f = FileAndPathUtil.getRealFilePath(saveDialog.getSelectedFile(), "png");
        saveToFile(savePNG, f);
      } else if (saveDialog.getFileFilter() == svgFilter
          || FileAndPathUtil.getFormat(f).equals("svg")) {
        f = FileAndPathUtil.getRealFilePath(saveDialog.getSelectedFile(), "svg");
        saveToFile(saveSVG, f);
      } else if (saveDialog.getFileFilter() == graphMLFilter
          || FileAndPathUtil.getFormat(f).equals("graphml")) {
        f = FileAndPathUtil.getRealFilePath(saveDialog.getSelectedFile(), "graphml");
        saveToFile(saveGraphML, f);
      }
    }
  }

  public void saveToFile(FileSink sink, File f) {
    try {
      if (graph != null && graph.getNodeCount() > 0)
        sink.writeAll(graph, f.getAbsolutePath());
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Screenshot of network not saved", e);
    }
  }

  public void setShowTitle(boolean showTitle) {
    lbTitle.setVisible(showTitle);
  }

  public void setTitle(String title) {
    lbTitle.setText(title);
  }

  public void showNodeLabels(boolean show) {
    for (Node node : graph) {
      if (show) {
        Object label = node.getAttribute("LABEL");
        if (label == null)
          label = node.getId();
        node.addAttribute("ui.label", label);
      } else
        node.removeAttribute("ui.label");
    }
  }

  public void showEdgeLabels(boolean show) {
    for (Edge edge : graph.getEdgeSet()) {
      if (show) {
        Object label = edge.getAttribute("LABEL");
        if (label == null)
          label = edge.getId();
        edge.addAttribute("ui.label", label);
      } else
        edge.removeAttribute("ui.label");
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

  public void setNodeVisible(Node node, boolean visible) {
    if (!visible)
      node.addAttribute("ui.hide");
    else
      node.removeAttribute("ui.hide");
  }

  public void setEdgeVisible(Edge edge, boolean visible) {
    if (!visible)
      edge.addAttribute("ui.hide");
    else
      edge.removeAttribute("ui.hide");
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

  public String addNewEdge(Node node1, Node node2, String edgeNameSuffix) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    return edge;
  }

  public String addNewEdge(String node1, String node2, String edgeNameSuffix) {
    String edge = node1 + node2 + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    return edge;
  }

  public String addNewEdge(Node node1, Node node2, String edgeNameSuffix, Object edgeLabel) {
    String edge = node1.getId() + node2.getId() + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    graph.getEdge(edge).addAttribute("ui.label", edgeLabel);
    graph.getEdge(edge).addAttribute("LABEL", edgeLabel);
    return edge;
  }

  public String addNewEdge(String node1, String node2, String edgeNameSuffix, Object edgeLabel) {
    String edge = node1 + node2 + edgeNameSuffix;
    graph.addEdge(edge, node1, node2);
    graph.getEdge(edge).addAttribute("ui.label", edgeLabel);
    graph.getEdge(edge).addAttribute("LABEL", edgeLabel);
    return edge;
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

  public JPanel getPnSettings() {
    return pnSettings;
  }

}
