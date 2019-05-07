package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.pseudospectra;

import java.awt.Color;
import java.text.NumberFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYSeries;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;

public class PseudoSpectrum {

  public static PseudoSpectrumDataSet createDataSet(PKLRowGroup group, RawDataFile raw,
      boolean sum) {
    // data
    PseudoSpectrumDataSet series = new PseudoSpectrumDataSet("G" + group.getGroupID(), true);
    // add all isotopes as a second series:
    XYSeries isoSeries = new XYSeries("Isotopes", true);
    // raw isotopes in a different color
    XYSeries rawIsoSeries = new XYSeries("Raw isotope pattern", true);
    // for each row
    for (PeakListRow row : group) {
      String annotation = null;
      for (PeakIdentity id : row.getPeakIdentities()) {
        if (id instanceof ESIAdductIdentity) {
          if (annotation == null)
            annotation = ((ESIAdductIdentity) id).getAdduct();
          else
            annotation += "\n" + ((ESIAdductIdentity) id).getAdduct();
        }
      }
      // sum -> heighest peak
      if (sum)
        series.addDP(row.getAverageMZ(), row.getBestPeak().getHeight(), annotation);
      else {
        Feature f = row.getPeak(raw);
        if (f != null)
          series.addDP(f.getMZ(), f.getHeight(), null);
      }
      // add isotopes
      IsotopePattern pattern = row.getBestIsotopePattern();
      if (pattern != null) {
        if (pattern.getStatus().equals(IsotopePatternStatus.RAW))
          for (DataPoint dp : pattern.getDataPoints())
            rawIsoSeries.add(dp.getMZ(), dp.getIntensity());
        else
          for (DataPoint dp : pattern.getDataPoints())
            isoSeries.add(dp.getMZ(), dp.getIntensity());
      }
    }
    series.addSeries(isoSeries);
    series.addSeries(rawIsoSeries);
    return series;
  }

  public static ChartPanel createChart(PKLRowGroup group, RawDataFile raw, boolean sum) {
    PseudoSpectrumDataSet dataset = createDataSet(group, raw, sum);

    //
    JFreeChart chart = ChartFactory.createXYLineChart("Pseudo spectrum: G" + group.getGroupID(), // title
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

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzFormat);
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
    ChartPanel pn = new ChartPanel(chart);
    PseudoSpectraItemLabelGenerator labelGenerator = new PseudoSpectraItemLabelGenerator(pn);
    renderer.setDefaultItemLabelsVisible(true);
    renderer.setDefaultItemLabelPaint(Color.BLACK);
    renderer.setSeriesItemLabelGenerator(0, labelGenerator);
    //
    return pn;
  }
}
