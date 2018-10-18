package net.sf.mzmine.modules.visualization.multimsms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import net.sf.mzmine.chartbasics.chartgroups.ChartGroup;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrum;
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

  // MS 1
  private ChartViewWrapper msone;

  // MS 2
  private ChartGroup group;
  //
  private JPanel contentPane;
  private JPanel pnCharts;
  private int col = 4;
  private int realCol = col;
  private boolean autoCol = true;
  private boolean alwaysShowBest = false;
  private boolean showTitle = false;
  private boolean showLegend = false;
  // only the last doamin axis
  private boolean onlyShowOneAxis = true;
  // click marker in all of the group
  private boolean showCrosshair = true;

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

    addMenu();
  }

  private void addMenu() {
    JMenuBar menu = new JMenuBar();
    JMenu settings = new JMenu("Settings");
    menu.add(settings);

    JFrame thisframe = this;

    // set columns
    JMenuItem setCol = new JMenuItem("set columns");
    menu.add(setCol);
    setCol.addActionListener(e -> {
      try {
        col = Integer.parseInt(JOptionPane.showInputDialog("Columns", col));
        setAutoColumns(false);
        setColumns(col);
      } catch (Exception e2) {
      }
    });

    //
    addCheckBox(settings, "auto columns", autoCol,
        e -> setAutoColumns(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show one axis only", onlyShowOneAxis,
        e -> setOnlyShowOneAxis(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show legend", showLegend,
        e -> setShowLegend(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show title", showTitle,
        e -> setShowTitle(((JCheckBoxMenuItem) e.getSource()).isSelected()));
    addCheckBox(settings, "show click marker", showCrosshair,
        e -> setShowCrosshair(((JCheckBoxMenuItem) e.getSource()).isSelected()));;


    this.setJMenuBar(menu);
  }

  public void setColumns(int col2) {
    col = col2;
    renewCharts(group);
  }

  public void setAutoColumns(boolean selected) {
    this.autoCol = selected;
  }

  public void setShowCrosshair(boolean showCrosshair) {
    this.showCrosshair = showCrosshair;
    if (group != null)
      group.setShowCrosshair(showCrosshair, showCrosshair);
  }

  public void setShowLegend(boolean showLegend) {
    this.showLegend = showLegend;
    forAllCharts(c -> c.getLegend().setVisible(showLegend));
  }

  public void setShowTitle(boolean showTitle) {
    this.showTitle = showTitle;
    forAllCharts(c -> c.getTitle().setVisible(showTitle));
  }

  public void setOnlyShowOneAxis(boolean onlyShowOneAxis) {
    this.onlyShowOneAxis = onlyShowOneAxis;
    int i = 0;
    forAllCharts(c -> {
      // show only the last domain axes
      ValueAxis axis = c.getXYPlot().getDomainAxis();
      axis.setVisible(!onlyShowOneAxis || i >= group.size() - realCol);
    });
  }

  private void addCheckBox(JMenu menu, String title, boolean state, ItemListener il) {
    JCheckBoxMenuItem item = new JCheckBoxMenuItem(title);
    item.setSelected(state);
    item.addItemListener(il);
    menu.add(item);
  }

  /**
   * Sort rows
   * 
   * @param rows
   * @param raw
   * @param sorting
   * @param direction
   */
  public void setData(PeakListRow[] rows, RawDataFile raw, boolean createMS1,
      SortingProperty sorting, SortingDirection direction) {
    Arrays.sort(rows, new PeakListRowSorter(sorting, direction));
    setDataAndCreatePseudoMS1(rows, raw);
  }

  /**
   * Create charts and show
   * 
   * @param rows
   * @param raw
   */
  public void setData(PeakListRow[] rows, RawDataFile raw, boolean createMS1) {
    msone = null;
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    // MS1
    if (createMS1) {
      Scan scan = null;
      Feature best = null;
      for (PeakListRow r : rows) {
        Feature f = raw == null ? r.getBestPeak() : r.getPeak(raw);
        if (f != null && (best == null || f.getHeight() > best.getHeight())) {
          best = f;
        }
      }
      if (best != null) {
        scan = raw.getScan(best.getRepresentativeScanNumber());
        EChartPanel cp = SpectrumChartFactory.createChartPanel(scan, showTitle, showLegend);
        if (cp != null)
          msone = new ChartViewWrapper(cp);
      }
    }
    if (msone != null)
      group.add(msone);

    // MS2 of all rows
    for (PeakListRow row : rows) {
      EChartPanel c = SpectrumChartFactory.createMSMSChartPanel(row, alwaysShowBest ? null : raw,
          showTitle, showLegend);
      if (c != null) {
        group.add(new ChartViewWrapper(c));
      }
    }
    renewCharts(group);
  }

  /**
   * Create charts and show
   * 
   * @param rows
   * @param raw
   */
  public void setDataAndCreatePseudoMS1(PeakListRow[] rows, RawDataFile raw) {
    msone = null;
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    // MS1

    Scan scan = null;
    Feature best = null;
    for (PeakListRow r : rows) {
      Feature f = raw == null ? r.getBestPeak() : r.getPeak(raw);
      if (f != null && (best == null || f.getHeight() > best.getHeight())) {
        best = f;
      }
    }
    if (best != null) {
      scan = raw.getScan(best.getRepresentativeScanNumber());
      EChartPanel cp = PseudoSpectrum.createChartPanel(rows, raw, false, "pseudo");
      cp.getChart().getLegend().setVisible(showLegend);
      cp.getChart().getTitle().setVisible(showTitle);
      if (cp != null)
        msone = new ChartViewWrapper(cp);
    }

    if (msone != null)
      group.add(msone);

    // MS2 of all rows
    for (PeakListRow row : rows) {
      EChartPanel c = SpectrumChartFactory.createMSMSChartPanel(row, alwaysShowBest ? null : raw,
          showTitle, false);
      if (c != null) {
        group.add(new ChartViewWrapper(c));
      }
    }
    renewCharts(group);
  }

  /**
   * 
   * @param group
   */
  public void renewCharts(ChartGroup group) {
    pnCharts.removeAll();
    if (group != null && group.size() > 0) {
      realCol = autoCol ? (int) Math.ceil(Math.sqrt(group.size())) : col;
      GridLayout layout = new GridLayout(0, realCol);
      pnCharts.setLayout(layout);
      // add to layout
      int i = 0;
      for (ChartViewWrapper cp : group.getList()) {
        // show only the last domain axes
        ValueAxis axis = cp.getChart().getXYPlot().getDomainAxis();
        axis.setVisible(!onlyShowOneAxis || i >= group.size() - realCol);

        pnCharts.add(cp.getChartSwing());
        i++;
      }
    }
    pnCharts.revalidate();
    pnCharts.repaint();
  }

  public void forAllCharts(Consumer<JFreeChart> op) {
    if (group != null)
      group.forAllCharts(op);
  }
}
