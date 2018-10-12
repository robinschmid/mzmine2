package net.sf.mzmine.modules.visualization.metamsecorrelate.networks.corrnetwork.visual;

import java.awt.EventQueue;
import javax.swing.JFrame;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;

public class CorrNetworkFrame extends JFrame {

  private CorrNetworkPanel contentPane;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          CorrNetworkFrame frame = new CorrNetworkFrame();
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
  public CorrNetworkFrame() {
    setTitle("Correlation networks");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new CorrNetworkPanel();
    setContentPane(contentPane);
  }


  public CorrNetworkFrame(PeakList pkl, double minR) {
    this();
    contentPane.setMinR(minR);
    contentPane.setPeakList(pkl);
  }

  public CorrNetworkFrame(PeakList pkl, R2RCorrMap map, double minR) {
    this();
    contentPane.setMinR(minR);
    contentPane.setPeakList(pkl, map);
  }

}
