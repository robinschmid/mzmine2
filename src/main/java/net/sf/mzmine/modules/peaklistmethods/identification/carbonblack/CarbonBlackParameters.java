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

package net.sf.mzmine.modules.peaklistmethods.identification.carbonblack;

import java.text.DecimalFormat;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * 
 */
public class CarbonBlackParameters extends SimpleParameterSet {

  public static final PeakListsParameter peakLists = new PeakListsParameter();

  public static final MassListParameter masses = new MassListParameter();

  public static final IntegerParameter minConsecutive =
      new IntegerParameter("Minimum consecutive C signals",
          "For example C10-C12 are detected = 3 consecutive signals (isotopes are not counted)", 4,
          1, 100000);

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final ComboParameter<PolarityType> polarity = new ComboParameter<PolarityType>(
      "Polarity", "Add or remove electron mass", PolarityType.values(), PolarityType.POSITIVE);


  public static final DoubleParameter minCosine = new DoubleParameter("Minimum cos similarity",
      "Minimum cosine similarity. (Isotope pattern)", new DecimalFormat("0.000"), 0.7);
  public static final DoubleParameter minHeight = new DoubleParameter(
      "Minimum height (noise level)", "Minimum height (noise level for isotope pattern)",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public CarbonBlackParameters() {
    super(new Parameter[] {peakLists, masses, mzTolerance, minConsecutive, polarity, minHeight,
        minCosine});
  }

}
