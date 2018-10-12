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

package net.sf.mzmine.modules.visualization.metamsecorrelate.rtnetwork;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class RTNetworkParameters extends SimpleParameterSet {

  /**
   * The data file.
   */
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final OptionalModuleParameter MIN_FEATURE_FILTER = new OptionalModuleParameter(
      "Minimum shared features", "Only link if both rows have a minimum number of shared feautres",
      new MinimumFeaturesFilterParameters());
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter();

  /**
   * Create the parameter set.
   */
  public RTNetworkParameters() {
    super(new Parameter[] {PEAK_LISTS, MIN_FEATURE_FILTER, RT_TOLERANCE});
  }
}
