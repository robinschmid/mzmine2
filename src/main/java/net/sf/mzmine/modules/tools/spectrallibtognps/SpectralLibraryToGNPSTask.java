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

package net.sf.mzmine.modules.tools.spectrallibtognps;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.parser.AutoLibraryParser;
import net.sf.mzmine.util.spectraldb.parser.LibraryEntryProcessor;
import net.sf.mzmine.util.spectraldb.parser.UnsupportedFormatException;

class SpectralLibraryToGNPSTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  final String newLine = System.lineSeparator();
  private static final NumberFormat mzForm = new DecimalFormat("0.00000");

  private final File dataBaseFile;
  private Vector<SpectralDBEntry> databse = new Vector<>();
  private int totalTasks;
  private AutoLibraryParser parser;

  SpectralLibraryToGNPSTask(ParameterSet parameters) {
    dataBaseFile = parameters.getParameter(SpectralLibraryToGNPSParameters.dataBaseFile).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    int size = databse.size();
    if (size == 0)
      return 1;
    return 1.0 / size;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Convert spectral library to GNPS FBMN mgf " + dataBaseFile;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    File mgf = FileAndPathUtil.getRealFilePath(dataBaseFile.getParentFile(),
        FileAndPathUtil.eraseFormat(dataBaseFile.getName()) + "_GNPS_FBMN", "mgf");
    File csv = FileAndPathUtil.getRealFilePath(dataBaseFile.getParentFile(),
        FileAndPathUtil.eraseFormat(dataBaseFile.getName()) + "_GNPS_FBMN", "csv");
    // Open file

    try (FileWriter mgfWriter = new FileWriter(mgf); FileWriter csvWriter = new FileWriter(csv)) {
      csvWriter.write("row ID,row m/z,row retention time,Library Peak area," + newLine);

      // parse db file
      parseFile(dataBaseFile);

      int counter = 1;
      while (!parser.isDone() || !databse.isEmpty()) {
        if (isCanceled()) {
          logger.info("Added " + counter + " spectral library entries to GNPS FBMN files");
          return;
        }
        if (!databse.isEmpty()) {
          SpectralDBEntry db = databse.remove(0);
          if (db.getPrecursorMZ() != null) {
            export(db, counter, csvWriter, mgfWriter);
            counter++;
          }
        }
      }


      logger.info("Added " + (counter - 1) + " spectral library entries to GNPS FBMN files");
      setStatus(TaskStatus.FINISHED);
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + csv + " for writing.");
      return;
    }
  }

  private void export(SpectralDBEntry db, int counter, FileWriter csvWriter, FileWriter mgfWriter)
      throws IOException {
    // export csv quant table
    exportCSV(db, counter, csvWriter);

    // export mgf
    exportMGF(db, counter, mgfWriter);
  }

  private void exportCSV(SpectralDBEntry db, int counter, FileWriter writer) throws IOException {
    // id,mz,rt,area
    writer.write(counter + "," + mzForm.format(db.getPrecursorMZ()) + ",0,1," + newLine);
  }

  private void exportMGF(SpectralDBEntry db, int counter, FileWriter writer) throws IOException {

    writer.write("BEGIN IONS" + newLine);

    writer.write("FEATURE_ID=" + counter + newLine);

    String mass = mzForm.format(db.getPrecursorMZ());
    writer.write("PEPMASS=" + mass + newLine);

    writer.write("SCANS=" + counter + newLine);
    writer.write("RTINSECONDS=" + 0 + newLine);

    int msmsCharge = (int) db.getField(DBEntryField.CHARGE).orElse(1);
    writer.write("CHARGE=" + msmsCharge + newLine);

    writer.write("MSLEVEL=2" + newLine);

    DataPoint[] dataPoints = db.getDataPoints();
    for (DataPoint peak : dataPoints) {
      writer.write(mzForm.format(peak.getMZ()) + " " + peak.getIntensity() + newLine);
    }
    writer.write("END IONS" + newLine);
    writer.write(newLine);
  }

  /**
   * Load all library entries from data base file
   * 
   * @param dataBaseFile
   * @return
   */
  private void parseFile(File dataBaseFile) {
    //
    parser = new AutoLibraryParser(100, new LibraryEntryProcessor() {
      @Override
      public void processNextEntries(List<SpectralDBEntry> list, int alreadyProcessed) {
        databse.addAll(list);
      }
    });

    // return tasks
    Thread t = new Thread(() -> {
      try {
        parser.parse(this, dataBaseFile);
      } catch (UnsupportedFormatException | IOException e) {
        e.printStackTrace();
      }
    });
    t.start();
  }



}
