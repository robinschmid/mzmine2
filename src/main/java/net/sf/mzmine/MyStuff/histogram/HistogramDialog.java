package net.sf.mzmine.MyStuff.histogram;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;;

public class HistogramDialog extends JFrame {

  /**
   * Create the dialog.
   */
  public HistogramDialog(String title, HistogramData data) {
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setTitle(title);
    setBounds(100, 100, 1000, 800);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(new HistogramPanel(data), BorderLayout.CENTER);
    this.setTitle(title);
  }
}
