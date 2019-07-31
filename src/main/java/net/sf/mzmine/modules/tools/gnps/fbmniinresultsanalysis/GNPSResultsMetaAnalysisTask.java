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

package net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Extract statistics from GNPS IIN and FBMN results
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GNPSResultsMetaAnalysisTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public static void main(String[] args) {
    new GNPSResultsMetaAnalysisTask(new File("D:\\Dropbox\\IIN_PAPER\\Data\\")).run();
  }

  private ParameterSet parameters;
  private File folder;
  private int total = 0;
  private int done = 0;


  /**
   * @param parameters
   * @param peakList
   */
  public GNPSResultsMetaAnalysisTask(ParameterSet parameters) {
    this.parameters = parameters;
    folder = parameters.getParameter(GNPSResultsMetaAnalysisParameters.FOLDER).getValue();
  }


  public GNPSResultsMetaAnalysisTask(File folder) {
    super();
    this.folder = folder;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return total == 0 ? 0 : done / (double) total;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Analyse GNPS results  in file " + folder;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Importing GNPS results for main folder: " + folder);

    File[] sub = FileAndPathUtil.getSubDirectories(folder);
    total = sub.length;
    if (total == 0) {
      logger.log(Level.SEVERE, "Empty folder");
      setErrorMessage("Empty folder");
      setStatus(TaskStatus.ERROR);
      return;
    }

    for (int i = 0; i < sub.length; i++) {
      try (Stream<Path> walk = Files.walk(Paths.get(sub[i].getAbsolutePath()))) {
        List<String> graphml = new ArrayList<>();
        List<String> mgf = new ArrayList<>();
        walk.map(p -> p.toString()).forEach(p -> {
          if (p.toLowerCase().endsWith("graphml"))
            graphml.add(p);
          else if (p.toLowerCase().endsWith("mgf"))
            mgf.add(p);
        });

        if (graphml.size() != 1 || mgf.size() != 1) {
          setErrorMessage(
              "number of mgf and graphml is not 1 in folder " + sub[i].getAbsolutePath());
          setStatus(TaskStatus.ERROR);
          return;
        } else {
          // run import for these files
          // target:
          File target = FileAndPathUtil.getRealFilePath(folder, sub[i].getName() + "_stats", "csv");
          if (target.exists()) {
            logger.info("Skipped: Results file already exists: " + target.getAbsolutePath());
            continue;
          }

          logger.info("From ... to ... " + graphml.get(0) + "  to  " + target.getAbsolutePath());
          GNPSResultsAnalysisTask task =
              new GNPSResultsAnalysisTask(new File(graphml.get(0)), new File(mgf.get(0)), target);
          task.run();

          // failed?
          if (task.getStatus().equals(TaskStatus.ERROR)) {
            setErrorMessage(
                "Error while importing results files from sub folder: " + sub[i].getAbsolutePath());
            setStatus(TaskStatus.ERROR);
            return;
          }
        }
      } catch (Exception e) {
        logger.log(Level.INFO, "Error while importing results file: " + folder.getAbsolutePath());
        setErrorMessage("Error while importing results file: " + folder.getAbsolutePath());
        setStatus(TaskStatus.ERROR);
        return;
      }
      done++;
    }

    logger.info("Finished import of GNPS results for " + folder);
    setStatus(TaskStatus.FINISHED);
  }
}
