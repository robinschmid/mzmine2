/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package net.sf.mzmine.modules.autobatch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import com.google.common.io.Files;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.batchmode.BatchModeParameters;
import net.sf.mzmine.modules.batchmode.BatchQueue;
import net.sf.mzmine.modules.batchmode.BatchTask;
import net.sf.mzmine.modules.rawdatamethods.rawclusteredimport.RawClusteredImportParameters;
import net.sf.mzmine.modules.rawdatamethods.rawdataimport.RawDataImportParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.taskcontrol.TaskStatusListener;
import net.sf.mzmine.util.files.FileAdapter;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.files.FileEvent;
import net.sf.mzmine.util.files.FileWatcher;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class AutoBatchTask extends AbstractTask implements TaskStatusListener {
  private Logger logger = Logger.getLogger(this.getClass().getName());


  private final File batchFile;
  private final File listFile;
  private File doneFile;
  private File errorFile;
  private FileWatcher watcher;
  private FileWatcher[] watcherNewFiles;

  //
  private List<String> todo = Collections.synchronizedList(new ArrayList<>());
  private List<String> done = Collections.synchronizedList(new ArrayList<>());
  private List<String> error = Collections.synchronizedList(new ArrayList<>());
  private String currentFile;

  private BatchQueue queue;
  private MZmineProject project;

  private BatchTask batchTask;


  private boolean autoAddFiles;
  private File[] listenToFolders;

  AutoBatchTask(MZmineProject project, ParameterSet parameters) {
    this.project = project;
    this.listFile = parameters.getParameter(AutoBatchParameters.FILENAME).getValue();
    this.batchFile = parameters.getParameter(AutoBatchParameters.BATCH).getValue();
    this.autoAddFiles = parameters.getParameter(AutoBatchParameters.listenToFolder).getValue();
    this.listenToFolders = parameters.getParameter(AutoBatchParameters.listenToFolder)
        .getEmbeddedParameter().getValue();
    doneFile = FileAndPathUtil.getRealFilePath(listFile, "_done.txt");
    errorFile = FileAndPathUtil.getRealFilePath(listFile, "_error.txt");
  }

  @Override
  public double getFinishedPercentage() {
    if (todo == null)
      return 1;
    return done.size() / (double) (todo.size() + done.size());
  }

  @Override
  public String getTaskDescription() {
    if (listFile == null)
      return "";
    return "Auto batch processing of files listed in: " + listFile.getAbsolutePath()
        + (currentFile != null ? "  current file:" + currentFile + ")" : "") + " remaining:"
        + todo.size();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // read done list if available
    readDoneFilesList();
    readErrorFilesList();
    // read list
    if (!readFilesList() || !readBatchFile()) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Cannot read list of files");
      return;
    }

    // listen for changes to the file
    addFileListener();
    addFolderListener();

    // loop
    while (getStatus().equals(TaskStatus.PROCESSING)) {
      if (currentFile == null) {
        // start new
        if (todo.size() > 0) {
          currentFile = todo.get(0);
          File file = null;
          try {
            file = new File(currentFile);
            if (file.exists()) {
              // set file to raw data parameters or clustered import
              if (!setImportFile(file)) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage(
                    "Batch file does not start with raw data import or clustered raw data import");
                close();
                return;
              }
              BatchModeParameters param = new BatchModeParameters();
              param.getParameter(BatchModeParameters.batchQueue).setValue(queue);
              batchTask = new BatchTask(project, param);
              MZmineCore.getTaskController().addTask(batchTask);
              batchTask.addTaskStatusListener(this);
            } else {
              // file does not exist
              errorFile(currentFile, BatchResult.FILE_NOT_FOUND);
              todo.remove(currentFile);
              currentFile = null;
            }
          } catch (Exception e) {
          }
        }
      }

      // done?
      if (batchTask != null && batchTask.isFinished()) {
        synchronized (todo) {
          if (TaskStatus.FINISHED.equals(batchTask.getStatus())) {
            finishedFile(currentFile);
          } else {
            if (TaskStatus.CANCELED.equals(batchTask.getStatus()))
              errorFile(currentFile, BatchResult.CANCELED);
            if (TaskStatus.ERROR.equals(batchTask.getStatus()))
              errorFile(currentFile, BatchResult.ERROR);
          }
          todo.remove(currentFile);
          currentFile = null;
          // remove file
          project.clearAll();
          batchTask = null;
        }
      }

      try {
        Thread.currentThread().sleep(1000);
      } catch (Exception e) {
      }
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private void finishedFile(String f) {
    synchronized (done) {
      done.add(f);


      BufferedWriter writer = null;
      try {
        // Success
        logger.info("Write done files to " + doneFile.getAbsolutePath());

        try {
          if (!doneFile.getParentFile().exists())
            doneFile.getParentFile().mkdirs();
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Cannot create folder " + doneFile.getParent(), e);
        }
        writer = new BufferedWriter(new FileWriter(doneFile, true));
        writer.append(f + "\n");

      } catch (Throwable t) {
        logger.log(Level.SEVERE, "done export error", t);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(t.getMessage());
      } finally {
        if (writer != null)
          try {
            writer.close();
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Closing error", e);
          }
      }
    }
  }

  private void errorFile(String f, BatchResult res) {
    synchronized (error) {
      error.add(f);


      BufferedWriter writer = null;
      try {
        // Success
        logger.info("Write error files to " + errorFile.getAbsolutePath());

        try {
          if (!doneFile.getParentFile().exists())
            doneFile.getParentFile().mkdirs();
        } catch (Exception e) {
          logger.log(Level.SEVERE, "Cannot create folder " + errorFile.getParent(), e);
        }
        writer = new BufferedWriter(new FileWriter(errorFile, true));
        writer.append(f + ";" + res.toString() + "\n");

      } catch (Throwable t) {
        logger.log(Level.SEVERE, "error during export to error file", t);
        setStatus(TaskStatus.ERROR);
        setErrorMessage(t.getMessage());
      } finally {
        if (writer != null)
          try {
            writer.close();
          } catch (IOException e) {
            logger.log(Level.SEVERE, "Closing error", e);
          }
      }
    }
  }

  private void close() {
    if (watcher != null)
      watcher.stop();

    if (watcherNewFiles != null)
      for (FileWatcher w : watcherNewFiles)
        w.stop();
  }

  private boolean setImportFile(File raw) {
    boolean success = false;
    try {
      queue.get(0).getParameterSet().getParameter(RawDataImportParameters.fileNames)
          .setValue(new File[] {raw});
      success = true;
    } catch (Exception e) {
    }
    try {
      queue.get(0).getParameterSet().getParameter(RawClusteredImportParameters.fileNames)
          .setValue(new File[] {raw});
      success = true;
    } catch (Exception e) {
    }
    return success;
  }

  private boolean readBatchFile() {
    try {
      queue = BatchQueue.loadFromXml(DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .parse(batchFile).getDocumentElement());
      return true;
    } catch (SAXException | IOException | ParserConfigurationException e) {
      logger.log(Level.WARNING, "Cannot load batch file: " + batchFile.getAbsolutePath(), e);
      return false;
    }
  }

  private void addFileListener() {
    watcher = new FileWatcher(listFile.getParentFile(), 1000);
    watcher.addListener(new FileAdapter() {
      @Override
      public void onModified(FileEvent event) {
        if (event.getFile().getAbsolutePath().equals(listFile.getAbsolutePath()))
          readFilesList();
      }
    });
    watcher.watch();
  }

  /**
   * Listen to new raw data files
   */
  private void addFolderListener() {
    if (!autoAddFiles)
      return;

    watcherNewFiles = new FileWatcher[listenToFolders.length];

    for (int i = 0; i < listenToFolders.length; i++) {
      watcherNewFiles[i] = new FileWatcher(listenToFolders[i], 5000);
      watcherNewFiles[i].addListener(new FileAdapter() {
        @Override
        public void onModified(FileEvent event) {
          String path = event.getFile().getAbsolutePath();
          String s = path.toLowerCase();
          if (s.endsWith("imzml") || s.endsWith("mzml") || s.endsWith("mzxml")) {
            appendFileTodo(path);
          }
        }
      });
      watcherNewFiles[i].watch();
    }
  }

  /**
   * Append new file to list files
   * 
   * @param f
   */
  protected void appendFileTodo(String f) {
    BufferedWriter writer = null;
    try {
      // Success
      logger.info("Add new file " + f + "\nto list " + listFile.getAbsolutePath());

      try {
        if (!listFile.getParentFile().exists())
          listFile.getParentFile().mkdirs();
      } catch (Exception e) {
        logger.log(Level.SEVERE, "Cannot create folder " + listFile.getParent(), e);
      }
      writer = new BufferedWriter(new FileWriter(listFile, true));
      writer.append(f + "\n");

    } catch (Throwable t) {
      logger.log(Level.SEVERE, "done export error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    } finally {
      if (writer != null)
        try {
          writer.close();
        } catch (IOException e) {
          logger.log(Level.SEVERE, "Closing error", e);
        }
    }
  }

  protected synchronized boolean readFilesList() {
    try {
      logger.info("Reading list of files");
      List<String> lines = Files.readLines(listFile, StandardCharsets.UTF_8);

      for (String s : lines) {
        if (s.isEmpty())
          continue;

        File f = new File(s);
        if (!f.exists()) {
          logger.log(Level.WARNING, "File not found (excluding it): " + s);
          errorFile(s, BatchResult.FILE_NOT_FOUND);
        } else if (!done.contains(s) && !todo.contains(s)) {
          todo.add(s);
        }
      }
      logger.info("Auto batch mode has todo list size:" + todo.size() + "  DONE:" + done.size());
      return true;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read file (UTF8) " + listFile.getAbsolutePath(), e);
      return false;
    }
  }

  protected synchronized boolean readDoneFilesList() {
    try {
      List<String> lines = Files.readLines(doneFile, StandardCharsets.UTF_8);

      for (String s : lines) {
        if (s.isEmpty())
          continue;

        File f = new File(s);
        if (!f.exists()) {
          logger.log(Level.WARNING, "File not found (excluding it): " + s);
        } else if (!done.contains(s)) {
          done.add(s);
        }
      }
      return true;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read file (UTF8) " + doneFile.getAbsolutePath(), e);
      return false;
    }
  }

  protected synchronized boolean readErrorFilesList() {
    try {
      List<String> lines = Files.readLines(errorFile, StandardCharsets.UTF_8);

      for (String s : lines) {
        if (s.isEmpty())
          continue;

        File f = new File(s);
        if (!f.exists()) {
          logger.log(Level.WARNING, "File not found (excluding it): " + s);
          errorFile(s, BatchResult.FILE_NOT_FOUND);
        } else if (!done.contains(s)) {
          error.add(s);
        }
      }
      return true;
    } catch (IOException e) {
      logger.log(Level.WARNING, "Cannot read file (UTF8) " + errorFile.getAbsolutePath(), e);
      return false;
    }
  }

  @Override
  public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
    switch (newStatus) {
      case CANCELED:
        logger.log(Level.WARNING, "Canceled while processing file " + currentFile);
        break;
      case ERROR:
        logger.log(Level.WARNING, "Exception while processing file " + currentFile);
        break;
      case FINISHED:
        logger.log(Level.INFO, "DONE: processing file " + currentFile);
        break;
      case PROCESSING:
      case WAITING:
        return;
    }
  }
}
