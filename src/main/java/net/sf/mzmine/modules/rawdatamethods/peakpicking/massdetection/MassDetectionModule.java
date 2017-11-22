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

package net.sf.mzmine.modules.rawdatamethods.peakpicking.massdetection;

import java.util.Collection;

import javax.annotation.Nonnull;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.preferences.MZminePreferences;
import net.sf.mzmine.desktop.preferences.NumOfThreadsParameter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class MassDetectionModule implements MZmineProcessingModule {

    private static final String MODULE_NAME = "Mass detection";
    private static final String MODULE_DESCRIPTION = "This module detects individual ions in each scan and builds a mass list for each scan.";

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
                .getParameter(MassDetectionParameters.dataFiles).getValue()
                .getMatchingRawDataFiles();

        // start multiple tasks if the number of raw data files is 1
        if(dataFiles.length==1) {
        	ScanSelection sel = parameters.getParameter(
                    MassDetectionParameters.scanSelection).getValue();
        	// Obtain the settings of max concurrent threads
    	    NumOfThreadsParameter parameter = MZmineCore.getConfiguration()
    		    .getPreferences()
    		    .getParameter(MZminePreferences.numOfThreads);
    	    int maxRunningThreads;
    	    if (parameter.isAutomatic() || (parameter.getValue() == null))
    	    	maxRunningThreads = Runtime.getRuntime().availableProcessors();
    	    else maxRunningThreads = parameter.getValue();

    	    // number of scans
        	int[] scans = sel.getMatchingScanNumbers(dataFiles[0]);
        	int numPerTask = scans.length/maxRunningThreads;
    	    // start tasks
    	    for(int i=0; i<maxRunningThreads; i++) {
    	    	ParameterSet p2 = parameters.cloneParameterSet();
    	    	// use scans.length for last thread to include all remaining scans
    	    	Range<Integer> range = Range.closed(scans[numPerTask*i], 
    	    			scans[i==maxRunningThreads-1? scans.length-1 : numPerTask*(i+1)]);
    	    	
    	    	ScanSelection nsel = new ScanSelection(range, sel.getScanRTRange(), sel.getPolarity(),
    	    			sel.getSpectrumType(), sel.getMsLevel(), sel.getScanDefinition());
    	    	p2.getParameter(MassDetectionParameters.scanSelection).setValue(nsel);
    	    	
    	    	// start task
	            Task newTask = new MassDetectionTask(dataFiles[0], p2);
	            tasks.add(newTask);
    	    }
        }
        else {
	        for (RawDataFile dataFile : dataFiles) {
	            Task newTask = new MassDetectionTask(dataFile, parameters);
	            tasks.add(newTask);
	        }
        }
        return ExitCode.OK;

    }

    @Override
    public @Nonnull MZmineModuleCategory getModuleCategory() {
        return MZmineModuleCategory.PEAKPICKING;
    }

    @Override
    public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
        return MassDetectionParameters.class;
    }

}
