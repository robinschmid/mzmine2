/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.masslistmethods.exportaslibrary;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MergedScan;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats.GnpsJsonGenerator;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Exports all spectra to a library file
 *
 */
public class ExportScansAsLibraryTask extends AbstractTask {

  // Logger
  private static final Logger LOG = Logger.getLogger(ExportScansAsLibraryTask.class.getName());

  private final File exportFile;

  private int progress;
  private int progressMax;

  private String massListName;

  private Boolean bestScan;

  private Boolean maxScan;

  private Boolean meanScan;

  private Boolean sumScan;

  private Integer minSignals;

  private String description;

  private ScanSelection scanSelection;

  private RawDataFile[] raws;

  private Boolean addFileNameDescription;

  public ExportScansAsLibraryTask(ParameterSet parameters) {
    progress = 0;
    progressMax = 0;
    massListName = parameters.getParameter(ExportScansAsLibraryParameters.massList).getValue();

    bestScan = parameters.getParameter(ExportScansAsLibraryParameters.bestScan).getValue();
    maxScan = parameters.getParameter(ExportScansAsLibraryParameters.maxScan).getValue();
    meanScan = parameters.getParameter(ExportScansAsLibraryParameters.meanScan).getValue();
    sumScan = parameters.getParameter(ExportScansAsLibraryParameters.sumScan).getValue();

    addFileNameDescription =
        parameters.getParameter(ExportScansAsLibraryParameters.addFileNameDescription).getValue();

    minSignals = parameters.getParameter(ExportScansAsLibraryParameters.minSignals).getValue();
    description = parameters.getParameter(ExportScansAsLibraryParameters.description).getValue();

    raws = parameters.getParameter(ExportScansAsLibraryParameters.dataFiles).getValue()
        .getMatchingRawDataFiles();
    scanSelection = parameters.getParameter(ExportScansAsLibraryParameters.scanSelect).getValue();

    this.exportFile = FileAndPathUtil.getRealFilePath(
        parameters.getParameter(ExportScansAsLibraryParameters.file).getValue(), "json");
  }

  @Override
  public String getTaskDescription() {
    return "Exporting all spectra to library";
  }

  @Override
  public double getFinishedPercentage() {
    return progressMax == 0 ? 0.0 : (double) progress / (double) progressMax;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    BufferedWriter writer = null;
    try {
      // Success
      LOG.info("Export of spectra finished");

      try {
        if (!exportFile.getParentFile().exists())
          exportFile.getParentFile().mkdirs();
      } catch (Exception e) {
        LOG.log(Level.SEVERE, "Cannot create folder " + exportFile.getParent(), e);
      }
      writer = new BufferedWriter(new FileWriter(exportFile, true));

      for (RawDataFile raw : raws) {
        Scan[] scans = scanSelection.getMatchingScans(raw);
        for (Scan scan : scans) {
          MassList masses = scan.getMassList(massListName);
          if (masses == null) {
            LOG.log(Level.SEVERE, "Scan has no mass list with the name: " + massListName);
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Scan has no mass list with the name: " + massListName);
            return;
          }

          // check scan for export
          if (checkExportFilters(scan, masses)) {
            // export
            exportScan(scan, masses, raw, writer);
          }
        }
      }
      setStatus(TaskStatus.FINISHED);
    } catch (Throwable t) {
      LOG.log(Level.SEVERE, "Spectrum export error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    } finally {
      if (writer != null)
        try {
          writer.close();
        } catch (IOException e) {
          LOG.log(Level.SEVERE, "Closing error", e);
        }
    }
  }

  private void exportScan(Scan scan, MassList masses, RawDataFile raw, BufferedWriter writer)
      throws IOException {
    String comment = "";
    if (addFileNameDescription)
      comment = raw.getName();
    if (description.length() > 0)
      comment = comment.isEmpty() ? description : comment + " " + description;
    String json = GnpsJsonGenerator.generateJSON(scan, comment, masses.getDataPoints());
    // export json
    writer.append(json + "\n");
  }


  private boolean checkExportFilters(Scan scan, MassList masses) {
    // min signals
    if (minSignals < masses.getDataPoints().length)
      return false;

    // export spectrum to file
    if (scan instanceof MergedScan) {
      IntensityMergeMode mode = ((MergedScan) scan).getIntensityMode();
      if (mode.equals(IntensityMergeMode.AVERAGE) && !meanScan)
        return false;
      if (mode.equals(IntensityMergeMode.MAXIMUM) && !maxScan)
        return false;
      if (mode.equals(IntensityMergeMode.SUM) && !sumScan)
        return false;
    } else {
      if (!bestScan)
        return false;
    }
    return true;
  }

}
