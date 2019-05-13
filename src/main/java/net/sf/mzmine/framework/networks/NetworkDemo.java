package net.sf.mzmine.framework.networks;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.text.DecimalFormat;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class NetworkDemo extends JFrame {

  private JPanel contentPane;
  private DecimalFormat form = new DecimalFormat("0.00");

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    System.setProperty("org.graphstream.ui", "swing");
    System.setProperty("gs.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          NetworkDemo frame = new NetworkDemo();
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the frame.
   */
  public NetworkDemo() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    NetworkPanel net = new NetworkPanel("Network demo", true);
    contentPane.add(net, BorderLayout.CENTER);

    Random rand = new Random(1l);
    int edges = 100;
    int nodes = 20;
    for (int i = 1; i <= edges; i++) {
      while (true) {
        int a = rand.nextInt(nodes) + 1;
        int b = rand.nextInt(nodes) + 1;
        if (add(net, nodeName(a), nodeName(b), rand.nextGaussian()))
          break;
      }
    }
    net.showNodeLabels(true);
    net.showEdgeLabels(true);
  }

  public boolean add(NetworkPanel net, String a, String b, double score) {
    if (a == b)
      return false;
    String edge = edgeName(a, b);
    if (net.getGraph().getEdge(edge) != null)
      return false;

    net.addNewEdge(a, b, "test", "cos=" + form.format(score));
    return true;
  }

  public String nodeName(int a) {
    return "test" + a;
  }

  public String edgeName(String a, String b) {
    return a + b + "test";
  }
}
