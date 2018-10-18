package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.msms;

import java.awt.BasicStroke;
import java.awt.Color;
import java.text.MessageFormat;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectraItemLabelGenerator;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectraRenderer;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrumDataSet;

public class SpectrumChartFactory {

  public static Scan getMSMSScan(PeakListRow row, RawDataFile raw) {
    Scan scan = null;
    if (raw != null) {
      Feature peak = row.getPeak(raw);
      if (peak == null)
        scan = raw.getScan(peak.getMostIntenseFragmentScanNumber());
    }
    if (scan == null)
      scan = row.getBestFragmentation();
    return scan;
  }

  public static PseudoSpectrumDataSet createMSMSDataSet(Scan scan) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();

    if (scan != null) {
      // data
      PseudoSpectrumDataSet series =
          new PseudoSpectrumDataSet(MessageFormat.format("MSMS for m/z={0} RT={1}",
              mzForm.format(scan.getPrecursorMZ()), rtForm.format(scan.getRetentionTime())), true);
      // for each row
      for (DataPoint dp : scan.getDataPoints()) {
        series.addDP(dp.getMZ(), dp.getIntensity(), null);
      }
      return series;
    } else
      return null;
  }

  /**
   * Also adds the label gen
   * 
   * @param row
   * @param raw
   * @param showTitle
   * @param showLegend
   * @return
   */
  public static EChartPanel createChartPanel(PeakListRow row, RawDataFile raw, boolean showTitle,
      boolean showLegend) {
    JFreeChart chart = createChart(row, raw, showTitle, showLegend);
    //
    EChartPanel pn = new EChartPanel(chart);
    XYItemRenderer renderer = chart.getXYPlot().getRenderer();
    PseudoSpectraItemLabelGenerator labelGenerator = new PseudoSpectraItemLabelGenerator(pn);
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setDefaultItemLabelPaint(Color.BLACK);
    renderer.setSeriesItemLabelGenerator(0, labelGenerator);
    return pn;
  }


  public static JFreeChart createChart(PeakListRow row, RawDataFile raw, boolean showTitle,
      boolean showLegend) {
    Scan scan = getMSMSScan(row, raw);
    if (scan == null)
      return null;

    PseudoSpectrumDataSet dataset = createMSMSDataSet(scan);
    //
    if (dataset == null)
      return null;
    //
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    JFreeChart chart = ChartFactory.createXYLineChart(
        MessageFormat.format("MSMS for m/z={0} RT={1}", mzForm.format(scan.getPrecursorMZ()),
            rtForm.format(scan.getRetentionTime())), // title
        "m/z", // x-axis label
        "Intensity", // y-axis label
        dataset, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // isotopeFlag, // create legend?
        true, // generate tooltips?
        false // generate URLs?
    );
    chart.setBackgroundPaint(Color.white);
    chart.getTitle().setVisible(false);
    // set the plot properties
    XYPlot plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(RectangleInsets.ZERO_INSETS);

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzForm);
    xAxis.setUpperMargin(0.08);
    xAxis.setLowerMargin(0.00);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));
    xAxis.setAutoRangeIncludesZero(true);
    xAxis.setMinorTickCount(5);

    // set the Y axis (intensity) properties
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setNumberFormatOverride(intensityFormat);
    yAxis.setUpperMargin(0.20);

    PseudoSpectraRenderer renderer = new PseudoSpectraRenderer(Color.BLACK, false);
    plot.setRenderer(0, renderer);
    plot.setRenderer(1, renderer);
    plot.setRenderer(2, renderer);
    renderer.setSeriesVisibleInLegend(1, false);
    renderer.setSeriesPaint(2, Color.ORANGE);
    //
    chart.getTitle().setVisible(showTitle);
    chart.getLegend().setVisible(showLegend);
    //
    addPrecursorMarker(chart, scan);
    return chart;
  }

  /**
   * Precursor mass marker
   * 
   * @param chart
   * @param scan
   */
  private static void addPrecursorMarker(JFreeChart chart, Scan scan) {
    chart.getXYPlot().addDomainMarker(
        new ValueMarker(scan.getPrecursorMZ(), Color.ORANGE, new BasicStroke(1.5f)));
  }
}