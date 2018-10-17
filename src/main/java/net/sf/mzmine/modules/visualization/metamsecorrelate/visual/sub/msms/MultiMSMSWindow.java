package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.msms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;

/**
 * Holds more charts for data reviewing
 * 
 * @author Robin Schmid
 *
 */
public class MultiMSMSWindow extends JFrame {

  private JPanel contentPane;
  private JPanel pnCharts;
  private int col = 4;
  private boolean autoCol = true;

  /**
   * Create the frame.
   */
  public MultiMSMSWindow() {
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setBounds(100, 100, 853, 586);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    pnCharts = new JPanel();
    contentPane.add(pnCharts, BorderLayout.CENTER);
    pnCharts.setLayout(new GridLayout(0, 4));
  }

  public void setData(PeakListRow[] rows, RawDataFile raw) {
    pnCharts.removeAll();

    List<EChartPanel> charts = new ArrayList<>();
    for (PeakListRow row : rows) {
      EChartPanel c = SpectrumChartFactory.createChart(row, raw);
      if (c != null)
        charts.add(c);
    }

    if (charts.size() > 0) {
      int realCol = autoCol ? (int) Math.ceil(Math.sqrt(charts.size())) : col;
      GridLayout layout = new GridLayout(0, realCol);
      pnCharts.setLayout(layout);
      // add to layout
      for (EChartPanel cp : charts) {
        pnCharts.add(cp);
      }
    }
    pnCharts.revalidate();
    pnCharts.repaint();
  }
}
