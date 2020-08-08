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

package net.sf.mzmine.modules.masslistmethods.mergedscanfeaturelist;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class MergedScanMSMSFeatureListBuilderParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final MassListParameter massList = new MassListParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final BooleanParameter includeSingleSpectra =
      new BooleanParameter("Include single spectra",
          "Include single MS1 spectra (otherwise only clustered spectra)", false);


  public static final StringParameter suffix = new StringParameter("Suffix",
      "This string is added to filename as suffix", "merged spectra + MS2");

  public MergedScanMSMSFeatureListBuilderParameters() {
    super(new Parameter[] {dataFiles, massList, mzTolerance, includeSingleSpectra, suffix});
  }

}
