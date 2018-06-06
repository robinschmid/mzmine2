/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.masslistmethods.imagebuilder.charts;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.SaveImage;
import net.sf.mzmine.util.SaveImage.FileType;
import net.sf.mzmine.util.chartexport.ChartExportUtil;

/**
 * 
 */
public class SignalDistributionPlot extends ChartPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private JFreeChart chart;
  private XYPlot plot;

  // peak labels color
  private static final Color labelsColor = Color.darkGray;

  // grid color
  private static final Color gridColor = Color.lightGray;

  // title font
  private static final Font titleFont = new Font("SansSerif", Font.BOLD, 12);
  private static final Font subTitleFont = new Font("SansSerif", Font.PLAIN, 11);

  // legend
  private static final Font legendFont = new Font("SansSerif", Font.PLAIN, 11);


  public SignalDistributionPlot(String title, String xlabel, String ylabel, XYDataset data) {

    super(null, true);

    setBackground(Color.white);
    setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

    // initialize the chart by default time series chart from factory
    chart = ChartFactory.createXYLineChart(title, // title
        xlabel, // x-axis label
        ylabel, // y-axis label
        data, // data set
        PlotOrientation.VERTICAL, // orientation
        true, // isotopeFlag, // create legend?
        true, // generate tooltips?
        false // generate URLs?
    );
    chart.setBackgroundPaint(Color.white);
    setChart(chart);

    // legend constructed by ChartFactory
    LegendTitle legend = chart.getLegend();
    legend.setItemFont(legendFont);
    legend.setFrame(BlockBorder.NONE);

    // disable maximum size (we don't want scaling)
    setMaximumDrawWidth(Integer.MAX_VALUE);
    setMaximumDrawHeight(Integer.MAX_VALUE);
    setMinimumDrawHeight(0);

    // set the plot properties
    plot = chart.getXYPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

    // set rendering order
    plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

    // set grid properties
    plot.setDomainGridlinePaint(gridColor);
    plot.setRangeGridlinePaint(gridColor);

    // set crosshair (selection) properties
    plot.setDomainCrosshairVisible(false);
    plot.setRangeCrosshairVisible(false);

    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    // set the X axis (retention time) properties
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setNumberFormatOverride(mzFormat);
    xAxis.setUpperMargin(0.001);
    xAxis.setLowerMargin(0.001);
    xAxis.setTickLabelInsets(new RectangleInsets(0, 0, 20, 20));

    // set focusable state to receive key events
    setFocusable(true);

    // register key handlers
    GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("LEFT"), this, "PREVIOUS_PEAK");
    GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke("RIGHT"), this, "NEXT_PEAK");
    GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('+'), this, "ZOOM_IN");
    GUIUtils.registerKeyHandler(this, KeyStroke.getKeyStroke('-'), this, "ZOOM_OUT");

    JPopupMenu popupMenu = getPopupMenu();

    // Robin Schmid
    // add image export
    ChartExportUtil.addExportMenu(this);

    // Add EMF and EPS options to the save as menu
    JMenuItem saveAsMenu = (JMenuItem) popupMenu.getComponent(3);
    GUIUtils.addMenuItem(saveAsMenu, "EMF...", this, "SAVE_EMF");
    GUIUtils.addMenuItem(saveAsMenu, "EPS...", this, "SAVE_EPS");
  }

  @Override
  public void actionPerformed(final ActionEvent event) {

    super.actionPerformed(event);

    final String command = event.getActionCommand();


    if ("PREVIOUS_PEAK".equals(command)) {
      jumpToPrevPeak();
    } else if ("NEXT_PEAK".equals(command)) {
      jumpToNextPeak();
    } else if ("SAVE_EMF".equals(command)) {

      JFileChooser chooser = new JFileChooser();
      FileNameExtensionFilter filter = new FileNameExtensionFilter("EMF Image", "EMF");
      chooser.setFileFilter(filter);
      int returnVal = chooser.showSaveDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        String file = chooser.getSelectedFile().getPath();
        if (!file.toLowerCase().endsWith(".emf"))
          file += ".emf";

        int width = (int) this.getSize().getWidth();
        int height = (int) this.getSize().getHeight();

        // Save image
        SaveImage SI = new SaveImage(getChart(), file, width, height, FileType.EMF);
        new Thread(SI).start();

      }
    } else if ("SAVE_EPS".equals(command)) {

      JFileChooser chooser = new JFileChooser();
      FileNameExtensionFilter filter = new FileNameExtensionFilter("EPS Image", "EPS");
      chooser.setFileFilter(filter);
      int returnVal = chooser.showSaveDialog(null);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        String file = chooser.getSelectedFile().getPath();
        if (!file.toLowerCase().endsWith(".eps"))
          file += ".eps";

        int width = (int) this.getSize().getWidth();
        int height = (int) this.getSize().getHeight();

        // Save image
        SaveImage SI = new SaveImage(getChart(), file, width, height, FileType.EPS);
        new Thread(SI).start();

      }

    }
  }


  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToPrevPeak() {
    XYDataset data = getXYPlot().getDataset(0);
    // get center of zoom
    ValueAxis x = getXYPlot().getDomainAxis();
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
    XYDataset data = getXYPlot().getDataset(0);
    // get center of zoom
    ValueAxis x = getXYPlot().getDomainAxis();
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
    XYDataset data = getXYPlot().getDataset(0);
    double maxy = data.getYValue(0, i);
    getXYPlot().getRangeAxis().setRange(0, maxy);

    double lower = data.getXValue(0, i);
    for (int x = i; x >= 0; x--) {
      if (data.getYValue(0, x) == 0) {
        lower = data.getXValue(0, x);
        break;
      }
    }
    double upper = data.getXValue(0, i);
    for (int x = i; x < data.getItemCount(0); x++) {
      if (data.getYValue(0, x) == 0) {
        upper = data.getXValue(0, x);
        break;
      }
    }

    getXYPlot().getDomainAxis().setRange(lower, upper);

    // set constant rangezoom
    getXYPlot().getDomainAxis().setRangeAroundValue(data.getXValue(0, i), 0.05);
  }

  public XYPlot getXYPlot() {
    return plot;
  }

  /**
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  public void mouseClicked(MouseEvent event) {

    // let the parent handle the event (selection etc.)
    super.mouseClicked(event);

    // request focus to receive key events
    requestFocus();
  }

}
