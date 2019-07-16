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

package net.sf.mzmine.modules.rawdatamethods.timstofimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportModule;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportParameters;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.files.FileNameExtFilter;

public class BrukerSingleSpecImportTask extends AbstractTask {

  private double perc = 0;
  private File file;
  private File msConvert;

  /**
   * ExtractAllScansParameters or ExtractScansParameters
   * 
   * @param file
   */
  public BrukerSingleSpecImportTask(File file) {
    this.file = file;
    this.msConvert = MZmineCore.getConfiguration().getPreferences()
        .getParameter(MZminePreferences.msconvertPath).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return perc;
  }



  @Override
  public void run() {
    if (msConvert == null || !msConvert.exists()) {
      setErrorMessage(
          "Please set the directory to msconvert.exe in the preferences (download msconvert as part of Proteowizzard");
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.PROCESSING);

    List<File> result = new ArrayList<>();


    List<File[]> list =
        FileAndPathUtil.findFilesInDir(file, new FileNameExtFilter("", ".d", true), true);
    System.out.println("Searching for .d in folder " + file.getAbsolutePath());

    int total = 0;
    for (File[] ff : list)
      for (File f : ff)
        total++;

    int i = 0;
    for (File[] ff : list) {
      for (File f : ff) {
        System.out.println(ff.length + "  File " + f.getAbsolutePath());
        try {
          // output file
          File out = f.getParentFile().getParentFile().getParentFile();
          // rename file
          File mzml = FileAndPathUtil.getRealFilePath(out, f.getName(), "mzML");
          File renamed =
              new File(out, out.getName() + "_" + f.getParentFile().getParentFile().getName() + "_"
                  + FileAndPathUtil.eraseFormat(mzml.getName() + ".mzML"));

          if (renamed.exists()) {
            result.add(renamed);
          } else {
            // convert and import
            Process process = new ProcessBuilder(msConvert.getAbsolutePath(), f.getAbsolutePath())
                .directory(out).start();
            process.waitFor();
            // Process process = Runtime.getRuntime().exec("msconvert.exe",
            // new String[] {"msconvert", "--help"}, msConvert.getParentFile());
            String line;
            BufferedReader input =
                new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null) {
              System.out.println(line);
            }
            input.close();
            BufferedReader err =
                new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = err.readLine()) != null) {
              System.out.println(line);
            }
            input.close();

            if (mzml.exists()) {
              mzml.renameTo(renamed);

              if (renamed.exists()) {
                result.add(renamed);
              }
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        i++;
        perc = i / (double) total;
      }
    }

    if (!result.isEmpty()) {
      // import
      RawDataImportParameters param = (RawDataImportParameters) MZmineCore.getConfiguration()
          .getModuleParameters(RawDataImportModule.class);
      param.getParameter(RawDataImportParameters.fileNames).setValue(result.toArray(new File[0]));
      ArrayList<Task> tasks = new ArrayList<Task>();
      MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();
      RawDataImportModule mod = new RawDataImportModule();
      mod.runModule(project, param.cloneParameterSet(), tasks);
      MZmineCore.getTaskController().addTasks(tasks.toArray(new Task[0]));
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  @Override
  public String getTaskDescription() {
    return "Extracting scans to CSV file(s)";
  }
}
