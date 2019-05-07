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

package net.sf.mzmine.modules.masslistmethods.imagebuildersimple;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import io.github.msdk.MSDKRuntimeException;
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
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.masslistmethods.imagebuildersimple.SimpleImageBuilderParameters.Weight;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class SimpleImageBuilderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  private NumberFormat mzform, rtform, iform;

  // scan counter
  private int processedScans = 0, totalScans;
  private ScanSelection scanSelection;
  private int newPeakID = 1;
  private Scan[] scans;

  // User parameters
  private String suffix, massListName;
  private Range<Double> mzRange;

  //
  private boolean useRTRange;
  private Range<Double> rtRange;
  private MZTolerance mzTolerance;
  private double binWidth;


  private SimplePeakList newPeakList;
  private double sigmaFactor = 3;

  private ParameterSet parameters;

  private int[] scanNumbers;
  private boolean showResult = false;

  // stats
  private int countTooSmall = 0;
  private int countFitSmallerThanMZTolerance = 0;

  private Weight weight;

  // filter
  private double minimumHeight;
  private int minScans;
  private double minScanPerc;

  /**
   * @param dataFile
   * @param parameters
   */
  public SimpleImageBuilderTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters) {

    this.parameters = parameters;
    this.project = project;
    this.dataFile = dataFile;
    this.scanSelection =
        parameters.getParameter(SimpleImageBuilderParameters.scanSelection).getValue();
    this.massListName = parameters.getParameter(SimpleImageBuilderParameters.massList).getValue();

    this.mzRange = parameters.getParameter(SimpleImageBuilderParameters.mzRange).getValue();
    this.useRTRange = parameters.getParameter(SimpleImageBuilderParameters.rtRange).getValue();
    if (useRTRange)
      this.rtRange = parameters.getParameter(SimpleImageBuilderParameters.rtRange)
          .getEmbeddedParameter().getValue();

    this.minimumHeight =
        parameters.getParameter(SimpleImageBuilderParameters.minimumHeight).getValue();

    this.minScans = parameters.getParameter(SimpleImageBuilderParameters.minimumScans).getValue();
    this.minScanPerc =
        parameters.getParameter(SimpleImageBuilderParameters.minimumScanPerc).getValue();

    this.binWidth = parameters.getParameter(SimpleImageBuilderParameters.binWidth).getValue();
    this.weight = parameters.getParameter(SimpleImageBuilderParameters.weight).getValue();

    mzTolerance = parameters.getParameter(SimpleImageBuilderParameters.mzTolerance).getValue();

    this.suffix = parameters.getParameter(SimpleImageBuilderParameters.suffix).getValue();

    rtform = MZmineCore.getConfiguration().getRTFormat();
    mzform = MZmineCore.getConfiguration().getMZFormat();
    iform = MZmineCore.getConfiguration().getIntensityFormat();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Detecting images in " + dataFile;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
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
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Started simple image builder on " + dataFile);

    // Create new peak list
    newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

    // all selected scans
    scans = scanSelection.getMatchingScans(dataFile);
    scanNumbers = scanSelection.getMatchingScanNumbers(dataFile);
    totalScans = scans.length;

    double minimumMZDist;
    try {
      minimumMZDist = getMinimumMZDistance(scans, massListName);
    } catch (Exception e1) {
      return;
    }
    logger.info("minimum mz distance in all mass lists is " + minimumMZDist);

    // reset progress
    processedScans = 0;

    // half overlap
    binWidth /= 2.0;

    // store max intensities in bins across all scans
    double datawidth = (mzRange.upperEndpoint() - mzRange.lowerEndpoint());
    // overlap = 0.5
    int cbin = (int) Math.ceil(datawidth / binWidth);

    // data in bins
    double[] maxIntensityInBin = new double[cbin + 1];
    double[] mzBin = new double[cbin + 1];
    double[] intensityBin = new double[cbin + 1];
    int[] countBin = new int[cbin + 1];

    for (Scan scan : scans) {
      if (isCanceled())
        return;

      // retention time in range
      if (!useRTRange || rtRange.contains(scan.getRetentionTime())) {
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
        for (DataPoint dp : mzValues) {
          // filter
          if (mzRange.contains(dp.getMZ())) {
            // get bin index
            int bini = getBinIndex(mzBin.length, dp.getMZ(), mzRange.lowerEndpoint(), binWidth);

            // add intensity mz and count
            intensityBin[bini] += dp.getIntensity();
            mzBin[bini] += dp.getMZ();
            countBin[bini]++;

            // max intensitiy
            if (maxIntensityInBin[bini] < dp.getIntensity())
              maxIntensityInBin[bini] = dp.getIntensity();
          }
        }

        processedScans++;
      }
    }
    processedScans = 0;

    // every second bin is added to its previous bin
    for (int i = 0; i < countBin.length - 1; i++) {
      mzBin[i] += mzBin[i + 1];
      countBin[i] += countBin[i + 1];
      intensityBin[i] += intensityBin[i + 1];
      maxIntensityInBin[i] = Math.max(maxIntensityInBin[i], maxIntensityInBin[i + 1]);
    }

    // average
    for (int i = 0; i < countBin.length; i++) {
      mzBin[i] = mzBin[i] / countBin[i];
      intensityBin[i] = intensityBin[i] / countBin[i];
    }

    List<Feature> features = new ArrayList<>();
    // percentage done
    processedScans = 0;
    totalScans = countBin.length;
    // add bins to peaklist
    for (int i = 0; i < countBin.length; i++) {
      // filter by min intensity
      // filter by min number of scans
      // filter by min percentage in scans
      if (maxIntensityInBin[i] >= minimumHeight && countBin[i] >= minScans
          && countBin[i] / (double) scans.length >= minScanPerc) {
        // add to peak list
        // binWidth has half overlap
        double sigma = binWidth * 2;
        double center = mzRange.lowerEndpoint() + binWidth * (i + 1);
        double avgMZ = mzBin[i];
        // logger.info("Adding bin " + i + " with avg m/z=" + avgMZ + " at center (sigma) " +
        // center+ "(" + sigma + ")");
        Feature f = createFeature(scans, center, sigma);
        if (f != null)
          features.add(f);
      }

      processedScans++;
    }

    if (!features.isEmpty()) {
      // filter duplicates
      for (int i = 0; i < features.size() - 1; i++) {
        Feature r = features.get(i);
        Feature r2 = features.get(i + 1);
        if (mzTolerance.checkWithinTolerance(r.getMZ(), r2.getMZ())) {
          // remove the one with the lowest number of scans
          if (r.getScanNumbers().length < r2.getScanNumbers().length)
            features.remove(i);
          else
            features.remove(i + 1);
        }
      }

      // add to peaklist
      for (Feature f : features)
        addToPeakList(newPeakList, f);

      //
      logger.info("Adding peaklist to project: " + newPeakList.getName());
      // Add new peaklist to the project
      project.addPeakList(newPeakList);

      // Add quality parameters to peaks
      logger.info("Calculating quality parameters for peaklist: " + newPeakList.getName());
      QualityParameters.calculateQualityParameters(newPeakList);

    } else

    {
      throw new MSDKRuntimeException("Data was empty. Review your selected filters.");
    }

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished simple image builder on " + dataFile);
  }

  /**
   * 
   * @param scans2
   * @param massListName2
   * @return the minimum distance between two picked mz in masslists
   */
  private double getMinimumMZDistance(Scan[] scans, String massListName) throws Exception {
    double minDist = Double.POSITIVE_INFINITY;
    for (Scan scan : scans) {
      if (isCanceled())
        return 0;

      // retention time in range
      if (!useRTRange || rtRange.contains(scan.getRetentionTime())) {
        // go through all mass lists
        MassList massList = scan.getMassList(massListName);
        if (massList == null) {
          setStatus(TaskStatus.ERROR);
          setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
              + " does not have a mass list " + massListName);
          throw new MSDKRuntimeException("Scan " + dataFile + " #" + scan.getScanNumber()
              + " does not have a mass list " + massListName);
        }
        DataPoint mzValues[] = massList.getDataPoints();

        // insert all mz in order and count them
        for (int i = 0; i < mzValues.length - 1; i++) {

          if (mzRange.contains(mzValues[i + 1].getMZ()) && mzRange.contains(mzValues[i].getMZ())) {
            double d = mzValues[i + 1].getMZ() - mzValues[i].getMZ();
            if (d < minDist)
              minDist = d;
          }
        }

        processedScans++;
      }
    }
    return minDist;
  }

  private int getBinIndex(int bins, double value, double min, double binwidth) {
    int i = (int) Math.ceil((value - min) / binwidth) - 1;
    if (i < 0) // does only happen if min>than minimum value of data
      i = 0;
    if (i >= bins)
      i = bins - 1;
    return i;
  }


  private Feature createFeature(Scan[] scans, double meanMZ, double sigma) {
    try {
      Feature f = FeatureCreator.createFeature(dataFile, massListName, scans, meanMZ, sigma);
      // only add if meet filter
      if (f.getHeight() >= minimumHeight) {
        return f;
      } else {
        countTooSmall++;
      }
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error while adding feature", e);
    }
    return null;
  }

  private void addToPeakList(SimplePeakList pkl, Feature f) {
    try {
      SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
      newPeakID++;
      newRow.addPeak(dataFile, f);
      newPeakList.addRow(newRow);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error while adding feature", e);
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
