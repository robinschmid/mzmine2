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
import net.sf.mzmine.datamodel.Scan;
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
public class MultiThreadImzMLSpectralMergeReadSubTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final int taskID;
  private int parsedScans = 0;

  // imzML spectra to merge
  private List<Spectrum> spectra = Collections.synchronizedList(new ArrayList<Spectrum>());
  // primary scans to merge
  private List<Scan> sourceScans = Collections.synchronizedList(new ArrayList<Scan>());
  private String massListName;
  // each finished task gives one list
  // do not compare those entries again between each other
  private List<SimpleMergedScan> sourceMergedScans =
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


  public MultiThreadImzMLSpectralMergeReadSubTask(ParameterSet parameters, int taskID) {
    this.taskID = taskID;
    minCosine = parameters.getParameter(RawClusteredImportParameters.minCosine).getValue();
    mzTol = parameters.getParameter(RawClusteredImportParameters.mzTol).getValue();
    minHeight = parameters.getParameter(RawClusteredImportParameters.minHeight).getValue();
    noiseLevel = 0;
    try {
      noiseLevel = parameters.getParameter(RawClusteredImportParameters.noiseCutoff).getValue();
    } catch (Exception e) {
    }
    minMatch = parameters.getParameter(RawClusteredImportParameters.minMatch).getValue();
    boolean usePercent =
        parameters.getParameter(RawClusteredImportParameters.minPercentSpectra).getValue();
    minPercentSpectra = !usePercent ? 0d
        : parameters.getParameter(RawClusteredImportParameters.minPercentSpectra)
            .getEmbeddedParameter().getValue();
    minSpectra = parameters.getParameter(RawClusteredImportParameters.minSpectra).getValue();
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
   * For merging Scans
   * 
   * @param mass
   */
  public void setMassListName(String mass) {
    massListName = mass;
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
        while (!sourceMergedScans.isEmpty()) {
          if (isCanceled())
            break;

          SimpleMergedScan source = sourceMergedScans.get(0);
          mergeWithFirst(mergedScans, source);
          sourceMergedScans.remove(0);
          parsedScans++;
        }
        while (!sourceScans.isEmpty()) {
          if (isCanceled())
            break;

          Scan source = sourceScans.get(0);
          mergeScan(source);
          sourceScans.remove(0);
          parsedScans++;
        }

        while (!spectra.isEmpty()) {
          if (isCanceled())
            break;

          mergeImzMLSpectrum(spectra.get(0));
          spectra.remove(0);
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

  private void mergeImzMLSpectrum(Spectrum spectrum) {
    // get data points and try to merge
    DataPoint dataPoints[] = ImzMLSpectralMergeReadTask.extractDataPoints(spectrum, noiseLevel);

    // try to merge MS1 scans
    // check min signals and add new scan
    if (dataPoints.length >= minMatch && !mergeWithFirst(mergedScans, spectrum, null, dataPoints)) {
      // was not merged
      // create scan and add new merged scan
      SimpleImagingScan rawscan = ImzMLSpectralMergeReadTask.createScan(spectrum, dataPoints);
      mergedScans.add(new SimpleMergedScan(rawscan, IntensityMergeMode.AVERAGE, getTaskID()));
    }
  }

  private void mergeScan(Scan spectrum) {
    // get data points and try to merge
    DataPoint dataPoints[] = spectrum.getMassList(massListName).getDataPoints();

    // try to merge MS1 scans
    // check min signals and add new scan
    if (dataPoints.length >= minMatch && !mergeWithFirst(mergedScans, null, spectrum, dataPoints)) {
      // was not merged
      // create scan and add new merged scan
      mergedScans.add(new SimpleMergedScan(spectrum, IntensityMergeMode.AVERAGE, getTaskID()));
    }
  }

  /**
   * Merge datapoints into first matching scan. Sort MergedScans list by number of merged scans
   * 
   * @param mergedScans
   * @param spectrum only spectrum or spectrum2
   * @param spectrum2 only spectrum or spectrum2
   * @param dataPoints
   * @return
   */
  private boolean mergeWithFirst(List<SimpleMergedScan> mergedScans, Spectrum spectrum,
      Scan spectrum2, DataPoint[] dataPoints) {
    DataPoint[] filtered =
        minHeight < noiseLevel ? null : ScanUtils.getFiltered(dataPoints, minHeight);
    for (int m = 0; m < mergedScans.size(); m++) {
      if (isCanceled())
        return false;

      SimpleMergedScan scan = mergedScans.get(m);
      // try to merge
      Result res = scan.merge(dataPoints, filtered, mzTol, minHeight, minCosine, minMatch);
      if (!res.equals(Result.FALSE)) {
        if (res.equals(Result.MERGED_REPLACE_BEST_SCAN)) {
          // replace best scan in merged with this rawscan
          if (spectrum != null)
            scan.setBestScan(ImzMLSpectralMergeReadTask.createScan(spectrum, dataPoints));
          else if (spectrum2 != null)
            scan.setBestScan(spectrum2);
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
      if (isCanceled())
        return false;

      SimpleMergedScan scan = mergedScans.get(m);
      // try to merge
      Result res = scan.merge(source, mzTol, minHeight, minCosine, minMatch);
      if (!res.equals(Result.FALSE)) {
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
   * Add to list of waiting spectra (imzML import)
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
    sourceMergedScans.add(simpleMergedScan);
  }

  /**
   * Add mzmine scans to merge
   * 
   * @param scan
   */
  public void addSpectrum(Scan scan) {
    sourceScans.add(scan);
  }

  /**
   * Spectra that need to be merged
   * 
   * @return
   */
  public int getRemainingSpectra() {
    if (spectra == null || sourceMergedScans == null || sourceScans == null)
      return 0;
    return spectra.size() + sourceMergedScans.size() + sourceScans.size();
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
    return sourceMergedScans;
  }

  public void clear() {
    mergedScans = new ArrayList<>();
  }

}
