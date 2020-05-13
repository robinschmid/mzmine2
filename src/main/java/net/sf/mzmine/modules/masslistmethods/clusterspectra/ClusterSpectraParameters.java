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

package net.sf.mzmine.modules.masslistmethods.clusterspectra;

import java.text.DecimalFormat;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class ClusterSpectraParameters extends SimpleParameterSet {
  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelect =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final MassListParameter massList = new MassListParameter();

  public static final DoubleParameter minHeight = new DoubleParameter("Min height",
      "Minimum height of signals that are used to calculate the cosine similarity (higher - faster)",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter minCosine = new DoubleParameter("Min similarity",
      "Minimum cosine similarity (default=0.9)", new DecimalFormat("0.000"), 0.9);

  public static final IntegerParameter minMatch = new IntegerParameter("Minimum matched signals",
      "Minimum matched singals in two compared spectra", 5);

  public static final IntegerParameter minSpectra = new IntegerParameter("Minimum spectra with DP",
      "Minimum number of spectra with a specific data point (if a merged spectrum contains more than 4 spectra, data points are filtered to be contained in at least X spectra)",
      2);

  public static final OptionalParameter<IntegerParameter> threads = new OptionalParameter<>(
      new IntegerParameter("Threads", "Override number of threads", 8), false);
  public static final OptionalParameter<PercentParameter> minPercentSpectra =
      new OptionalParameter<>(new PercentParameter("Minimum spectra with DP (%)",
          "Minimum percantage of spectra with a specific data point of all merged spectra (if a merged spectrum contains more than 4 spectra, data points are filtered to be contained in at least X spectra)",
          0.10), false);

  public static final MZToleranceParameter mzTol =
      new MZToleranceParameter("m/z tolerance", "Tolerance to match and merge spectra", 0.02, 30);

  public static final StringParameter suffix =
      new StringParameter("Suffix", "Suffix to new raw data file", "clustered");


  public ClusterSpectraParameters() {
    super(new Parameter[] {dataFiles, scanSelect, massList, mzTol, threads, minHeight, minCosine,
        minMatch, minSpectra, minPercentSpectra, suffix});
  }

}
