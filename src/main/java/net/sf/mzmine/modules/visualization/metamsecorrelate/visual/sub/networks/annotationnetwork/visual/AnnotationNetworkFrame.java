package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
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
  private AnnotationNetworkFrame() {
    setTitle("Annotation networks");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new AnnotationNetworkPanel();
    setContentPane(contentPane);

    JMenuBar menuBar = new JMenuBar();
    JCheckBoxMenuItem toggleCollapseIons = new JCheckBoxMenuItem("Collapse ions", false);
    toggleCollapseIons
        .addItemListener(il -> contentPane.collapseIonNodes(toggleCollapseIons.isSelected()));
    menuBar.add(toggleCollapseIons);

    setJMenuBar(menuBar);

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        contentPane.dispose();
        dispose();
      }
    });
  }


  public AnnotationNetworkFrame(PeakList pkl, boolean connectByNetRelations, boolean onlyBest) {
    this();
    contentPane.setConnectByNetRelations(connectByNetRelations);
    contentPane.setOnlyBest(onlyBest);
    contentPane.setPeakList(pkl);
  }

}
