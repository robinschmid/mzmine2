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

package net.sf.mzmine.modules.visualization.metamsecorrelate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroupList;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.MSEcorrGroupWindow;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableModule;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableWindow;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerWindow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class MSECorrGroupVisualizerModule implements MZmineRunnableModule {

    private static final String MODULE_NAME = "MSEcorr groups visualizer";
    private static final String MODULE_DESCRIPTION = "Visualize groups from metaMSEcorrelate"; // TODO

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
    	
    	final PeakList[] peakLists = parameters.getParameter(MSECorrGroupVisualizerParameters.PEAK_LISTS).getValue().getMatchingPeakLists();
        final int index = parameters.getParameter(MSECorrGroupVisualizerParameters.GROUP_I).getValue();

        // Add the window
        // only if we have some metaMSEcorr groups in peakList
        boolean weHaveData = false;
        for (PeakList pkl : peakLists) {
            if(MSEGroupedPeakList.class.isInstance(pkl)) {
            	weHaveData = true;
            	MSEGroupedPeakList list = ((MSEGroupedPeakList)pkl);
            	PKLRowGroupList groups = list.getGroups();
            	// visualize
            	MSEcorrGroupWindow wnd = new MSEcorrGroupWindow(project, list, groups, index);
            	wnd.setVisible(true);
            	//
            	break;
            }
        }

        if (!weHaveData) { 
            MZmineCore.getDesktop().displayErrorMessage(
                    MZmineCore.getDesktop().getMainWindow(), "No groups found");
        } 
        return ExitCode.OK;
    }
    
    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return MSECorrGroupVisualizerParameters.class;
    }
}