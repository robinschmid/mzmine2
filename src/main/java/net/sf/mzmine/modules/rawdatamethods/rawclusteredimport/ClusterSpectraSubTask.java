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

package net.sf.mzmine.modules.rawdatamethods.rawclusteredimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.alanmrace.jimzmlparser.mzml.Spectrum;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleImagingScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan.Result;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.scans.ScanUtils;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class ClusterSpectraSubTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final int taskID;
  private int parsedScans = 0;

  private List<Spectrum> spectra = Collections.synchronizedList(new ArrayList<Spectrum>());
  // each finished task gives one list
  // do not compare those entries again between each other
  private List<SimpleMergedScan> sourceScans =
      Collections.synchronizedList(new ArrayList<SimpleMergedScan>());

  // define by user input
  private double minCosine = 0.90;
  private MZTolerance mzTol = new MZTolerance(0.02, 30);
  private double noiseLevel = 0;
  private double minHeight = 0;
  private int minMatch = 5;
  private double minPercentSpectra = 0.10;
  private int minSpectra = 1;
  private boolean finishedSpectraList = false;
  private boolean isDone = false;

  private List<SimpleMergedScan> mergedScans;


  public ClusterSpectraSubTask(ParameterSet parameters, int taskID) {
    this.taskID = taskID;
    minCosine = parameters.getParameter(ClusterSpectraParameters.minCosine).getValue();
    mzTol = parameters.getParameter(ClusterSpectraParameters.mzTol).getValue();
    minHeight = parameters.getParameter(ClusterSpectraParameters.minHeight).getValue();
    noiseLevel = parameters.getParameter(ClusterSpectraParameters.noiseCutoff).getValue();
    minMatch = parameters.getParameter(ClusterSpectraParameters.minMatch).getValue();
    boolean usePercent =
        parameters.getParameter(ClusterSpectraParameters.minPercentSpectra).getValue();
    minPercentSpectra = !usePercent ? 0d
        : parameters.getParameter(ClusterSpectraParameters.minPercentSpectra)
            .getEmbeddedParameter().getValue();
    minSpectra = parameters.getParameter(ClusterSpectraParameters.minSpectra).getValue();
    if (minHeight <= noiseLevel)
      minHeight = 0d;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (spectra == null)
      return 0;
    int size = getRemainingSpectra();
    return size == 0 ? 1 : (double) parsedScans / (parsedScans + size);
  }

  /**
   * Tag merged spectra with this id
   * 
   * @return
   */
  private int getTaskID() {
    return taskID;
  }


  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    mergedScans = new ArrayList<>();


    logger.info("Sub-task: merges spectra");

    // do until no more spectra are added and in the list
    while (getStatus().equals(TaskStatus.PROCESSING)
        && !(finishedSpectraList && getRemainingSpectra() == 0)) {
      try {
        while (!sourceScans.isEmpty()) {
          if (isCanceled())
            break;

          SimpleMergedScan source = sourceScans.remove(0);
          mergeWithFirst(mergedScans, source);
          parsedScans++;
        }

        while (!spectra.isEmpty()) {
          if (isCanceled())
            break;
          Spectrum spectrum = spectra.remove(0);

          // get data points and try to merge
          DataPoint dataPoints[] =
              ClusterSpectraTask.extractDataPoints(spectrum, noiseLevel);

          // try to merge MS1 scans
          // check min signals and add new scan
          if (dataPoints.length >= minMatch && !mergeWithFirst(mergedScans, spectrum, dataPoints)) {
            // was not merged
            // create scan and add new merged scan
            SimpleImagingScan rawscan = ClusterSpectraTask.createScan(spectrum, dataPoints);
            mergedScans.add(new SimpleMergedScan(rawscan, IntensityMergeMode.AVERAGE, getTaskID()));
          }
          parsedScans++;
        }

        Thread.sleep(100);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Error in spectra merging sub task - still trying", e);
      }
    }

    isDone = true;

    logger.info("Finished merging spectra sub task on " + parsedScans + " scans");
    if (getStatus().equals(TaskStatus.PROCESSING))
      setStatus(TaskStatus.FINISHED);
  }

  /**
   * Merge datapoints into first matching scan. Sort MergedScans list by number of merged scans
   * 
   * @param mergedScans
   * @param spectrum
   * @param dataPoints
   * @return
   */
  private boolean mergeWithFirst(List<SimpleMergedScan> mergedScans, Spectrum spectrum,
      DataPoint[] dataPoints) {
    DataPoint[] filtered =
        minHeight > noiseLevel ? null : ScanUtils.getFiltered(dataPoints, minHeight);
    for (int m = 0; m < mergedScans.size(); m++) {
      SimpleMergedScan scan = mergedScans.get(m);
      // try to merge
      Result res = scan.merge(dataPoints, filtered, mzTol, minHeight, minCosine, minMatch);
      if (!res.equals(Result.FALSE)) {
        logger.info("MERGED SCANS in list index " + m + "; total: " + scan.getScanCount());
        if (res.equals(Result.MERGED_REPLACE_BEST_SCAN)) {
          // replace best scan in merged with this rawscan
          scan.setBestScan(ClusterSpectraTask.createScan(spectrum, dataPoints));
          logger.info("Scan is new best in merged");
        }
        // was merged into the scan
        int mergedScanCount = scan.getScanCount();
        // insert sort list
        for (int s = 0; s < m; s++) {
          if (mergedScans.get(s).getScanCount() <= mergedScanCount) {
            mergedScans.remove(m);
            mergedScans.add(s, scan);
            return true;
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * Merge datapoints into first matching scan. Sort MergedScans list by number of merged scans
   * 
   * @param mergedScans
   * @param spectrum
   * @param dataPoints
   * @return
   */
  private boolean mergeWithFirst(List<SimpleMergedScan> mergedScans, SimpleMergedScan source) {
    for (int m = 0; m < mergedScans.size(); m++) {
      SimpleMergedScan scan = mergedScans.get(m);
      // try to merge
      Result res = scan.merge(source, mzTol, minHeight, minCosine, minMatch);
      if (!res.equals(Result.FALSE)) {
        logger.info("MERGED SCANS in list index " + m + "; total: " + scan.getScanCount());
        // was merged into the scan
        int mergedScanCount = scan.getScanCount();
        // insert sort list
        for (int s = 0; s < m; s++) {
          if (mergedScans.get(s).getScanCount() <= mergedScanCount) {
            mergedScans.remove(m);
            mergedScans.add(s, scan);
            return true;
          }
        }
        return true;
      }
    }

    // add as new
    source.addMergeTag(getTaskID());
    boolean added = false;
    for (int i = 0; i < mergedScans.size(); i++) {
      if (mergedScans.get(i).getScanCount() < source.getScanCount()) {
        mergedScans.add(i, source);
        added = true;
        break;
      }
    }
    if (!added)
      mergedScans.add(source);

    return false;
  }


  @Override
  public String getTaskDescription() {
    return "Merging scans. Done:" + parsedScans + "; TODO:" + getRemainingSpectra();
  }


  /**
   * Add to list of waiting spectra
   * 
   * @param spectrum
   */
  public void addSpectrum(Spectrum spectrum) {
    spectra.add(spectrum);
  }

  /**
   * Add merged scan to be merged with this list of merged scans
   * 
   * @param simpleMergedScan
   */
  public void addSpectrum(SimpleMergedScan simpleMergedScan) {
    sourceScans.add(simpleMergedScan);
  }

  /**
   * Spectra that need to be merged
   * 
   * @return
   */
  public int getRemainingSpectra() {
    if (spectra == null || sourceScans == null)
      return 0;
    return spectra.size() + sourceScans.size();
  }

  public void finishedSpectraList() {
    finishedSpectraList = true;;
  }

  public List<SimpleMergedScan> getMergedScans() {
    return mergedScans;
  }

  public boolean isDone() {
    return isDone;
  }

  /**
   * Other merged scans that were still waiting to be merged
   * 
   * @return
   */
  public Collection<? extends SimpleMergedScan> getRemainingScans() {
    return sourceScans;
  }

}
