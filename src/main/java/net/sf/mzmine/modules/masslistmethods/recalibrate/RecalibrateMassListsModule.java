/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.masslistmethods.recalibrate;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class RecalibrateMassListsModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Recalibrate mass lists";
    private static final String MODULE_DESCRIPTION = "This module uses lock masses for mass list recalibration";

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
    public ExitCode runModule(@Nonnull MZmineProject project,
            @Nonnull ParameterSet parameters, @Nonnull Collection<Task> tasks) {

        RawDataFile[] dataFiles = parameters
                .getParameter(RecalibrateMassListsParameters.dataFiles)
                .getValue().getMatchingRawDataFiles();

        for (int i = 0; i < dataFiles.length; i++) {
            Task newTask = new RecalibrateMassListsTask(project, dataFiles[i],
                    parameters.cloneParameterSet());
            tasks.add(newTask);
        }

        return ExitCode.OK;
    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKPICKING;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return RecalibrateMassListsParameters.class;
    }

}
