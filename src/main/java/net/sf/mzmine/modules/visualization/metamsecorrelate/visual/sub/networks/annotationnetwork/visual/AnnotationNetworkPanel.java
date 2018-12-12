package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

import java.awt.FlowLayout;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import net.sf.mzmine.datamodel.identities.iontype.networks.IonNetworkRelationInterf;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2RMap;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.R2RMS2Similarity;

public class AnnotationNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(AnnotationNetworkPanel.class.getName());

  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat percForm = new DecimalFormat("0.000");

  public enum ATT {
    TYPE, RT, MZ, ID, INTENSITY, NEUTRAL_MASS, CHARGE, ION_TYPE, MS2_VERIFICATION, LABEL, NET_ID, GROUP_ID;
    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  public enum EdgeType {
    ION_IDENTITY, NETWORK_RELATIONS, MS2_SIMILARITY, MS2_SIMILARITY_NEUTRAL_M;
  }

  public enum NodeType {
    NEUTRAL_M, FEATURE, NEUTRAL_LOSS_CENTER;
  }

  // data
  private PeakList pkl;
  private PeakListRow[] rows;

  private boolean connectByNetRelations;
  private boolean onlyBest;
  private boolean collapse = false;

  private boolean ms2SimEdges;

  private R2RMap<R2RMS2Similarity> ms2SimMap;


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

    JToggleButton toggleShowEdgeLabel = new JToggleButton("Show edge label", true);
    menu.add(toggleShowEdgeLabel);
    toggleShowEdgeLabel.addItemListener(il -> showEdgeLabels(toggleShowEdgeLabel.isSelected()));

    JToggleButton toggleShowNodeLabel = new JToggleButton("Show node label", true);
    menu.add(toggleShowNodeLabel);
    toggleShowNodeLabel.addItemListener(il -> showNodeLabels(toggleShowNodeLabel.isSelected()));

    this.revalidate();
  }

  public void collapseIonNodes(boolean collapse) {
    this.collapse = collapse;
    for (Node node : graph) {
      NodeType type = (NodeType) node.getAttribute(ATT.TYPE.toString());
      if (type != null) {
        switch (type) {
          case NEUTRAL_LOSS_CENTER:
          case FEATURE:
            setNodeVisible(node, !collapse);
            break;
          case NEUTRAL_M:
            break;
          default:
            break;
        }
      }
    }

    graph.edges().forEach(edge -> {
      EdgeType type = (EdgeType) edge.getAttribute(ATT.TYPE.toString());
      if (type != null) {
        switch (type) {
          case ION_IDENTITY:
            setEdgeVisible(edge, !collapse);
            break;
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
    LOG.info("Adding all annotations to a network");
    clear();

    if (rows != null) {
      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(rows, onlyBest);

      AtomicInteger added = new AtomicInteger(0);
      for (IonNetwork net : nets)
        addNetworkToGraph(rows, net, added);

      // add relations
      addNetworkRelationsEdges(nets);

      // add ms2 similarity edges
      addMS2SimEdges(nets);

      // add id name
      for (Node node : graph) {
        if (node.getId().startsWith("Net"))
          node.setAttribute("ui.class", "MOL");
        if (node.getId().equals("NEUTRAL LOSSES"))
          node.setAttribute("ui.class", "NEUTRAL");


        String l = (String) node.getAttribute(ATT.LABEL.toString());
        if (l != null)
          node.setAttribute("ui.label", l);
      }
      clearSelections();

      LOG.info("Added " + added.get() + " connections");

      // last state
      collapseIonNodes(collapse);
    }
  }

  private void addMS2SimEdges(IonNetwork[] nets) {
    if (ms2SimMap != null) {
      Map<Integer, PeakListRow> map = new HashMap<>();
      Arrays.stream(nets).flatMap(n -> n.keySet().stream()).forEach(r -> map.put(r.getID(), r));

      PeakListRow[] rows = map.values().toArray(new PeakListRow[0]);
      for (int i = 0; i < rows.length - 1; i++) {
        for (int j = i + 1; j < rows.length; j++) {
          PeakListRow a = rows[i];
          PeakListRow b = rows[j];
          R2RMS2Similarity sim = ms2SimMap.get(a, b);

          if (sim != null)
            addMS2SimEdges(a, b, sim);
        }
      }
    }
  }

  private void addMS2SimEdges(PeakListRow ra, PeakListRow rb, R2RMS2Similarity sim) {
    final String label = "spec=" + percForm.format(sim.getSpectralAvgCosine()) + " delta="
        + percForm.format(sim.getDiffAvgCosine());
    //
    Node a = getRowNode(ra);
    Node b = getRowNode(rb);
    if (a != null && b != null) {
      String edgeName = addNewEdge(a, b, EdgeType.MS2_SIMILARITY.toString(), label);
      Edge edge = graph.getEdge(edgeName);
      edge.setAttribute(ATT.TYPE.toString(), EdgeType.MS2_SIMILARITY);
      edge.setAttribute(ATT.LABEL.toString(), label);
    }
    // get all neutral M
    if (ra.hasIonIdentity() && rb.hasIonIdentity()) {
      ra.getIonIdentities().stream().map(IonIdentity::getNetwork)
          .map(netA -> getNeutralMolNode(netA, false)).filter(Objects::nonNull).forEach(nodeA -> {
            // add connection to all neutral M nodes of rowb
            rb.getIonIdentities().stream().map(IonIdentity::getNetwork)
                .map(netB -> getNeutralMolNode(netB, false)).filter(Objects::nonNull)
                .forEach(nodeB -> {
                  // connect a and b
                  String edgeName =
                      addNewEdge(nodeA, nodeB, EdgeType.MS2_SIMILARITY_NEUTRAL_M.toString(), label);
                  Edge edge = graph.getEdge(edgeName);
                  edge.setAttribute(ATT.TYPE.toString(), EdgeType.MS2_SIMILARITY_NEUTRAL_M);
                  edge.setAttribute(ATT.LABEL.toString(), label);
                });
          });
    }
  }

  /**
   * Adds all relational edges between networks
   * 
   * @param nets
   */
  private void addNetworkRelationsEdges(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      if (net.getRelations() != null) {

        net.getRelations().entrySet().stream().map(Map.Entry::getValue)
            // only do it once
            .filter(rel -> rel.isLowestIDNetwork(net)).forEach(rel -> addRelationEdges(rel));
      }
    }
  }

  /**
   * Adds all the edges of an relation between the networks
   * 
   * @param rel
   */
  private void addRelationEdges(IonNetworkRelationInterf rel) {
    IonNetwork[] nets = rel.getAllNetworks();
    for (int i = 0; i < nets.length - 1; i++) {
      for (int j = i + 1; j < nets.length; j++) {
        Node a = getNeutralMolNode(nets[i], false);
        Node b = getNeutralMolNode(nets[j], false);
        if (a != null && b != null) {
          String edgeLabel = rel.getDescription();
          String edgeName = addNewEdge(a, b, "relations", edgeLabel);
          Edge edge = graph.getEdge(edgeName);
          edge.setAttribute("ui.class", "medium");
          edge.setAttribute(ATT.TYPE.toString(), EdgeType.NETWORK_RELATIONS);
          edge.setAttribute(ATT.LABEL.toString(), edgeLabel);
        }
      }
    }
  }

  private void addNetworkToGraph(PeakListRow[] rows, IonNetwork net, AtomicInteger added) {
    Node mnode = !net.isUndefined() ? getNeutralMolNode(net, true) : null;
    // bundle all neutral losses together
    Node neutralNode = graph.addNode("NEUTRAL LOSSES");
    neutralNode.setAttribute("ui.class", "NEUTRAL");
    neutralNode.setAttribute(ATT.TYPE.toString(), NodeType.NEUTRAL_LOSS_CENTER);

    // add center neutral M
    net.entrySet().stream().forEach(e -> {
      Node node = getRowNode(e.getKey(), e.getValue());

      if (e.getValue().getIonType().isModifiedUndefinedAdduct()) {
        // neutral
        addNewEdge(neutralNode, node, "ions");
      } else if (!e.getValue().getIonType().isUndefinedAdduct()) {
        addNewDeltaMZEdge(mnode, node, Math.abs(net.getNeutralMass() - e.getKey().getAverageMZ()));
      }
      added.incrementAndGet();
    });
    // add all edges between ions
    net.entrySet().stream().forEach(e -> {
      PeakListRow row = e.getKey();
      Node node1 = getRowNode(row, e.getValue());

      e.getValue().getPartner().entrySet().stream().filter(Objects::nonNull).forEach(partner -> {
        PeakListRow prow = partner.getKey();
        IonIdentity link = partner.getValue();
        // do only once (for row with smaller index)
        if (prow != null && link != null && row.getID() < prow.getID()) {
          Node node2 = getRowNode(prow, link);
          addNewDeltaMZEdge(node1, node2,
              Math.abs(e.getKey().getAverageMZ() - prow.getAverageMZ()));
          added.incrementAndGet();
        }
      });
    });

  }

  /**
   * Creates or gets the neutral mol node of this net
   * 
   * @param net
   * @return
   */
  private Node getNeutralMolNode(IonNetwork net, boolean createNew) {
    if (net == null)
      return null;

    String name = MessageFormat.format("M (m={0} Da) Net{1} corrID={2}",
        mzForm.format(net.getNeutralMass()), net.getID(), net.getCorrID());

    Node node = graph.getNode("Net" + net.getID());
    if (node == null && createNew) {
      node = graph.addNode("Net" + net.getID());
      node.setAttribute(ATT.TYPE.toString(), NodeType.NEUTRAL_M);
      node.setAttribute(ATT.LABEL.toString(), name);
      node.setAttribute("ui.label", name);
      node.setAttribute(ATT.NET_ID.toString(), net.getID());
      node.setAttribute(ATT.RT.toString(), net.getAvgRT());
      node.setAttribute(ATT.NEUTRAL_MASS.toString(), net.getNeutralMass());
      node.setAttribute(ATT.INTENSITY.toString(), net.getHeightSum());
      node.setAttribute(ATT.ION_TYPE.toString(),
          net.values().stream().map(IonIdentity::getIonType).toArray());
    }

    return node;
  }

  public void setSelectedRow(PeakListRow row) {
    String node = toNodeName(row);
    // set selected
    Node n = graph.getNode(node);
    setSelectedNode(n);
  }

  private void addNewDeltaMZEdge(Node node1, Node node2, double dmz) {
    String edgeName = super.addNewEdge(node1, node2, "ions", "\u0394 " + mzForm.format(dmz));
    graph.getEdge(edgeName).setAttribute(ATT.TYPE.toString(), EdgeType.ION_IDENTITY);
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
    return "Row" + row.getID();
  }

  private Node getRowNode(PeakListRow row) {
    return graph.getNode(toNodeName(row));
  }

  private Node getRowNode(PeakListRow row, IonIdentity esi) {
    String id = "";
    if (esi != null) {
      id = esi.getAdduct() + " by n=" + esi.getPartnerRowsID().length;

      if (esi.getNetID() != -1)
        id += " (Net" + esi.getNetIDString() + ")";
    }
    String label = MessageFormat.format("{0} (mz={1}) {2}", row.getID(),
        mzForm.format(row.getAverageMZ()), id);

    Node node = graph.getNode(toNodeName(row));
    if (node == null) {
      node = graph.addNode(toNodeName(row));
      node.setAttribute(ATT.LABEL.toString(), label);
      node.setAttribute("ui.label", label);
      node.setAttribute(ATT.TYPE.toString(), NodeType.FEATURE);
      node.setAttribute(ATT.ID.toString(), row.getID());
      node.setAttribute(ATT.RT.toString(), row.getAverageRT());
      node.setAttribute(ATT.MZ.toString(), row.getAverageMZ());
      node.setAttribute(ATT.INTENSITY.toString(), row.getBestPeak().getHeight());
      node.setAttribute(ATT.CHARGE.toString(), row.getRowCharge());
      node.setAttribute(ATT.GROUP_ID.toString(), row.getGroupID());
      if (esi != null) {
        node.setAttribute(ATT.ION_TYPE.toString(), esi.getIonType());
        node.setAttribute(ATT.NEUTRAL_MASS.toString(),
            esi.getIonType().getMass(row.getAverageMZ()));
        node.setAttribute(ATT.NET_ID.toString(), esi.getNetID());
        String ms2Veri = (esi.getMSMSMultimerCount() > 0 ? "xmer_verified" : "")
            + (esi.getMSMSModVerify() > 0 ? " modification_verified" : "");
        node.setAttribute(ATT.MS2_VERIFICATION.toString(), ms2Veri);
      }
    }

    return node;
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
