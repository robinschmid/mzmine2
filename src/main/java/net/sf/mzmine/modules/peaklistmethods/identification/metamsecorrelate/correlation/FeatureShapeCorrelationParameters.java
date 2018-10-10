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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class FeatureShapeCorrelationParameters extends SimpleParameterSet {

  // AS MAIN SETTINGS
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");


  // General parameters
  public static final MassListParameter MASS_LIST = new MassListParameter();
  // use mass lists
  public static final BooleanParameter USE_MASS_LIST_DATA = new BooleanParameter(
      "Use mass list data", "Uses the raw data stored in the given mass list", true);



  // for SUB settings
  // peak shape
  // min intensity of main peaks
  public static final DoubleParameter MAIN_PEAK_HEIGHT =
      new DoubleParameter("Main peak height", "Starts with grouping all features >= mainPeakHeight",
          MZmineCore.getConfiguration().getIntensityFormat(), 5E5);

  // min intensity of data points to be peak shape correlated
  public static final DoubleParameter NOISE_LEVEL_PEAK_SHAPE = new DoubleParameter(
      "Noise level (peak shape correlation)", "Only correlate data points >= noiseLevel.",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  // min data points to be used for correlation
  public static final IntegerParameter MIN_DP_CORR_PEAK_SHAPE =
      new IntegerParameter("Min data points",
          "Minimum of data points to be used for correlation of peak shapes.", 3, 3, 100000);

  // minimum Pearson correlation (r) for feature grouping in the same scan event of one raw file
  public static final PercentParameter MIN_R_SHAPE_INTRA = new PercentParameter(
      "Min peak shape correlation",
      "Minimum percentage for Pearson peak shape correlation for feature grouping in the same scan event of one raw file.",
      0.85, 0, 1);

  // Constructor
  public FeatureShapeCorrelationParameters() {
    this(false);
  }

  /**
   * As sub settings: No retention time tolerance and peak lists
   * 
   * @param isSub
   */
  public FeatureShapeCorrelationParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {NOISE_LEVEL_PEAK_SHAPE, MIN_DP_CORR_PEAK_SHAPE, MIN_R_SHAPE_INTRA}
        : new Parameter[] {PEAK_LISTS, RT_TOLERANCE,
            // MASS_LIST, USE_MASS_LIST_DATA, MAIN_PEAK_HEIGHT,
            NOISE_LEVEL_PEAK_SHAPE, MIN_DP_CORR_PEAK_SHAPE, MIN_R_SHAPE_INTRA});
  }

}
