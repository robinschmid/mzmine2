package net.sf.mzmine.modules.visualization.multimsms;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.MSMSIonIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSIdentity;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrum;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrumDataSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
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

  // annotations for MSMS
  private List<AbstractMSMSIdentity> msmsAnnotations;
  // to flag annotations in spectra
  private MZTolerance mzTolerance;

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

    // reset zoom
    JMenuItem resetZoom = new JMenuItem("reset zoom");
    menu.add(resetZoom);
    resetZoom.addActionListener(e -> {
      if (group != null)
        group.resetZoom();
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
    setData(rows, raw, createMS1);
  }

  /**
   * Create charts and show
   * 
   * @param rows
   * @param raw
   */
  public void setData(PeakListRow[] rows, RawDataFile raw, boolean createMS1) {
    // use raw?
    if (alwaysShowBest)
      raw = null;

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
        scan = best.getDataFile().getScan(best.getRepresentativeScanNumber());
        EChartPanel cp = SpectrumChartFactory.createChartPanel(scan, showTitle, showLegend);
        if (cp != null)
          msone = new ChartViewWrapper(cp);
      }
    }
    if (msone != null)
      group.add(msone);

    // MS2 of all rows
    for (PeakListRow row : rows) {
      EChartPanel c = SpectrumChartFactory.createMSMSChartPanel(row, raw, showTitle, showLegend);
      if (c != null) {
        group.add(new ChartViewWrapper(c));
      }
    }

    // add all MSMS annotations
    addAllMSMSAnnotations(rows, raw);

    renewCharts(group);
  }


  /**
   * Adds all MS1 and MSMS annotations to all charts
   * 
   * @param rows
   * @param raw
   */
  public void addAllMSMSAnnotations(PeakListRow[] rows, RawDataFile raw) {
    for (PeakListRow row : rows) {
      // add MS1 annotations
      // limited by correlation group
      PKLRowGroup group = PKLRowGroup.from(row);

      ESIAdductIdentity best = MSAnnotationNetworkLogic.getMostLikelyAnnotation(row, group);
      if (best == null)
        continue;

      Scan scan = SpectrumChartFactory.getMSMSScan(row, raw);
      double precursorMZ = row.getPeak(scan.getDataFile()).getMZ();
      // add ms1 adduct annotation
      addMSMSAnnotation(
          new MSMSIonIdentity(mzTolerance, new SimpleDataPoint(precursorMZ, 1f), best.getA()));

      // add all MSMS annotations (found in MSMS)
      for (ESIAdductIdentity id : MSAnnotationNetworkLogic.getAllAnnotations(row)) {
        addMSMSAnnotations(id.getMSMSIdentities());
      }
    }
  }

  /**
   * Sort rows
   * 
   * @param rows
   * @param raw
   * @param sorting
   * @param direction
   */
  public void setDataAndCreatePseudoMS1(PeakListRow[] rows, RawDataFile raw,
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
  public void setDataAndCreatePseudoMS1(PeakListRow[] rows, RawDataFile raw) {
    // use raw?
    if (alwaysShowBest)
      raw = null;

    msone = null;
    group = new ChartGroup(showCrosshair, showCrosshair, true, false);
    // MS1
    EChartPanel cp = PseudoSpectrum.createChartPanel(rows, raw, false, "pseudo");
    if (cp != null) {
      cp.getChart().getLegend().setVisible(showLegend);
      cp.getChart().getTitle().setVisible(showTitle);
      msone = new ChartViewWrapper(cp);
    }
    if (msone != null)
      group.add(msone);

    // MS2 of all rows
    for (PeakListRow row : rows) {
      EChartPanel c = SpectrumChartFactory.createMSMSChartPanel(row, raw, showTitle, false);
      if (c != null) {
        group.add(new ChartViewWrapper(c));
      }
    }
    // add all MSMS annotations
    addAllMSMSAnnotations(rows, raw);

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

  // ANNOTATIONS
  public void addMSMSAnnotation(AbstractMSMSIdentity ann) {
    if (msmsAnnotations == null)
      msmsAnnotations = new ArrayList<>();
    msmsAnnotations.add(ann);

    // extract mz tolerance
    if (mzTolerance == null)
      mzTolerance = ann.getMzTolerance();

    // add to charts
    addAnnotationToCharts(ann);
  }

  public void addMSMSAnnotations(List<? extends AbstractMSMSIdentity> ann) {
    if (ann == null)
      return;
    // extract mz tolerance
    if (mzTolerance == null)
      for (AbstractMSMSIdentity a : ann)
        if (a.getMzTolerance() != null) {
          mzTolerance = a.getMzTolerance();
          break;
        }

    // add all
    for (AbstractMSMSIdentity a : ann)
      addMSMSAnnotation(a);
  }


  /**
   * To flag annotations in spectra
   * 
   * @param mzTolerance
   */
  public void setMzTolerance(MZTolerance mzTolerance) {
    boolean changed =
        mzTolerance == this.mzTolerance || (this.mzTolerance == null && mzTolerance != null)
            || !this.mzTolerance.equals(mzTolerance);
    this.mzTolerance = mzTolerance;

    if (changed)
      addAllAnnotationsToCharts();
  }

  private void addAllAnnotationsToCharts() {
    if (msmsAnnotations == null)
      return;

    removeAllAnnotationsFromCharts();

    for (AbstractMSMSIdentity a : msmsAnnotations)
      addAnnotationToCharts(a);
  }

  private void removeAllAnnotationsFromCharts() {
    forAllCharts(c -> {

    });
  }

  private void addAnnotationToCharts(AbstractMSMSIdentity ann) {
    if (mzTolerance != null)
      forAllCharts(c -> {
        PseudoSpectrumDataSet data = (PseudoSpectrumDataSet) c.getXYPlot().getDataset(0);
        data.addIdentity(mzTolerance, ann);
      });
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * all charts (ms1 and MS2)
   * 
   * @param op
   */
  public void forAllCharts(Consumer<JFreeChart> op) {
    if (group != null)
      group.forAllCharts(op);
  }


  /**
   * only ms2 charts
   * 
   * @param op
   */
  public void forAllMSMSCharts(Consumer<JFreeChart> op) {
    if (group == null || group.getList() == null)
      return;

    int start = msone == null ? 0 : 1;
    for (int i = start; i < group.getList().size(); i++)
      op.accept(group.getList().get(i).getChart());
  }
}
