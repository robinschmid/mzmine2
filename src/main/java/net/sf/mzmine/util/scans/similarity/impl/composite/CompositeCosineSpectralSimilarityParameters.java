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

package net.sf.mzmine.util.scans.similarity.impl.composite;

import java.text.DecimalFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import net.sf.mzmine.util.scans.similarity.Weights;

/**
 * 
 */
public class CompositeCosineSpectralSimilarityParameters extends SimpleParameterSet {

  public static final ComboParameter<Weights> weight = new ComboParameter<>("Weights",
      "Weights for m/z and intensity", Weights.VALUES, Weights.MASSBANK);
  public static final DoubleParameter minCosine = new DoubleParameter("Minimum  cos similarity",
      "Minimum cosine similarity. (All signals in the masslist against the spectral library entry. "
          + "Considers only signals which were found in both the masslist and the library entry)",
      new DecimalFormat("0.000"), 0.7);

  public static final ComboParameter<HandleUnmatchedSignalOptions> handleUnmatched =
      new ComboParameter<>("Handle unmatched signals",
          "Options to handle signals that only occur in one scan. (Usually - replace intensities of missing pairs to zero for a negative weight)",
          HandleUnmatchedSignalOptions.values(),
          HandleUnmatchedSignalOptions.KEEP_ALL_AND_MATCH_TO_ZERO);


  public CompositeCosineSpectralSimilarityParameters() {
    super(new Parameter[] {weight, minCosine, handleUnmatched});
  }

}
