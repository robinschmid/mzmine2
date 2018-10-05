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

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;

public class InterSampleIntCorrParameters extends SimpleParameterSet {


  // intensity correlation across sample max intensities
  public static final PercentParameter MIN_CORRELATION = new PercentParameter("Min correlation",
      "Minimum percentage for Pearson intensity profile correlation in the same scan event across raw files.",
      0.70, 0, 1);


  // min data points to be used for correlation
  public static final IntegerParameter MIN_DP = new IntegerParameter("Min data points",
      "Minimum of data points to be used for correlation", 3, 3, 100000);


  /**
   * 
   */
  public InterSampleIntCorrParameters() {
    super(new Parameter[] {MIN_CORRELATION, MIN_DP});
  }

}
