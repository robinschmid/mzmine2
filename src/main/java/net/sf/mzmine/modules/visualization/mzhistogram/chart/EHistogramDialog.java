package net.sf.mzmine.modules.visualization.mzhistogram.chart;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.util.GUIUtils;

public class EHistogramDialog extends HistogramDialog implements ActionListener {
  private static final long serialVersionUID = 1L;
  private JCheckBox cbKeepSameXaxis;

  /**
   * Create the dialog. Auto detect binWidth
   * 
   * @wbp.parser.constructor
   */
  public EHistogramDialog(String title, HistogramData data) {
    this(title, data, 0);
  }

  /**
   * 
   * @param title
   * @param data
   * @param binWidth zero (0) for auto detection, -1 to keep last binWidth
   */
  public EHistogramDialog(String title, HistogramData data, double binWidth) {
    super(title, data, binWidth);
    addKeyBindings();
  }


  private void addKeyBindings() {
    // set focusable state to receive key events
    setFocusable(true);

    // register key handlers
    JPanel pn = (JPanel) this.getContentPane();
    pn.setRequestFocusEnabled(true);
    GUIUtils.registerKeyHandler(pn, KeyStroke.getKeyStroke("LEFT"), this, "PREVIOUS_PEAK");
    GUIUtils.registerKeyHandler(pn, KeyStroke.getKeyStroke("RIGHT"), this, "NEXT_PEAK");

    JPanel pnJump = new JPanel();
    getHistoPanel().getBoxSettings().add(pnJump);

    cbKeepSameXaxis = new JCheckBox("keep same x-axis length");
    pnJump.add(cbKeepSameXaxis);

    JButton btnPrevious = new JButton("<");
    btnPrevious.setToolTipText("Jump to previous distribution (use left arrow");
    btnPrevious.addActionListener(e -> jumpToPrevPeak());
    pnJump.add(btnPrevious);

    JButton btnNext = new JButton(">");
    btnPrevious.setToolTipText("Jump to previous distribution (use right arrow");
    btnNext.addActionListener(e -> jumpToNextPeak());
    pnJump.add(btnNext);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final String command = event.getActionCommand();
    if ("PREVIOUS_PEAK".equals(command)) {
      jumpToPrevPeak();
    } else if ("NEXT_PEAK".equals(command)) {
      jumpToNextPeak();
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToPrevPeak() {
    XYPlot plot = getXYPlot();
    if (plot == null)
      return;

    XYDataset data = plot.getDataset(0);
    // get center of zoom
    ValueAxis x = plot.getDomainAxis();
    double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

    boolean started = false;

    for (int i = data.getItemCount(0) - 1; i >= 1; i--) {
      double mz = data.getXValue(0, i);
      if (mz < mid) {
        // wait for y to be 0 to start the search for a new peak
        if (!started) {
          if (data.getYValue(0, i) == 0)
            started = true;
        } else {
          // intensity drops?
          if (data.getYValue(0, i - 1) != 0 && data.getYValue(0, i) >= 100
              && data.getYValue(0, i - 1) < data.getYValue(0, i)) {
            // peak found with max at i
            setZoomAroundPeakAt(i);
            return;
          }
        }
      }
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToNextPeak() {
    XYPlot plot = getXYPlot();
    if (plot == null)
      return;

    XYDataset data = plot.getDataset(0);
    // get center of zoom
    ValueAxis x = plot.getDomainAxis();
    // mid of range
    double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

    boolean started = false;

    for (int i = 0; i < data.getItemCount(0) - 1; i++) {
      double mz = data.getXValue(0, i);
      if (mz > mid) {
        // wait for y to be 0 to start the search for a new peak
        if (!started) {
          if (data.getYValue(0, i) == 0)
            started = true;
        } else {
          // intensity drops?
          if (data.getYValue(0, i + 1) != 0 && data.getYValue(0, i) >= 100
              && data.getYValue(0, i + 1) < data.getYValue(0, i)) {
            // peak found with max at i
            setZoomAroundPeakAt(i);
            return;
          }
        }
      }
    }
  }

  /**
   * Set zoom factor around peak at data point i
   * 
   * @param i
   */
  private void setZoomAroundPeakAt(int i) {
    XYPlot plot = getXYPlot();
    if (plot == null)
      return;


    XYDataset data = plot.getDataset(0);
    // auto range y
    double maxy = data.getYValue(0, i);
    plot.getRangeAxis().setRange(0, maxy);


    // keep same domain axis range length
    boolean keepRange = cbKeepSameXaxis.isSelected();

    // find lower bound (where y=0)
    double lower = data.getXValue(0, i);
    for (int x = i; x >= 0; x--) {
      if (data.getYValue(0, x) == 0) {
        lower = data.getXValue(0, x);
        break;
      }
    }
    // find upper bound /where y=0)
    double upper = data.getXValue(0, i);
    for (int x = i; x < data.getItemCount(0); x++) {
      if (data.getYValue(0, x) == 0) {
        upper = data.getXValue(0, x);
        break;
      }
    }

    if (keepRange) {
      // set constant range zoom
      double length = plot.getDomainAxis().getRange().getLength();
      plot.getDomainAxis().setRangeAroundValue(data.getXValue(0, i), length);
    } else {
      // set range directly around peak
      plot.getDomainAxis().setRange(lower, upper);
    }

    // auto gaussian fit
    if (getHistoPanel().isGaussianFitEnabled()) {
      // find
      getHistoPanel().setGaussianFitRange(lower, upper);
    }
  }

  private XYPlot getXYPlot() {
    ChartPanel chart = getHistoPanel().getChartPanel();
    if (chart != null)
      return chart.getChart().getXYPlot();
    else
      return null;
  }

  public JCheckBox getCbKeepSameXaxis() {
    return cbKeepSameXaxis;
  }
}
