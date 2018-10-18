package net.sf.mzmine.modules.visualization.multimsms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.axis.ValueAxis;
import net.sf.mzmine.chartbasics.chartgroups.ChartGroup;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

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
  private boolean alwaysShowBest = false;
  private boolean showTitle = false;
  private boolean showLegend = false;
  // only the last doamin axis
  private boolean onlyShowOneAxis = true;
  // click marker in all of the group
  private boolean showClickMarker = true;

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

  /**
   * Sort rows
   * 
   * @param rows
   * @param raw
   * @param sorting
   * @param direction
   */
  public void setData(PeakListRow[] rows, RawDataFile raw, SortingProperty sorting,
      SortingDirection direction) {
    Arrays.sort(rows, new PeakListRowSorter(sorting, direction));
    setData(rows, raw);
  }

  /**
   * Create charts and show
   * 
   * @param rows
   * @param raw
   */
  public void setData(PeakListRow[] rows, RawDataFile raw) {
    pnCharts.removeAll();
    ChartGroup group = new ChartGroup(showClickMarker, showClickMarker, true, false);
    List<EChartPanel> charts = new ArrayList<>();
    for (PeakListRow row : rows) {
      EChartPanel c =
          SpectrumChartFactory.createChartPanel(row, alwaysShowBest ? null : raw, showTitle, false);
      if (c != null) {
        charts.add(c);
        group.add(new ChartViewWrapper(c));
      }
    }

    if (charts.size() > 0) {
      int realCol = autoCol ? (int) Math.ceil(Math.sqrt(charts.size())) : col;
      GridLayout layout = new GridLayout(0, realCol);
      pnCharts.setLayout(layout);
      // add to layout
      int i = 0;
      for (EChartPanel cp : charts) {
        // show only the last domain axes
        ValueAxis axis = cp.getChart().getXYPlot().getDomainAxis();
        axis.setVisible(!onlyShowOneAxis || i >= charts.size() - realCol);

        pnCharts.add(cp);
        i++;
      }
    }
    pnCharts.revalidate();
    pnCharts.repaint();
  }
}
