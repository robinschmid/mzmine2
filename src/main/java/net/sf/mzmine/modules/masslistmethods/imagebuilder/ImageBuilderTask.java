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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.sf.mzmine.chartbasics.EChartFactory;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.masslistmethods.imagebuilder.ImageBuilderParameters.Weight;
import net.sf.mzmine.modules.masslistmethods.imagebuilder.fitting.Fit;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.EHistogramDialog;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.HistogramData;
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
  private Range<Double> rtRange;
  private MZTolerance mzTolerance;
  private double minimumHeight;
  private double binWidth;

  private Weight weight;

  private SimplePeakList newPeakList;
  private double sigmaFactor = 3;

  private ParameterSet parameters;

  private int[] scanNumbers;
  private int tooSmall = 0;

  /**
   * @param dataFile
   * @param parameters
   */
  public ImageBuilderTask(MZmineProject project, RawDataFile dataFile, ParameterSet parameters) {

    this.parameters = parameters;
    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection = parameters.getParameter(ImageBuilderParameters.scanSelection).getValue();
    this.massListName = parameters.getParameter(ImageBuilderParameters.massList).getValue();

    this.mzRange = parameters.getParameter(ImageBuilderParameters.mzRange).getValue();
    this.rtRange = parameters.getParameter(ImageBuilderParameters.rtRange).getValue();

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

    // Create new peak list
    newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

    Feature[] chromatograms;

    // all selected scans
    scans = scanSelection.getMatchingScans(dataFile);
    scanNumbers = scanSelection.getMatchingScanNumbers(dataFile);
    totalScans = scans.length;

    // histo data
    DoubleArrayList data = new DoubleArrayList();

    for (Scan scan : scans) {
      if (isCanceled())
        return;

      // retention time in range
      if (rtRange.contains(scan.getRetentionTime())) {
        // go through all mass lists
        MassList massList = scan.getMassList(massListName);
        if (massList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
              + " does not have a mass list " + massListName);
          return;
        }
        DataPoint mzValues[] = massList.getDataPoints();

        // insert all mz in order and count them
        Arrays.stream(mzValues).mapToDouble(dp -> dp.getMZ()).filter(mz -> mzRange.contains(mz))
            .forEach(mz -> data.add(mz));
        processedScans++;
      }
    }
    if (!data.isEmpty()) {
      // to array
      double[] dat = new double[data.size()];
      for (int i = 0; i < data.size(); i++)
        dat[i] = data.get(i);

      // create histogram dialog
      EHistogramDialog dialog =
          new EHistogramDialog("m/z distribution", new HistogramData(dat), binWidth);
      dialog.setVisible(true);

      // apply binning
      List<DataPoint> histo = EChartFactory.createHistoList(data, binWidth, mzRange.lowerEndpoint(),
          mzRange.upperEndpoint(), null);

      // pick masses by gaussian fit
      double start = -1;
      double end = 0;
      int dpCount = 0;
      // double[] {normFactor, mean, sigma}
      double[] fit = null;

      // store all
      List<Fit> fitList = new ArrayList<>();

      // for all data points
      for (int i = 0; i < histo.size(); i++) {
        DataPoint dp = histo.get(i);
        // search for start (use one 0 value)
        if (dpCount == 0) {
          // start
          if (dp.getIntensity() > 0) {
            start = dp.getMZ();
            dpCount = 1;
          }
        } else {
          // search for end ( use one 0 value)
          if (dp.getIntensity() == 0
              && (i == histo.size() - 1 || histo.get(i + 1).getIntensity() == 0)) {
            // end found: 2 times 0
            end = dp.getMZ();

            if (dpCount > 3) {
              // Gaussian fit
              fit = EChartFactory.gaussianFit(histo, start, end);
              Fit fitObject = new Fit(start, end, dpCount, fit);
              fitList.add(fitObject);
              logger.info(fitObject.toString());
              // check correct fitting

              // add to peak list
              double sigma = fitObject.getSigma() * sigmaFactor;
              double meanMZ = fitObject.getMean();
              addToPeakList(newPeakList, scans, meanMZ, sigma);
            }

            // reset
            dpCount = 0;
            start = -1;
          } else {
            // add data point
            dpCount++;
          }
        }
      }

      if (newPeakList.getNumberOfRows() > 0) {
        // Add new peaklist to the project
        project.addPeakList(newPeakList);

        // Add quality parameters to peaks
        QualityParameters.calculateQualityParameters(newPeakList);
      }
    } else {
      throw new MSDKRuntimeException("Data was empty. Review your selected filters.");
    }

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished mz distribution histogram on " + dataFile);
  }

  private void addToPeakList(SimplePeakList pkl, Scan[] scans, double meanMZ, double sigma) {
    try {
      Feature f = FeatureCreator.createFeature(dataFile, massListName, scans, meanMZ, sigma);
      // only add if meet filter
      if (f.getHeight() >= minimumHeight) {
        SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
        newPeakID++;
        newRow.addPeak(dataFile, f);
        newPeakList.addRow(newRow);
        logger.info("Added chromatogram #" + newPeakList.getNumberOfRows() + " at " + f.getMZ());
      } else {
        tooSmall++;
        logger.info("NOT added TOO SMALL chromatogram #" + tooSmall + " at " + f.getMZ());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }



  /**
   * Add peaklist to project, delete old if requested, add description to result
   */
  public void addResultToProject(PeakList resultPeakList) {
    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("Image builder task", parameters));
  }
}
