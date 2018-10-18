package net.sf.mzmine.chartbasics.chartgroups;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import net.sf.mzmine.chartbasics.gestures.ChartGesture;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Entity;
import net.sf.mzmine.chartbasics.gestures.ChartGesture.Event;
import net.sf.mzmine.chartbasics.gestures.ChartGestureHandler;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.chartbasics.gui.wrapper.GestureMouseAdapter;
import net.sf.mzmine.chartbasics.listener.AxisRangeChangedListener;

/**
 * Combine the zoom of multiple charts
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ChartGroup {

  // max range of all charts
  private Range[] maxRange;

  // combine zoom of axes
  private boolean combineRangeAxes = false;
  private boolean combineDomainAxes = false;
  // click marker
  private boolean showClickDomainMarker = false;
  private boolean showClickRangeMarker = false;

  private List<ChartViewWrapper> list = null;
  private List<AxisRangeChangedListener> rangeListener;
  private List<AxisRangeChangedListener> domainListener;


  public ChartGroup(boolean showClickDomainMarker, boolean showClickRangeMarker,
      boolean combineDomainAxes, boolean combineRangeAxes) {
    this.combineRangeAxes = combineRangeAxes;
    this.combineDomainAxes = combineDomainAxes;
    this.showClickDomainMarker = showClickDomainMarker;
    this.showClickRangeMarker = showClickRangeMarker;
  }

  public void add(ChartViewWrapper chart) {
    if (list == null)
      list = new ArrayList<>();
    list.add(chart);

    // only if selected
    combineAxes(chart.getChart());
    addChartToMaxRange(chart.getChart());
    addClickMarker(chart);
  }

  public void add(ChartViewWrapper[] charts) {
    for (ChartViewWrapper c : charts)
      add(c);
  }

  public void add(List<ChartViewWrapper> charts) {
    for (ChartViewWrapper c : charts)
      add(c);
  }

  /**
   * Click marker to all charts
   * 
   * @param chart
   */
  private void addClickMarker(ChartViewWrapper chart) {
    GestureMouseAdapter m = chart.getGestureAdapter();
    if (m != null) {
      m.addGestureHandler(new ChartGestureHandler(new ChartGesture(Entity.PLOT, Event.MOVED), e -> {
        setCrosshair(e.getCoordinates());
      }));
    }
  }

  private void setCrosshair(Point2D pos) {
    if (pos == null)
      return;

    forAllCharts(chart -> {
      XYPlot p = chart.getXYPlot();
      if (showClickDomainMarker) {
        p.setDomainCrosshairValue(pos.getX());
        p.setDomainCrosshairVisible(true);
      }
      if (showClickRangeMarker) {
        p.setRangeCrosshairValue(pos.getY());
        p.setRangeCrosshairVisible(true);
      }
    });
  }

  /**
   * Perform operation on all charts
   * 
   * @param op
   */
  public void forAllCharts(Consumer<JFreeChart> op) {
    for (ChartViewWrapper c : list)
      op.accept(c.getChart());
  }

  /**
   * adds the charts range and domain range to the max ranges
   * 
   * @param chart
   */
  private void addChartToMaxRange(JFreeChart chart) {
    if (maxRange == null) {
      maxRange = new Range[2];
      if (hasDomainAxis(chart))
        maxRange[0] = chart.getXYPlot().getDomainAxis().getRange();
      if (hasRangeAxis(chart))
        maxRange[1] = chart.getXYPlot().getRangeAxis().getRange();
    } else {
      // domain
      Range nd = addRanges(maxRange[0], getDomainRange(chart));
      if (nd != null && (maxRange[0] == null || !nd.equals(maxRange[0])))
        domainHasChanged(nd);
      maxRange[0] = nd;

      // range axis
      nd = addRanges(maxRange[1], getRangeRange(chart));
      maxRange[1] = nd;
      if (nd != null && (maxRange[0] == null || !nd.equals(maxRange[0])))
        rangeHasChanged(nd);
    }
  }

  /**
   * 
   * @param chart
   * @return Domain axis range or null
   */
  private Range getDomainRange(JFreeChart chart) {
    if (hasDomainAxis(chart))
      return chart.getXYPlot().getDomainAxis().getRange();
    else
      return null;
  }

  /**
   * 
   * @param chart
   * @return range axis range or null
   */
  private Range getRangeRange(JFreeChart chart) {
    if (hasRangeAxis(chart))
      return chart.getXYPlot().getRangeAxis().getRange();
    else
      return null;
  }

  /**
   * Combines ranges to span all
   * 
   * @param a
   * @param b
   * @return
   */
  private Range addRanges(Range a, Range b) {
    if (a == null && b == null)
      return null;
    else if (a == null)
      return b;
    else if (b == null)
      return a;
    else {
      return new Range(Math.min(a.getLowerBound(), b.getLowerBound()),
          Math.max(a.getUpperBound(), b.getUpperBound()));
    }
  }

  /**
   * Combines the zoom of axes of all charts
   * 
   * @param chart
   */
  private void combineAxes(JFreeChart chart) {
    try {
      if (combineDomainAxes && hasDomainAxis(chart)) {
        if (domainListener == null)
          domainListener = new ArrayList<>();

        AxisRangeChangedListener listener = new AxisRangeChangedListener(null) {
          @Override
          public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
              Range newR) {
            domainHasChanged(newR);
          }
        };
        domainListener.add(listener);
        chart.getXYPlot().getDomainAxis().addChangeListener(listener);
      }
      if (combineRangeAxes && hasRangeAxis(chart)) {
        if (rangeListener == null)
          rangeListener = new ArrayList<>();

        AxisRangeChangedListener listener = new AxisRangeChangedListener(null) {

          @Override
          public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
              Range newR) {
            rangeHasChanged(newR);
          }
        };
        rangeListener.add(listener);
        chart.getXYPlot().getRangeAxis().addChangeListener(listener);
      }
    } catch (Exception e) {
    }
  }

  /**
   * Apply changes to all other charts
   * 
   * @param range
   */
  private void domainHasChanged(Range range) {
    if (combineDomainAxes) {
      forAllCharts(c -> {
        if (hasDomainAxis(c)) {
          ValueAxis axis = c.getXYPlot().getDomainAxis();
          if (!axis.getRange().equals(range))
            axis.setRange(range);
        }
      });
    }
  }

  /**
   * Apply changes to all other charts
   * 
   * @param range
   */
  private void rangeHasChanged(Range range) {
    if (combineRangeAxes) {
      forAllCharts(c -> {
        if (hasRangeAxis(c)) {
          ValueAxis axis = c.getXYPlot().getRangeAxis();
          if (!axis.getRange().equals(range))
            axis.setRange(range);
        }
      });
    }
  }

  private boolean hasDomainAxis(JFreeChart c) {
    return c.getXYPlot() != null && c.getXYPlot().getDomainAxis() != null;
  }

  private boolean hasRangeAxis(JFreeChart c) {
    return c.getXYPlot() != null && c.getXYPlot().getRangeAxis() != null;
  }

  public void remove(JFreeChart chart) {
    if (list != null) {
      int i = list.indexOf(chart);
      if (i >= 0) {
        list.remove(i);
        rangeListener.remove(i);
        domainListener.remove(i);
      }
    }
  }

  /**
   * COmbine zoom of axes
   * 
   * @param combineDomainAxes
   * @param combineRangeAxes
   */
  public void setCombineAxes(boolean combineDomainAxes, boolean combineRangeAxes) {
    if (combineDomainAxes != this.combineDomainAxes || combineRangeAxes != this.combineRangeAxes) {
      domainListener = null;
      rangeListener = null;
      this.combineRangeAxes = combineRangeAxes;
      this.combineDomainAxes = combineDomainAxes;
      forAllCharts(c -> {
        combineAxes(c);
      });
    }
  }

}