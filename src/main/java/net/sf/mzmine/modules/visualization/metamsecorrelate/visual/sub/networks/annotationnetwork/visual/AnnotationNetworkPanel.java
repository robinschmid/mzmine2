package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.MSAnnotationNetworkLogic;
import net.sf.mzmine.framework.networks.NetworkPanel;
import net.sf.mzmine.main.MZmineCore;
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
      // sort by ID
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));

      AtomicInteger added = new AtomicInteger(0);
      // add all connections
      for (PeakListRow row : rows) {
        for (AnnotationNetwork net : MSAnnotationNetworkLogic.getAllNetworks(row)) {
          if (net.hasSmallestID(row)) {
            addNetworkToGraph(rows, net, added);
          }
        }
      }
      // add id name
      for (Node node : graph) {
        if (node.getId().startsWith("M ("))
          node.addAttribute("ui.class", "MOL");
        if (node.getId().equals("NEUTRAL LOSSES"))
          node.addAttribute("ui.class", "NEUTRAL");


        node.addAttribute("ui.label", node.getId());
      }
      clearSelections();

      LOG.info("Added " + added.get() + " connections");
    }
  }

  private void addNetworkToGraph(PeakListRow[] rows, AnnotationNetwork net, AtomicInteger added) {
    String mnode = MessageFormat.format("M (m={0} Da) Net{1} corrID={2}",
        mzForm.format(net.getNeutralMass()), net.getID(), net.getCorrID());

    String neutralNode = "NEUTRAL LOSSES";

    // add center neutral M
    net.entrySet().stream().forEach(e -> {
      String node = toNodeName(e.getKey(), e.getValue());

      if (e.getValue().getIonType().isModifiedUndefinedAdduct()) {
        // neutral
        addNewEdge(neutralNode, node);
        graph.getNode(node).setAttribute("ui.class", "NEUTRAL");
      } else if (!e.getValue().getIonType().isUndefinedAdduct()) {
        addNewEdge(mnode, node, Math.abs(net.getNeutralMass() - e.getKey().getAverageMZ()));
      }
      added.incrementAndGet();
    });
    // add all edges between ions
    net.entrySet().stream().forEach(e -> {
      String node1 = toNodeName(e.getKey(), e.getValue());

      int[] partnerID = e.getValue().getPartnerRowsID();
      for (int id : partnerID) {
        PeakListRow prow = findRowByID(id, rows);
        if (prow != null) {
          IonIdentity link = net.get(prow);
          if (link != null) {
            String node2 = toNodeName(prow, link);
            addNewEdge(node1, node2, Math.abs(e.getKey().getAverageMZ() - prow.getAverageMZ()));
            added.incrementAndGet();
          }
        }
      }
    });
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
      if (pid instanceof IonIdentity) {
        IonIdentity esi = (IonIdentity) pid;
        id = esi.getAdduct() + " by n=" + esi.getPartnerRowsID().length;

        if (esi.getNetID() != -1)
          id += " (Net" + esi.getNetIDString() + ")";
      }
    }
    return MessageFormat.format("{0} (mz={1}) {2}", row.getID(), mzForm.format(row.getAverageMZ()),
        id);
  }

  private String toNodeName(PeakListRow row, IonIdentity esi) {
    String id = "";
    if (esi != null) {
      id = esi.getAdduct() + " by n=" + esi.getPartnerRowsID().length;

      if (esi.getNetID() != -1)
        id += " (Net" + esi.getNetIDString() + ")";
    }
    return MessageFormat.format("{0} (mz={1}) {2}", row.getID(), mzForm.format(row.getAverageMZ()),
        id);
  }
}
