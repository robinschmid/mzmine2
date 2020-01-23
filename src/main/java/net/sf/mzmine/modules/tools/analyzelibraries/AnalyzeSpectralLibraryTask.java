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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.parser.AutoLibraryParser;
import net.sf.mzmine.util.spectraldb.parser.LibraryEntryProcessor;
import net.sf.mzmine.util.spectraldb.parser.UnsupportedFormatException;

class AnalyzeSpectralLibraryTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final File[] dataBaseFiles;
  private File output;
  private ParameterSet parameters;
  private List<AnazyleSpectralLibrarySubTask> tasks;
  private int totalTasks;
  private ConcurrentHashMap<String, AtomicInteger> adducts;

  public AnalyzeSpectralLibraryTask(ParameterSet parameters) {
    this.parameters = parameters;
    dataBaseFiles =
        parameters.getParameter(AnalyzeSpectralLibraryParameters.dataBaseFile).getValue();
    output = parameters.getParameter(AnalyzeSpectralLibraryParameters.output).getValue();
    output = FileAndPathUtil.getRealFilePath(output, "csv");
    adducts = new ConcurrentHashMap<String, AtomicInteger>();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalTasks == 0 || tasks == null)
      return 0;
    return ((double) totalTasks - tasks.size()) / totalTasks;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Analyzing spectral library files: "
        + Arrays.stream(dataBaseFiles).map(File::getName).collect(Collectors.joining(", "));
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    int count = 0;
    try {
      tasks = parseFile(dataBaseFiles);
      totalTasks = tasks.size();
      if (!tasks.isEmpty()) {
        // wait for the tasks to finish
        while (!isCanceled() && !tasks.isEmpty()) {
          for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).isFinished() || tasks.get(i).isCanceled()) {
              count += tasks.get(i).getCount();
              tasks.remove(i);
              i--;
            }
          }
          // wait for all sub tasks to finish
          try {
            Thread.sleep(100);
          } catch (Exception e) {
            cancel();
          }
        }
        // cancelled
        if (isCanceled()) {
          tasks.stream().forEach(AbstractTask::cancel);
        }
      } else {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("DB file was empty - or error while parsing "
            + Arrays.stream(dataBaseFiles).map(File::getName).collect(Collectors.joining(", ")));
        return;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE,
          "Could not read file "
              + Arrays.stream(dataBaseFiles).map(File::getName).collect(Collectors.joining(", ")),
          e);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(e.toString());
      return;
    }
    // write
    writeToFile();


    // Repaint the window to reflect the change in the feature list
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();

    setStatus(TaskStatus.FINISHED);
  }


  // ##################################################################################
  // WRITING
  private void writeToFile() {
    if (!output.getParentFile().exists())
      output.getParentFile().mkdirs();

    Path path = Paths.get(output.getAbsolutePath());
    try {
      List<String> text = new ArrayList<>();
      adducts.entrySet().stream()
          .sorted((a, b) -> Integer.compare(a.getValue().get(), b.getValue().get())).forEach(e -> {
            text.add(e.getKey() + "," + e.getValue());
          });
      Files.write(path, text);
      logger.info("Exported all to " + output.getAbsolutePath());
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Cannot export to: " + output.getAbsolutePath(), e);
      setErrorMessage("Cannot export to: " + output.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      cancel();
    }
  }

  /**
   * Load all library entries from data base file
   * 
   * @param dataBaseFile
   * @return
   */
  private List<AnazyleSpectralLibrarySubTask> parseFile(File[] dataBaseFiles)
      throws UnsupportedFormatException, IOException {
    //
    List<AnazyleSpectralLibrarySubTask> tasks = new ArrayList<>();

    // Merge multiple files
    for (File dbFile : dataBaseFiles) {
      AutoLibraryParser parser = new AutoLibraryParser(100, new LibraryEntryProcessor() {
        @Override
        public void processNextEntries(List<SpectralDBEntry> list, int alreadyProcessed) {
          // start last task
          AnazyleSpectralLibrarySubTask task =
              new AnazyleSpectralLibrarySubTask(dbFile, alreadyProcessed + 1, list) {

                @Override
                public void anaylzeLibraryEntry(SpectralDBEntry e) {
                  String adductName = getAdductString(e);
                  // already inserted?
                  AtomicInteger counter = adducts.get(adductName);
                  if (counter == null) {
                    adducts.put(adductName, new AtomicInteger(1));
                  } else {
                    counter.getAndIncrement();
                  }
                }

                private String getAdductString(SpectralDBEntry e) {
                  Optional<Object> optional = e.getField(DBEntryField.ION_TYPE);
                  String adduct = optional.map(Object::toString).orElse("");
                  // no spaces
                  adduct = adduct.replaceAll("\\s+", "");

                  if (adduct.isEmpty())
                    return "NONE";

                  return adduct;
                }
              };
          MZmineCore.getTaskController().addTask(task);
          tasks.add(task);
        }
      });

      // return tasks
      parser.parse(this, dbFile);
    }
    return tasks;
  }

}
