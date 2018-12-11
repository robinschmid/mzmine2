package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.externalsource;

import javax.swing.JFrame;

public class ExternalSourceNetworkFrame extends JFrame {

  private ExternalSourceNetworkPanel contentPane;

  public static void main(String[] args) {
    new ExternalSourceNetworkFrame().setVisible(true);
  }

  /**
   * Create the frame.
   */
  public ExternalSourceNetworkFrame() {
    setTitle("Correlation networks");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new ExternalSourceNetworkPanel();
    setContentPane(contentPane);
  }


}
