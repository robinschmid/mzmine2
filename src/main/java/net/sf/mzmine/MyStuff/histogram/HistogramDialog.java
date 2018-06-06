package net.sf.mzmine.MyStuff.histogram;

import java.awt.BorderLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;;

public class HistogramDialog extends JFrame {

  protected HistogramPanel histo;

  /**
   * Create the dialog. Bin width is automatically chosen
   */
  public HistogramDialog(String title, HistogramData data) {
    this(title, data, 0);
  }

  /**
   * 
   * @param title
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public HistogramDialog(String title, HistogramData data, double binWidth) {
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    setTitle(title);
    setBounds(100, 100, 1000, 800);
    getContentPane().setLayout(new BorderLayout());
    histo = new HistogramPanel(data, binWidth);
    getContentPane().add(histo, BorderLayout.CENTER);
    this.setTitle(title);
  }

  public HistogramPanel getHistoPanel() {
    return histo;
  }
}
