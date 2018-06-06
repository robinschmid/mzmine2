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

package net.sf.mzmine.modules.masslistmethods.imagebuilder;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.swing.JDialog;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import net.sf.mzmine.MyStuff.histogram.HistogramData;
import net.sf.mzmine.chartbasics.EChartFactory;
import net.sf.mzmine.chartbasics.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.modules.masslistmethods.chromatogrambuilder.Chromatogram;
import net.sf.mzmine.modules.masslistmethods.imagebuilder.ImageBuilderParameters.Weight;
import net.sf.mzmine.modules.masslistmethods.imagebuilder.charts.MassListMzDistribution;
import net.sf.mzmine.modules.visualization.mzhistogram.EHistogramDialog;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class ImageBuilderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private ScanSelection scanSelection;
  private int newPeakID = 1;
  private Scan[] scans;

  // User parameters
  private String suffix, massListName;
  private Range<Double> mzRange;
  private MZTolerance mzTolerance;
  private double minimumHeight;
  private double binWidth;

  private Weight weight;

  private SimplePeakList newPeakList;

  /**
   * @param dataFile
   * @param parameters
   */
  public ImageBuilderTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters) {

    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getParameter(ImageBuilderParameters.scanSelection).getValue();
    this.massListName = parameters.getParameter(ImageBuilderParameters.massList).getValue();

    this.mzRange = parameters.getParameter(ImageBuilderParameters.mzRange).getValue();

    this.mzTolerance = parameters.getParameter(ImageBuilderParameters.mzTolerance).getValue();
    this.minimumHeight = parameters.getParameter(ImageBuilderParameters.minimumHeight).getValue();
    this.binWidth = parameters.getParameter(ImageBuilderParameters.binWidth).getValue();

    this.weight = parameters.getParameter(ImageBuilderParameters.weight).getValue();

    this.suffix = parameters.getParameter(ImageBuilderParameters.suffix).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  public String getTaskDescription() {
    return "Detecting images in " + dataFile;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  public double getFinishedPercentage() {
    if (totalScans == 0)
      return 0;
    else
      return (double) processedScans / totalScans;
  }

  public RawDataFile getDataFile() {
    return dataFile;
  }


  /**
   * @see Runnable#run()
   */
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Started image builder on " + dataFile);

    scans = scanSelection.getMatchingScans(dataFile);
    int allScanNumbers[] = scanSelection.getMatchingScanNumbers(dataFile);
    totalScans = scans.length;

    // Create new peak list
    newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

    Chromatogram[] chromatograms;

    // Create new histogram
    double range = mzRange.upperEndpoint() - mzRange.lowerEndpoint();
    int size = (int) (range / binWidth) + 1;
    int[] bins = new int[size];

    // histo 3
    List<Double> data3 = new ArrayList<Double>();

    // insert all mz in order and count them
    // mz as integer to avoid floating point * decimals
    // m/z number
    TreeMap<Integer, Double> signals = new TreeMap<Integer, Double>();


    int decimals = 3;
    double factor = Math.pow(10, decimals);

    for (Scan scan : scans) {

      if (isCanceled())
        return;

      MassList massList = scan.getMassList(massListName);
      if (massList == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
            + " does not have a mass list " + massListName);
        return;
      }

      DataPoint mzValues[] = massList.getDataPoints();

      if (mzValues == null) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Mass list " + massListName + " does not contain m/z values for scan #"
            + scan.getScanNumber() + " of file " + dataFile);
        return;
      }

      // minimum distance between two detected masses
      double minDistance = Double.POSITIVE_INFINITY;
      double lastMZ = -1;

      // add all m/z values in bins
      // insert all mz in order and count them
      double increment = 1;
      try {
        for (int i = 0; i < mzValues.length; i++) {
          double cMZ = mzValues[i].getMZ();
          // minimum distance
          if (lastMZ != -1 && Math.abs(cMZ - lastMZ) < minDistance)
            minDistance = Math.abs(cMZ - lastMZ);

          // save as integer to get around floating point
          Integer mz = (int) Math.round(cMZ * factor);


          // weighting
          switch (weight) {
            case None:
              increment = 1;
              break;
            case Linear:
              increment = mzValues[i].getIntensity();
              break;
            case log10:
              increment = Math.log10(mzValues[i].getIntensity());
              break;
          }

          // add increment to value or create new
          Double number = signals.get(mz);
          if (number != null) {
            signals.put(mz, number + increment);
          } else
            signals.put(mz, increment);

          // add to histo data
          EChartFactory.addValueToHistoArray(bins, cMZ, binWidth, mzRange.lowerEndpoint());
          data3.add(cMZ);
          //
          lastMZ = cMZ;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      processedScans++;
    }

    addZeros(signals);


    MassListMzDistribution frame = new MassListMzDistribution();
    frame.createChart(signals, decimals);
    frame.setVisible(true);

    // create histo dialog 2
    XYSeries series = new XYSeries("m/z distr", false);
    // add all m/z values
    for (int i = 0; i < bins.length; i++)
      series.add(mzRange.lowerEndpoint() + binWidth / 2 + binWidth * i, bins[i]);
    JFreeChart chart = EChartFactory.createHistogram(series, binWidth, "n");
    EChartPanel cp = new EChartPanel(chart);
    JDialog d2 = new JDialog();
    d2.getContentPane().setLayout(new BorderLayout());
    d2.getContentPane().add(cp, BorderLayout.CENTER);
    d2.setVisible(true);


    // create histogram dialog
    double[] hist3 = Doubles.toArray(data3);
    EHistogramDialog d =
        new EHistogramDialog("m/z distribution", new HistogramData(hist3), binWidth);
    d.setVisible(true);



    setStatus(TaskStatus.FINISHED);

    logger.info("Finished chromatogram builder on " + dataFile);

  }

  private void addZeros(TreeMap<Integer, Double> signals) {
    // add zeros within half of minimum spacing
    Iterator<Entry<Integer, Double>> it = signals.entrySet().iterator();
    if (it.hasNext()) {
      // temp map
      TreeMap<Integer, Double> tmp = new TreeMap<Integer, Double>();
      //
      Entry<Integer, Double> last = it.next();
      for (int i = 1; i < signals.size() && it.hasNext(); i++) {
        Entry<Integer, Double> e = it.next();
        // is the spacing higher than 1 significance?
        // the key is the mz value times a factor
        if (e.getKey() - last.getKey() > 2) {
          // add end of peak and start of peak
          tmp.put(last.getKey() + 1, 0.0);
          tmp.put(e.getKey() - 1, 0.0);
        } else if (e.getKey() - last.getKey() > 1) {
          // add separation between two values that are only separated by 1
          tmp.put(last.getKey() + 1, 0.0);
        }
        last = e;
      }

      // add all
      signals.putAll(tmp);
    }
  }

}
