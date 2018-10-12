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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.metamsecorrelate;

import java.awt.Window;
import java.util.HashMap;
import java.util.Map;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.util.ExitCode;

public class MSECorrGroupVisualizerParameters extends SimpleParameterSet {

    /**
     * The data file.
     */
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    public static final IntegerParameter GROUP_I = new IntegerParameter(
    		"Group index", "The group index (G(#)) defined by metaMSECorrelate.", 0);

    /**
     * Create the parameter set.
     */
    public MSECorrGroupVisualizerParameters() {
        super(new Parameter[] { PEAK_LISTS, GROUP_I});
    }
}
