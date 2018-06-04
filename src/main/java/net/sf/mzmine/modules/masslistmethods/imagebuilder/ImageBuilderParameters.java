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

package net.sf.mzmine.modules.masslistmethods.imagebuilder;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ImageBuilderParameters extends SimpleParameterSet {

  public static enum Weight {
    None, Linear, log10;
  }

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelection =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final MassListParameter massList = new MassListParameter();
  public static final MZRangeParameter mzRange = new MZRangeParameter(true);

  public static final DoubleParameter minimumHeight = new DoubleParameter("Min height",
      "Minimum intensity of the highest data point in the image. If image intensity is below this level, it is discarded.",
      MZmineCore.getConfiguration().getIntensityFormat());

  public static final DoubleParameter binWidth =
      new DoubleParameter("m/z bin width", "Binning of m/z values for peak picking ");


  public static final ComboParameter<Weight> weight = new ComboParameter<Weight>(
      "Intensity weighting",
      "None (the none normalised distribution, choices, defaultValue), linear (n*intensity) and log10 (n*log10(intensity)",
      Weight.values(), Weight.None);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final StringParameter suffix =
      new StringParameter("Suffix", "This string is added to filename as suffix", "image");

  public ImageBuilderParameters() {
    super(new Parameter[] {dataFiles, scanSelection, massList, mzRange, minimumHeight, binWidth,
        weight, mzTolerance, suffix});
  }

}
