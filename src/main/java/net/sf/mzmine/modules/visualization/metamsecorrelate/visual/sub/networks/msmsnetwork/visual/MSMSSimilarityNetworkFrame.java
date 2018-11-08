package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.msmsnetwork.visual;

import java.awt.EventQueue;
import javax.swing.JFrame;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.similarity.R2RMS2Similarity;

public class MSMSSimilarityNetworkFrame extends JFrame {

  private MSMSSimilarityNetworkPanel contentPane;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          MSMSSimilarityNetworkFrame frame = new MSMSSimilarityNetworkFrame();
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
  public MSMSSimilarityNetworkFrame() {
    setTitle("Correlation networks");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new MSMSSimilarityNetworkPanel();
    setContentPane(contentPane);
  }

  public MSMSSimilarityNetworkFrame(R2RMap<R2RMS2Similarity> map, double minCosine,
      int minOverlap) {
    this();
    contentPane.setMin(minCosine, minOverlap);
    contentPane.setMap(map);
  }

}
