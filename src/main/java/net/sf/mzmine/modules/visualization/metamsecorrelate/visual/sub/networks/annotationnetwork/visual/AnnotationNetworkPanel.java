package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

import java.awt.FlowLayout;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2RMap;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.R2RMS2Similarity;
import net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsIdentity;

public class AnnotationNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(AnnotationNetworkPanel.class.getName());

  private AnnotationNetworkGenerator generator = new AnnotationNetworkGenerator();

  public enum ATT {
    TYPE, RT, MZ, ID, INTENSITY, NEUTRAL_MASS, CHARGE, ION_TYPE, MS2_VERIFICATION, LABEL, NET_ID, GROUP_ID;
    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public enum EDGE_ATT {
    TYPE, LABEL, GNPS_SCORE, DIFF_SCORE, SIM_SCORE, DIFF_N, SIM_N;
    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public enum EdgeType {
    ION_IDENTITY, NETWORK_RELATIONS, MS2_SIMILARITY, MS2_SIMILARITY_NEUTRAL_M, MS2_SIMILARITY_NEUTRAL_M_TO_FEATURE;
  }

  public enum NodeType {
    NEUTRAL_M, ION_FEATURE, SINGLE_FEATURE, NEUTRAL_LOSS_CENTER;
  }

  // data
  private PeakList pkl;
  private PeakListRow[] rows;

  private boolean connectByNetRelations;
  private boolean onlyBest;
  private boolean collapse = true;

  private boolean ms2SimEdges;

  private R2RMap<R2RMS2Similarity> ms2SimMap;

  private boolean showIonEdges = true;


  /**
   * Create the panel.
   */
  public AnnotationNetworkPanel() {
    this(false);
  }

  public AnnotationNetworkPanel(boolean showTitle) {
    super("Ion identity networks (IINs)", showTitle);
    addMenu();
  }

  private void addMenu() {
    JPanel menu = getPnSettings();
    menu.setVisible(true);
    menu.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    menu.add(new JLabel("Menu"));

    JToggleButton toggleCollapseIons = new JToggleButton("Collapse ions", false);
    menu.add(toggleCollapseIons);
    toggleCollapseIons.addItemListener(il -> collapseIonNodes(toggleCollapseIons.isSelected()));

    JToggleButton toggleShowMS2SimEdges = new JToggleButton("Show MS2 sim", true);
    menu.add(toggleShowMS2SimEdges);
    toggleShowMS2SimEdges
        .addItemListener(il -> setShowMs2SimEdges(toggleShowMS2SimEdges.isSelected()));

    JToggleButton toggleShowRelations = new JToggleButton("Show relational edges", true);
    menu.add(toggleShowRelations);
    toggleShowRelations
        .addItemListener(il -> setConnectByNetRelations(toggleShowRelations.isSelected()));

    JToggleButton toggleShowIonIdentityEdges = new JToggleButton("Show ion edges", true);
    menu.add(toggleShowIonIdentityEdges);
    toggleShowIonIdentityEdges
        .addItemListener(il -> showIonIdentityEdges(toggleShowIonIdentityEdges.isSelected()));

    JToggleButton toggleShowEdgeLabel = new JToggleButton("Show edge label", false);
    menu.add(toggleShowEdgeLabel);
    toggleShowEdgeLabel.addItemListener(il -> showEdgeLabels(toggleShowEdgeLabel.isSelected()));

    JToggleButton toggleShowNodeLabel = new JToggleButton("Show node label", false);
    menu.add(toggleShowNodeLabel);
    toggleShowNodeLabel.addItemListener(il -> showNodeLabels(toggleShowNodeLabel.isSelected()));

    JButton showGNPSMatches = new JButton("GNPS matches");
    menu.add(showGNPSMatches);
    showGNPSMatches.addActionListener(e -> showGNPSMatches());

    this.revalidate();
  }

  /**
   * Show GNPS library match
   */
  private void showGNPSMatches() {
    int n = 0;
    for (Node node : graph) {
      String name = (String) node.getAttribute(GNPSResultsIdentity.ATT.COMPOUND_NAME.getKey());
      if (name != null) {
        node.setAttribute("ui.label", name);
        n++;
      }
    }
    LOG.info("Show " + n + " GNPS library matches");
  }

  private void showIonIdentityEdges(boolean selected) {
    showIonEdges = selected;
    collapseIonNodes(collapse);
  }

  public void collapseIonNodes(boolean collapse) {
    this.collapse = collapse;
    for (Node node : graph) {
      NodeType type = (NodeType) node.getAttribute(ATT.TYPE.toString());
      if (type != null) {
        switch (type) {
          case NEUTRAL_LOSS_CENTER:
          case ION_FEATURE:
            setNodeVisible(node, !collapse);
            break;
          case NEUTRAL_M:
            break;
          case SINGLE_FEATURE:
            break;
          default:
            break;
        }
      }
    }

    graph.edges().forEach(edge -> {
      EdgeType type = (EdgeType) edge.getAttribute(EDGE_ATT.TYPE.toString());
      if (type != null) {
        switch (type) {
          case ION_IDENTITY:
            setEdgeVisible(edge, !collapse && showIonEdges);
            break;
          case MS2_SIMILARITY_NEUTRAL_M_TO_FEATURE:
          case MS2_SIMILARITY_NEUTRAL_M:
          case MS2_SIMILARITY:
            setEdgeVisible(edge, ms2SimEdges);
            break;
          case NETWORK_RELATIONS:
            setEdgeVisible(edge, connectByNetRelations);
            break;
          default:
            break;
        }
      }
    });
  }

  /**
   * All the peaklist
   * 
   * @param pkl
   */
  public void setPeakList(PeakList pkl) {
    this.pkl = pkl;
    if (pkl != null) {
      this.ms2SimMap = pkl.getR2RSimilarityMap();
      createNewGraph(pkl.getRows());
    } else
      clear();
  }

  /**
   * Array of rows
   * 
   * @param rows
   */
  public void setPeakListRows(PeakListRow[] rows, R2RMap<R2RMS2Similarity> ms2SimMap) {
    pkl = null;
    this.rows = rows;
    this.ms2SimMap = ms2SimMap;
    if (rows != null) {
      createNewGraph(rows);
    } else {
      clear();
    }
  }

  public void createNewGraph(PeakListRow[] rows) {
    generator.createNewGraph(rows, graph, onlyBest, ms2SimMap);
    setStyleSheet(styleSheet);
    clearSelections();

    // last state
    collapseIonNodes(collapse);
  }



  public void setSelectedRow(PeakListRow row) {
    String node = generator.toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
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


  public void setConnectByNetRelations(boolean connectByNetRelations) {
    this.connectByNetRelations = connectByNetRelations;
    collapseIonNodes(collapse);
  }

  public void setOnlyBest(boolean onlyBest) {
    this.onlyBest = onlyBest;
  }

  public void dispose() {
    graph.clear();
  }

  public void setShowMs2SimEdges(boolean ms2SimEdges) {
    this.ms2SimEdges = ms2SimEdges;
    collapseIonNodes(collapse);
  }
}
