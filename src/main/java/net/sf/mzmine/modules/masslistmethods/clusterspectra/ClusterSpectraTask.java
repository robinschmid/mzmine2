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

package net.sf.mzmine.modules.masslistmethods.clusterspectra;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.google.common.base.Stopwatch;
import net.sf.mzmine.datamodel.ImagingScan;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleImagingScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan;
import net.sf.mzmine.datamodel.impl.SimpleScan;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.rawclusteredimport.MultiThreadImzMLSpectralMergeReadSubTask;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.project.impl.ImagingRawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;

public class ClusterSpectraTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File file;
  private MZmineProject project;
  private int totalScans = 0, parsedScans;
  private int lastScanNumber = 0;

  // all sub tasks merging scans
  private List<MultiThreadImzMLSpectralMergeReadSubTask> subTasks = new ArrayList<>();

  // define by user input
  private double minCosine = 0.90;
  private MZTolerance mzTol = new MZTolerance(0.02, 30);
  private double minHeight = 0;
  private int minMatch = 5;
  private double minPercentSpectra = 0.10;
  private int minSpectra = 1;

  private ParameterSet parameters;

  private int threads;


  public ClusterSpectraTask(Collection<Task> tasks, MZmineProject project,
      ParameterSet parameters) {
    this.project = project;
    this.parameters = parameters;

    threads = MZmineCore.getConfiguration().getPreferences()
        .getParameter(MZminePreferences.numOfThreads).getValue();
    if (parameters.getParameter(ClusterSpectraParameters.threads).getValue())
      threads = parameters.getParameter(ClusterSpectraParameters.threads).getEmbeddedParameter()
          .getValue();
    minCosine = parameters.getParameter(ClusterSpectraParameters.minCosine).getValue();
    mzTol = parameters.getParameter(ClusterSpectraParameters.mzTol).getValue();
    minHeight = parameters.getParameter(ClusterSpectraParameters.minHeight).getValue();
    minMatch = parameters.getParameter(ClusterSpectraParameters.minMatch).getValue();
    boolean usePercent =
        parameters.getParameter(ClusterSpectraParameters.minPercentSpectra).getValue();
    minPercentSpectra = !usePercent ? 0d
        : parameters.getParameter(ClusterSpectraParameters.minPercentSpectra).getEmbeddedParameter()
            .getValue();
    minSpectra = parameters.getParameter(ClusterSpectraParameters.minSpectra).getValue();

    tasks.add(this);
    startOtherTasks(tasks);
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }


  public void startOtherTasks(Collection<Task> tasks) {
    // start -1
    for (int i = 0; i < threads - 1; i++) {
      MultiThreadImzMLSpectralMergeReadSubTask sub =
          new MultiThreadImzMLSpectralMergeReadSubTask(parameters, i);
      MZmineCore.getTaskController().addTask(sub, TaskPriority.HIGH);
      // tasks.add(sub);
      subTasks.add(sub);
    }
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    //
    List<SimpleMergedScan> mergedScans = new ArrayList<SimpleMergedScan>();
    List<SimpleImagingScan> ms2Scans = new ArrayList<SimpleImagingScan>();

    RawDataFile[] raws = parameters.getParameter(ClusterSpectraParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    ScanSelection scanSelect =
        parameters.getParameter(ClusterSpectraParameters.scanSelect).getValue();
    String masses = parameters.getParameter(ClusterSpectraParameters.massList).getValue();
    String suffix = parameters.getParameter(ClusterSpectraParameters.suffix).getValue();

    subTasks.forEach(s -> s.setMassListName(masses));

    RawDataFileWriter newMZmineFile;
    String newName = raws[0].getName() + suffix;
    try {
      if (raws[0] instanceof ImagingRawDataFileImpl)
        newMZmineFile = new ImagingRawDataFileImpl(newName);
      else
        newMZmineFile = MZmineCore.createNewFile(newName);
    } catch (IOException e1) {
      setErrorMessage("Cannot create raw data file");
      setStatus(TaskStatus.ERROR);
      ((AbstractTask) subTasks.stream()).cancel();
      return;
    }

    Stopwatch watch = Stopwatch.createStarted();

    totalScans = 0;
    for (RawDataFile r : raws) {
      Scan[] scans = scanSelect.getMatchingScans(r);
      totalScans += scans.length;
      for (int i = 0; i < scans.length; i++) {
        // distribute scans
        if (scans[i] instanceof ImagingScan) {
          SimpleImagingScan ss = new SimpleImagingScan(scans[i]);
          ss.setDataPoints(scans[i].getMassList(masses).getDataPoints());
          SimpleMergedScan ms = new SimpleMergedScan(ss, IntensityMergeMode.AVERAGE);
          subTasks.get(i % subTasks.size()).addSpectrum(ms);
        } else {
          SimpleScan ss = new SimpleScan(scans[i]);
          ss.setDataPoints(scans[i].getMassList(masses).getDataPoints());
          SimpleMergedScan ms = new SimpleMergedScan(ss, IntensityMergeMode.AVERAGE);
          subTasks.get(i % subTasks.size()).addSpectrum(ms);
        }
        parsedScans++;
      }
    }
    parsedScans = 0;

    logger.info("Starting to cluster " + totalScans + " spectra from "
        + Arrays.stream(raws).map(RawDataFile::getName).collect(Collectors.joining(", ")));
    try {
      // wait for all sub tasks to finish
      // subTasks.forEach(s -> s.finishedSpectraList());

      while (!subTasks.isEmpty()) {
        if (isCanceled()) {
          ((AbstractTask) subTasks).cancel();
          break;
        }
        try {
          //
          boolean lastIterationStarted = false;
          for (int i = 0; i < subTasks.size(); i++) {
            // empty list. distribute to other tasks
            if (subTasks.get(i).getRemainingSpectra() == 0) {
              // stop task
              List<SimpleMergedScan> source = subTasks.get(i).getMergedScans();
              if (subTasks.size() == 1 && !lastIterationStarted) {
                logger.log(Level.INFO,
                    "Last clusterin iteration. Removing all unclustered spectra and rechecking all clustered spectra (n>=2)");
                lastIterationStarted = true;
                // filter by min 2 spectra
                for (SimpleMergedScan ms : source) {
                  if (ms.getScanCount() > 1) {
                    ms.resetMergeTags();
                    subTasks.get(i).addSpectrum(ms);
                  }
                }
                break;
              } else {
                subTasks.get(i).finishedSpectraList();
                subTasks.remove(i);
                i--;
                if (subTasks.isEmpty()) {
                  logger.log(Level.INFO,
                      "Last sub task finished. Getting list of finished merged scans");
                  mergedScans = source;
                } else {
                  logger.log(Level.INFO,
                      "One task is done. Distributing scans to other tasks: " + (subTasks.size()));
                  for (int s = 0; s < source.size(); s++) {
                    subTasks.get(s % subTasks.size()).addSpectrum(source.get(s));
                  }
                  break;
                }
              }
            }
          }
          Thread.sleep(100);
        } catch (Exception e) {
          logger.log(Level.WARNING, "Error while waiting for all sub tasks", e);
        }
      }

      // maybe stopped and now just collect all scans
      if (mergedScans.isEmpty()) {
        for (MultiThreadImzMLSpectralMergeReadSubTask t : subTasks) {
          mergedScans.addAll(t.getMergedScans());
          mergedScans.addAll(t.getRemainingScans());
        }
      }

      logger.log(Level.INFO, "add all scans to raw file");
      parsedScans = 0;
      int i = 1;
      for (SimpleMergedScan scan : mergedScans) {
        // add merged
        if (scan.getScanCount() > 1) {
          // clean up
          scan.clean(minPercentSpectra, minSpectra);

          // add average
          scan.setScanNumber(i);
          newMZmineFile.addScan(scan);
          i++;
          // add maximum merged scan
          SimpleMergedScan maxScan = new SimpleMergedScan(scan, IntensityMergeMode.MAXIMUM);
          maxScan.setScanNumber(i);
          newMZmineFile.addScan(maxScan);
          i++;

          // sum
          SimpleMergedScan sumScan = new SimpleMergedScan(scan, IntensityMergeMode.SUM);
          sumScan.setScanNumber(i);
          newMZmineFile.addScan(sumScan);
          i++;

          // add best scan
          if (scan instanceof SimpleImagingScan)
            ((SimpleImagingScan) scan.getBestScan()).setScanNumber(i);
          else
            ((SimpleScan) scan.getBestScan()).setScanNumber(i);

          newMZmineFile.addScan(scan.getBestScan());
          i++;
        }
        // add best
        else {
          ((SimpleImagingScan) scan.getBestScan()).setScanNumber(i + 1);
          newMZmineFile.addScan(scan.getBestScan());
          i++;
        }
        parsedScans++;
      }
      for (SimpleImagingScan scan : ms2Scans) {
        // add ms2 at the end
        scan.setScanNumber(i);
        newMZmineFile.addScan(scan);
        i++;
        parsedScans++;
      }

      RawDataFile finalRawDataFile = newMZmineFile.finishWriting();
      // set settings of image
      if (finalRawDataFile instanceof ImagingRawDataFileImpl)
        ((ImagingRawDataFileImpl) finalRawDataFile)
            .setImagingParam(((ImagingRawDataFileImpl) newMZmineFile).getImagingParam());
      //
      watch.stop();
      logger.info("TIME: " + watch.elapsed(TimeUnit.SECONDS) + "; Finished parsing " + file
          + ", added " + finalRawDataFile.getScanNumbers().length + " scans from a total of "
          + totalScans + " raw spectra");
      project.addFile(finalRawDataFile);
    } catch (Throwable e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      e.printStackTrace();
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public String getTaskDescription() {
    return "Clustering spectra in " + file;
  }

}
