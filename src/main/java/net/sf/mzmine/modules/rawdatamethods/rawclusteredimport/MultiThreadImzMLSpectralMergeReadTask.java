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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.alanmrace.jimzmlparser.exceptions.ImzMLParseException;
import com.alanmrace.jimzmlparser.imzml.ImzML;
import com.alanmrace.jimzmlparser.mzml.Spectrum;
import com.alanmrace.jimzmlparser.mzml.SpectrumList;
import com.alanmrace.jimzmlparser.parser.ImzMLHandler;
import com.google.common.base.Stopwatch;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.impl.ImagingParameters;
import net.sf.mzmine.datamodel.impl.SimpleImagingScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.project.impl.ImagingRawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class MultiThreadImzMLSpectralMergeReadTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File file;
  private MZmineProject project;
  private RawDataFileWriter newMZmineFile;
  private ImagingRawDataFileImpl finalRawDataFile;
  private int totalScans = 0, parsedScans;

  private int lastScanNumber = 0;

  // all sub tasks merging scans
  private List<MultiThreadImzMLSpectralMergeReadSubTask> subTasks = new ArrayList<>();

  // define by user input
  private double minCosine = 0.90;
  private MZTolerance mzTol = new MZTolerance(0.02, 30);
  private double noiseLevel = 0;
  private double minHeight = 0;
  private int minMatch = 5;
  private double minPercentSpectra = 0.10;
  private int minSpectra = 1;

  private ParameterSet parameters;


  public MultiThreadImzMLSpectralMergeReadTask(MZmineProject project, File fileToOpen,
      RawDataFileWriter newMZmineFile, ParameterSet parameters) {
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;
    this.parameters = parameters;

    minCosine = parameters.getParameter(RawClusteredImportParameters.minCosine).getValue();
    mzTol = parameters.getParameter(RawClusteredImportParameters.mzTol).getValue();
    minHeight = parameters.getParameter(RawClusteredImportParameters.minHeight).getValue();
    noiseLevel = parameters.getParameter(RawClusteredImportParameters.noiseCutoff).getValue();
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
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }


  public void startOtherTasks(Collection<Task> tasks) {
    int threads = MZmineCore.getConfiguration().getPreferences()
        .getParameter(MZminePreferences.numOfThreads).getValue();
    // start -1
    for (int i = 0; i < threads - 1; i++) {
      MultiThreadImzMLSpectralMergeReadSubTask sub =
          new MultiThreadImzMLSpectralMergeReadSubTask(parameters, i);
      tasks.add(sub);
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

    Stopwatch watch = Stopwatch.createStarted();

    logger.info("Started parsing file " + file);
    // file = new File("C:/DATA/MALDI Sh/examples/Example_Processed.imzML");
    ImzML imzml;
    try {
      imzml = ImzMLHandler.parseimzML(file.getAbsolutePath());
    } catch (ImzMLParseException e1) {
      logger.log(Level.SEVERE, "Error while parsing imzML", e1);
      setErrorMessage("Cannot load imzML");
      setStatus(TaskStatus.ERROR);
      return;
    }
    SpectrumList spectra = imzml.getRun().getSpectrumList();
    totalScans = spectra.size();

    try {
      for (int i = 0; i < totalScans; i++) {
        if (isCanceled())
          break;

        Spectrum spectrum = spectra.get(i);

        // Ignore scans that are not MS, e.g. UV
        if (!ImzMLSpectralMergeReadTask.isMsSpectrum(spectrum)) {
          parsedScans++;
          continue;
        }

        // get data points and try to merge
        int msLevel = ImzMLSpectralMergeReadTask.extractMSLevel(spectrum);
        // add MS2 scan
        if (msLevel > 1) {
          DataPoint dataPoints[] =
              ImzMLSpectralMergeReadTask.extractDataPoints(spectrum, noiseLevel);
          ms2Scans.add(ImzMLSpectralMergeReadTask.createScan(spectrum, dataPoints));
        } else {
          // add to sub task
          subTasks.get(i % subTasks.size()).addSpectrum(spectrum);
        }

        parsedScans++;
      }

      // wait for all sub tasks to finish
      // subTasks.forEach(s -> s.finishedSpectraList());

      while (!subTasks.isEmpty()) {
        if (isCanceled())
          break;
        try {
          boolean lastIterationStarted = false;
          for (int i = 0; i < subTasks.size(); i++) {
            // empty list. distribute to other tasks
            if (subTasks.get(i).getRemainingSpectra() == 0) {
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
                // stop task
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
      totalScans = mergedScans.size() + ms2Scans.size();
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
          ((SimpleImagingScan) scan.getBestScan()).setScanNumber(i);
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

      finalRawDataFile = (ImagingRawDataFileImpl) newMZmineFile.finishWriting();
      // set settings of image
      finalRawDataFile.setImagingParam(new ImagingParameters(imzml));
      //
      project.addFile(finalRawDataFile);

      // TODO
      // add each spectrum as a signal to a peaklist with the most abundant peak in the spectrum
      // for(int s=1; s<i; s++) {
      // Scan scan = finalRawDataFile.getScan(s);
      //
      // }

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

    watch.stop();
    logger.info("TIME: " + watch.elapsed(TimeUnit.SECONDS) + "Finished parsing " + file + ", added "
        + finalRawDataFile.getScanNumbers().length + " scans from a total of " + spectra.size()
        + " raw spectra");
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public String getTaskDescription() {
    return "Opening file and merging spectra " + file;
  }

}
