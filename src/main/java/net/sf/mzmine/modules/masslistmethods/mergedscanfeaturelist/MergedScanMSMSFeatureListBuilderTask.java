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

package net.sf.mzmine.modules.masslistmethods.mergedscanfeaturelist;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MergedScan;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class MergedScanMSMSFeatureListBuilderTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private RawDataFile dataFile;

  // scan counter
  private int processedScans = 0, totalScans;
  private int newPeakID = 1;

  // User parameters
  private String suffix, massListName;

  private boolean includeSingleSpectra;
  //
  private MZTolerance mzTolerance;


  private SimplePeakList newPeakList;

  private ParameterSet parameters;



  /**
   * @param dataFile
   * @param parameters
   */
  public MergedScanMSMSFeatureListBuilderTask(MZmineProject project, RawDataFile dataFile,
      ParameterSet parameters) {

    this.parameters = parameters;
    this.project = project;
    this.dataFile = dataFile;
    this.massListName =
        parameters.getParameter(MergedScanMSMSFeatureListBuilderParameters.massList).getValue();
    mzTolerance =
        parameters.getParameter(MergedScanMSMSFeatureListBuilderParameters.mzTolerance).getValue();
    includeSingleSpectra = parameters
        .getParameter(MergedScanMSMSFeatureListBuilderParameters.includeSingleSpectra).getValue();
    this.suffix =
        parameters.getParameter(MergedScanMSMSFeatureListBuilderParameters.suffix).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Creating a feature list from merges scans (one row per merged scan) and MS/MS scans (precursor m/z as feature) for "
        + dataFile;
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
    logger.info(
        "Started building feature list of merged spectra (highest signal of each spectrum represents this spectrum) and all MS/MS scan (represented by precursor m/z)"
            + dataFile);

    // Create new peak list
    newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

    // all selected scans
    totalScans = dataFile.getScanNumbers().length;

    String comment = "";
    boolean lastWasMerged = false;
    int lastMergedSize = 0;
    int mergedCounter = 1;
    NumberFormat format = new DecimalFormat("000");

    for (int scannumber : dataFile.getScanNumbers()) {
      if (isCanceled())
        return;
      Scan scan = dataFile.getScan(scannumber);
      if (scan == null)
        continue;


      if (scan instanceof MergedScan) {
        lastWasMerged = true;
        lastMergedSize = ((MergedScan) scan).getScanCount();
        comment = "MID" + format.format(mergedCounter) + " with " + lastMergedSize
            + " scans merged: " + ((MergedScan) scan).getIntensityMode();
      } else if (lastWasMerged) {
        // add best scan - is always the first after merged scans
        comment = "MID" + format.format(mergedCounter) + " with " + lastMergedSize
            + " scans merged: best single scan (highest TIC)";
        lastWasMerged = false;
        lastMergedSize = 0;
        mergedCounter++;
      } else {
        // is a single spectrum
        if (includeSingleSpectra) {
          comment = "single spec";
        }
      }
      // add
      if (!comment.isEmpty()) {
        // create feature as highest signal
        MassList masses = scan.getMassList(massListName);
        if (masses == null) {
          setErrorMessage("Scan has no mass list named " + massListName
              + ". Run mass detection first on all scans");
          setStatus(TaskStatus.ERROR);
          return;
        }
        DataPoint dp = Arrays.stream(masses.getDataPoints()).filter(Objects::nonNull)
            .max(Comparator.comparingDouble(DataPoint::getIntensity)).orElse(null);
        if (dp != null) {
          SimpleFeature f = new SimpleFeature(dataFile, dp.getMZ(), 0d, dp.getIntensity(),
              dp.getIntensity(), new int[] {scan.getScanNumber()}, new DataPoint[] {dp},
              FeatureStatus.DETECTED, scan.getScanNumber(), -1, new int[0], Range.singleton(0d),
              Range.singleton(dp.getMZ()), Range.singleton(dp.getIntensity()));
          addToPeakList(newPeakList, f, comment);
        }
        comment = "";
      }
      processedScans++;
    }

    processedScans = 0;
    logger.info("Now adding MS/MS: " + newPeakList.getName());
    // add MSMS
    List<Double> precursor = new ArrayList<>();
    List<List<Scan>> list = new ArrayList<>();
    for (int scannumber : dataFile.getScanNumbers()) {
      if (isCanceled())
        return;
      Scan scan = dataFile.getScan(scannumber);
      if (scan == null)
        continue;

      if (scan.getMSLevel() > 1 && scan.getPrecursorMZ() > 0) {
        boolean added = false;
        double precursorMZ = scan.getPrecursorMZ();
        // can be added to multiple precursor windows
        for (int i = 0; i < precursor.size(); i++) {
          if (mzTolerance.checkWithinTolerance(precursor.get(i), precursorMZ)) {
            // add to list and calc new average
            double oldSize = list.get(i).size();
            list.get(i).add(scan);
            double newMZ = (precursor.remove(i) * oldSize + precursorMZ) / (oldSize + 1.0);
            precursor.add(i, newMZ);
            added = true;
          }
        }
        if (!added) {
          // create new
          List<Scan> l = new ArrayList<>();
          l.add(scan);
          list.add(l);
          precursor.add(precursorMZ);
        }
      }
    }
    // add all precursors as features
    totalScans = list.size();
    for (int i = 0; i < list.size(); i++) {
      if (isCanceled())
        return;

      Scan[] msmsScans = list.get(i).stream()
          .sorted(Comparator.comparingDouble(Scan::getPrecursorMZ).reversed()).toArray(Scan[]::new);
      int[] scannumbers = Arrays.stream(msmsScans).mapToInt(Scan::getScanNumber).toArray();
      Range<Double> mzRange = Range.singleton(msmsScans[0].getPrecursorMZ());
      for (int s = 1; s < msmsScans.length; s++)
        mzRange.span(Range.singleton(msmsScans[s].getPrecursorMZ()));

      double bestTIC = msmsScans[0].getTIC();
      // create feature as highest signal
      DataPoint dp = new SimpleDataPoint(precursor.get(i), bestTIC);
      DataPoint[] dataPointsPerScan = Arrays.stream(msmsScans)
          .map(s -> new SimpleDataPoint(s.getPrecursorMZ(), s.getTIC())).toArray(DataPoint[]::new);

      comment = msmsScans.length + " MS/MS scans: m/z " + dp.getMZ();
      SimpleFeature f = new SimpleFeature(dataFile, dp.getMZ(), 0d, dp.getIntensity(),
          dp.getIntensity(), scannumbers, dataPointsPerScan, FeatureStatus.DETECTED,
          msmsScans[0].getScanNumber(), msmsScans[0].getScanNumber(), scannumbers,
          Range.singleton(0d), mzRange, Range.singleton(dp.getIntensity()));
      addToPeakList(newPeakList, f, comment);

      processedScans++;
    }
    //
    logger.info("Adding peaklist to project: " + newPeakList.getName());
    // Add new peaklist to the project
    project.addPeakList(newPeakList);

    setStatus(TaskStatus.FINISHED);
    logger.info("Finished merged scan + MS/MS feature list creation on " + dataFile);
  }


  private void addToPeakList(SimplePeakList pkl, Feature f, String comment) {
    try {
      SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
      newRow.setComment(comment);
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
