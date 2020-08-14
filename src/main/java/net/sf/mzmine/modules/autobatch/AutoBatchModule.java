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

package net.sf.mzmine.modules.autobatch;

import java.util.Collection;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskPriority;
import net.sf.mzmine.util.ExitCode;

/**
 * Batch mode module
 */
public class AutoBatchModule implements MZmineProcessingModule {

  private static Logger logger = Logger.getLogger(AutoBatchModule.class.getName());

  private static final String MODULE_NAME = "Auto batch mode";
  private static final String MODULE_DESCRIPTION =
      "This module allows execution of multiple processing tasks in a batch. The batch is applied to all files listed in a text file";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    AutoBatchTask newTask = new AutoBatchTask(project, parameters);

    /*
     * We do not add the task to the tasks collection, but instead directly submit to the task
     * controller, because we need to set the priority to HIGH. If the priority is not HIGH and the
     * maximum number of concurrent tasks is set to 1 in the MZmine preferences, then this BatchTask
     * would block all other tasks. See getTaskPriority in BatchTask
     */
    MZmineCore.getTaskController().addTask(newTask, TaskPriority.HIGH);
    // tasks.add(newTask);

    return ExitCode.OK;
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.PROJECT;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return AutoBatchParameters.class;
  }

}
