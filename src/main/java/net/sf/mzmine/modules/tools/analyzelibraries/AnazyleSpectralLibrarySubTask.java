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

package net.sf.mzmine.modules.tools.analyzelibraries;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

public abstract class AnazyleSpectralLibrarySubTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String METHOD = "Spectral DB search";
  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private final File dataBaseFile;

  private int finishedRows = 0;
  private final int totalRows;
  private List<SpectralDBEntry> list;

  private int count = 0;

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;
  private int listsize;

  public AnazyleSpectralLibrarySubTask(File dataBaseFile, int startEntry,
      List<SpectralDBEntry> list) {
    this.startEntry = startEntry;
    this.list = list;
    listsize = list.size();
    this.dataBaseFile = dataBaseFile;
    totalRows = list.size();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return ((double) finishedRows) / totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return MessageFormat.format(
        "(entry {1}-{2}) spectral database identification in using database {0}",
        dataBaseFile.getName(), startEntry, startEntry + listsize - 1);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (isCanceled()) {
      logger.info("Added " + count + " spectral library matches (before being cancelled)");
      return;
    }

    for (SpectralDBEntry ident : list) {
      anaylzeLibraryEntry(ident);

      // next row
      finishedRows++;
    }
    // check for max error (missing masslist)
    if (errorCounter > MAX_ERROR) {
      logger.log(Level.WARNING, "Data base matching failed. To many missing mass lists ");
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Data base matching failed. To many missing mass lists ");
      list = null;
      return;
    }

    if (count > 0)
      logger.info("Added " + count + " spectral library matches");

    list = null;
    setStatus(TaskStatus.FINISHED);
  }

  public int getCount() {
    return count;
  }

  public abstract void anaylzeLibraryEntry(SpectralDBEntry e);

}
