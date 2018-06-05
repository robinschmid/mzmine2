package net.sf.mzmine.modules.visualization.imaging;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.sf.mzmine.project.impl.ImagingRawDataFileImpl;

public class TICImageWindow extends JFrame {

  private JPanel contentPane;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          TICImageWindow frame = new TICImageWindow(null);
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  private ImagingRawDataFileImpl raw;

  /**
   * Create the frame.
   */
  public TICImageWindow(ImagingRawDataFileImpl raw) {
    setTitle("TIC Image");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    JPanel panel = new JPanel();
    contentPane.add(panel, BorderLayout.CENTER);

    this.raw = raw;
    if (raw != null) {

    }
  }

}
