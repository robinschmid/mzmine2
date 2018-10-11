package net.sf.mzmine.modules.visualization.metamsecorrelate.annotationnetwork.visual;

import java.awt.EventQueue;
import javax.swing.JFrame;
import net.sf.mzmine.datamodel.PeakList;

public class AnnotationNetworkFrame extends JFrame {

  private AnnotationNetworkPanel contentPane;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          AnnotationNetworkFrame frame = new AnnotationNetworkFrame();
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
  public AnnotationNetworkFrame() {
    setTitle("Annotation networks");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new AnnotationNetworkPanel();
    setContentPane(contentPane);
  }


  public AnnotationNetworkFrame(PeakList pkl) {
    this();
    contentPane.setPeakList(pkl);
  }

}
