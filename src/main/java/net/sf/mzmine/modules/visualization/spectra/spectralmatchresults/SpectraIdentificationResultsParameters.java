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

package net.sf.mzmine.modules.visualization.spectra.spectralmatchresults;

import java.text.DecimalFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;

/**
 * Saves the export paths of the SpectraIdentificationResultsWindow
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SpectraIdentificationResultsParameters extends SimpleParameterSet {

  public static final FileNameParameter file =
      new FileNameParameter("file", "file without extension");

  public static final BooleanParameter all =
      new BooleanParameter("Show export all", "Show button in panel", true);
  public static final BooleanParameter pdf =
      new BooleanParameter("Show export pdf", "Show button in panel", true);
  public static final BooleanParameter emf =
      new BooleanParameter("Show export emf", "Show button in panel", true);
  public static final BooleanParameter eps =
      new BooleanParameter("Show export eps", "Show button in panel", true);
  public static final BooleanParameter svg =
      new BooleanParameter("Show export svg", "Show button in panel", true);


  public static final DoubleParameter weightScore = new DoubleParameter("Weight for combined score",
      "Weight is applied to match score to calculate combined score: (WEIGHT*score + explainedIntensity)/2",
      new DecimalFormat("0.0"), 2.0, 0.00001, 1000000d);

  public static final ComboParameter<MatchSortMode> sorting = new ComboParameter<>(
      "Sort matches by", "Sort matches by score or explained library intensity",
      MatchSortMode.values(), MatchSortMode.MATCH_SCORE);

  public SpectraIdentificationResultsParameters() {
    super(new Parameter[] {sorting, weightScore, file, all, pdf, emf, eps, svg});
  }

}
