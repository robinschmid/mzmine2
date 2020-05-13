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

package net.sf.mzmine.modules.rawdatamethods.rawclusteredimport;

import java.text.DecimalFormat;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class RawClusteredImportParameters extends SimpleParameterSet {

  private static final FileFilter filters[] =
      new FileFilter[] {new FileNameExtensionFilter("imzML files (imaging)", "imzML")};

  public static final FileNamesParameter fileNames =
      new FileNamesParameter("Raw files (imzML)", "imzML", filters);

  public static final DoubleParameter minHeight = new DoubleParameter("Min height",
      "Minimum height of signals that are used to calculate the cosine similarity (higher - faster)",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter noiseCutoff = new DoubleParameter("Noise cut-off",
      "Signals below this threshold are going to be removed from all scans prior to clustering (higher - faster)",
      MZmineCore.getConfiguration().getIntensityFormat(), 0d);

  public static final DoubleParameter minCosine = new DoubleParameter("Min similarity",
      "Minimum cosine similarity (default=0.9)", new DecimalFormat("0.000"), 0.9);

  public static final IntegerParameter minMatch = new IntegerParameter("Minimum matched signals",
      "Minimum matched singals in two compared spectra", 5);

  public static final IntegerParameter minSpectra = new IntegerParameter("Minimum spectra with DP",
      "Minimum number of spectra with a specific data point (if a merged spectrum contains more than 4 spectra, data points are filtered to be contained in at least X spectra)",
      2);

  public static final OptionalParameter<PercentParameter> minPercentSpectra =
      new OptionalParameter<>(new PercentParameter("Minimum spectra with DP (%)",
          "Minimum percantage of spectra with a specific data point of all merged spectra (if a merged spectrum contains more than 4 spectra, data points are filtered to be contained in at least X spectra)",
          0.10), false);

  public static final OptionalParameter<IntegerParameter> multiThreaded =
      new OptionalParameter<>(new IntegerParameter("Threads",
          "Override the number of threads. We found that more threads than CPU cores saves time on larger datasets (e.g., >=16 threads on a 8-core system)",
          16, 1, 99999999), true);


  public static final MZToleranceParameter mzTol =
      new MZToleranceParameter("m/z tolerance", "Tolerance to match and merge spectra", 0.02, 30);



  public RawClusteredImportParameters() {
    super(new Parameter[] {fileNames, multiThreaded, mzTol, noiseCutoff, minHeight, minCosine,
        minMatch, minSpectra, minPercentSpectra});
  }

}
