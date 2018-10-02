package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import java.awt.BorderLayout;
import java.util.Iterator;
import javax.swing.JFrame;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

public class TestNetworks {
  public static void main(String args[]) {
    // System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    new TestNetworks();
  }

  public TestNetworks() {
    createNewFrame();
    // createDirect();
  }

  private void createDirect() {
    Graph graph = new SingleGraph("tutorial 1");

    graph.addAttribute("ui.stylesheet", styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);
    graph.display();

    graph.addEdge("AB", "A", "B");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("CA", "C", "A");
    graph.addEdge("AD", "A", "D");
    graph.addEdge("DE", "D", "E");
    graph.addEdge("DF", "D", "F");
    graph.addEdge("EF", "E", "F");

    for (Node node : graph) {
      node.addAttribute("ui.label", node.getId());
    }

    explore(graph.getNode("A"));
  }

  public void createNewFrame() {
    Graph graph = new SingleGraph("tutorial 1");

    graph.addAttribute("ui.stylesheet", styleSheet);
    graph.setAutoCreate(true);
    graph.setStrict(false);

    graph.addEdge("AB", "A", "B");
    graph.addEdge("BC", "B", "C");
    graph.addEdge("CA", "C", "A");
    graph.addEdge("AD", "A", "D");
    graph.addEdge("DE", "D", "E");
    graph.addEdge("DF", "D", "F");
    graph.addEdge("EF", "E", "F");

    for (Node node : graph) {
      node.addAttribute("ui.label", node.getId());
    }

    Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
    viewer.enableAutoLayout();
    ViewPanel view = viewer.addDefaultView(false); // false indicates "no JFrame".

    JFrame frame = new JFrame("Test");
    frame.getContentPane().add(view, BorderLayout.CENTER);
    frame.setSize(800, 800);
    frame.setVisible(true);
  }

  public void explore(Node source) {
    Iterator<? extends Node> k = source.getBreadthFirstIterator();

    while (k.hasNext()) {
      Node next = k.next();
      next.setAttribute("ui.class", "marked");
      sleep();
    }
  }

  protected void sleep() {
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
    }
  }

  protected String styleSheet =
      "node {" + "   fill-color: black;" + "}" + "node.marked {" + "   fill-color: red;" + "}";
}
