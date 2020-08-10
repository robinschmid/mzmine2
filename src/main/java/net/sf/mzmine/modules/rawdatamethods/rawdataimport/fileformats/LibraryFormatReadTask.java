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

package net.sf.mzmine.modules.rawdatamethods.rawdataimport.fileformats;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.impl.SimpleLibraryScan;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.parser.AutoLibraryParser;
import net.sf.mzmine.util.spectraldb.parser.LibraryEntryProcessor;

/**
 * Json, mgf, jdx, ... library formats
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class LibraryFormatReadTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private File file;
  private MZmineProject project;
  private RawDataFileWriter newMZmineFile;
  private RawDataFile finalRawDataFile;
  private int totalScans = 0, parsedScans;

  private int lastScanNumber = 0;

  private Map<String, Integer> scanIdTable = new Hashtable<String, Integer>();

  public LibraryFormatReadTask(MZmineProject project, File fileToOpen, RawDataFileWriter newMZmineFile) {
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started parsing file " + file);

    try {

      AutoLibraryParser parser = new AutoLibraryParser(1, new LibraryEntryProcessor() {
        @Override
        public void processNextEntries(List<SpectralDBEntry> list, int alreadyProcessed) {
          SpectralDBEntry e = list.get(0);
          int scanNumber = alreadyProcessed + 1;

          SimpleLibraryScan scan = SimpleLibraryScan.create(scanNumber, e);
          try {
            newMZmineFile.addScan(scan);
          } catch (IOException e1) {
            setErrorMessage(
                "Error parsing library format: " + ExceptionUtils.exceptionToString(e1));
          }

          parsedScans++;
          totalScans++;
        }
      });
      parser.parse(this, file);

      finalRawDataFile = newMZmineFile.finishWriting();
      project.addFile(finalRawDataFile);
    } catch (Throwable e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing library format: " + ExceptionUtils.exceptionToString(e));
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    logger.info("Finished parsing " + file + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);
  }

  @Override
  public String getTaskDescription() {
    return "Opening file " + file;
  }

}
