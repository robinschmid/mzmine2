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

package net.sf.mzmine.taskcontrol;

import java.util.List;
import java.util.function.Consumer;

/**
 * Listens for end of all tasks
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class AllTasksFinishedListener implements TaskStatusListener {

  private List<AbstractTask> tasks;
  private Consumer<List<AbstractTask>> operation;
  private Consumer<List<AbstractTask>> operationOnError;
  private boolean stopOnError = false;
  // mark when done
  private boolean done = false;

  public AllTasksFinishedListener(List<AbstractTask> tasks,
      Consumer<List<AbstractTask>> operation) {
    this(tasks, false, operation);
  }

  public AllTasksFinishedListener(List<AbstractTask> tasks, boolean stopOnError,
      Consumer<List<AbstractTask>> operation) {
    this(tasks, stopOnError, operation, null);
  }

  /**
   * 
   * @param tasks
   * @param stopOnError
   * @param operation gets fired on completion of all tasks
   * @param operationOnError gets fired on error (only once)
   */
  public AllTasksFinishedListener(List<AbstractTask> tasks, boolean stopOnError,
      Consumer<List<AbstractTask>> operation, Consumer<List<AbstractTask>> operationOnError) {
    this.tasks = tasks;
    this.stopOnError = stopOnError;
    this.operation = operation;
    this.operationOnError = operationOnError;
    tasks.stream().forEach(t -> t.addTaskStatusListener(this));
  }

  @Override
  public void taskStatusChanged(Task task, TaskStatus newStatus, TaskStatus oldStatus) {
    if (done)
      return;
    // stop on error
    if (stopOnError
        && tasks.stream().map(Task::getStatus).anyMatch(s -> s.equals(TaskStatus.ERROR))) {
      if (operationOnError != null)
        operationOnError.accept(tasks);
      done = true;
      return;
    }
    // is one still running?
    boolean stillRunning = tasks.stream().map(Task::getStatus)
        .anyMatch(s -> !(s.equals(TaskStatus.FINISHED) || s.equals(TaskStatus.ERROR)));
    if (!stillRunning) {
      operation.accept(tasks);
      done = true;
      return;
    }
  }
}
