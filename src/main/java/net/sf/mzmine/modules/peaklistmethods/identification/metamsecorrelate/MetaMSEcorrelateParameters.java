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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate;

import java.awt.Window;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.esiadducts.ESIAdductsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class MetaMSEcorrelateParameters extends SimpleParameterSet {

  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
  // General parameters
  public static final MassListParameter MASS_LIST = new MassListParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between peaks");

  // GROUPING
  // sample sets
  public static final ComboParameter<Object> GROUPSPARAMETER = new ComboParameter<Object>(
      "Sample set",
      "Paremeter defining the sample set of each sample. (Set them in Project/Set sample parameters)",
      new Object[0]);

  // minimum of samples per set (with the feature detected or filled in (min height?)
  // ... showing RT<=tolerance and r>=MIN_PEARSON_R
  public static final PercentParameter MIN_SAMPLES = new PercentParameter("Min samples",
      "Minimum of samples per group (with the feature detected or filled in) matching the conditions (RT, R^2).",
      0.30, 0d, 1d);
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
      "Min r peak shape correlation (intra)",
      "Minimum percentage for Pearson peak shape correlation for feature grouping in the same scan event of one raw file.",
      0.85, 0d, 1d);

  // intensity profile
  public static final PercentParameter MIN_R_I_PROFILE = new PercentParameter(
      "Min r (height profile)",
      "Minimum percentage for Pearson intensity profile correlation in the same scan event across raw files.",
      0.70, 0d, 1d);

  // use mass lists
  public static final BooleanParameter USE_MASS_LIST_DATA = new BooleanParameter(
      "Use mass list data", "Uses the raw data stored in the given mass list", true);


  // scan event assignment
  // minimum Pearson correlation (r) for feature grouping across different scan events of one raw
  // file
  public static final PercentParameter MIN_PEARSON_R_MSE = new PercentParameter(
      "Min r peak shape correlation (inter)",
      "Minimum Pearson correlation (r) for feature grouping across different scan events of one raw file.",
      0.80, 0d, 1d);

  // deisotoping: use groups, mztolerance, min height, check boxes for isotopes, or list like
  // adducts? monotonic
  public static final DoubleParameter ISO_MIN_HEIGHT =
      new DoubleParameter("Min isotopic peak height",
          "Minimum of a peak height used to search isotopic peaks in the raw data.");
  public static final BooleanParameter ISO_MONOTONIC = new BooleanParameter(
      "Monotonic 13C isotope pattern",
      "Search for a monotonic 13C isotope pattern. Still finds other specified isotopes.", true);
  public static final BooleanParameter ISO_RAW_SEARCH = new BooleanParameter(
      "Search isotopic peaks in raw data",
      "Search for a monotonic 13C isotope pattern. Still finds other specified isotopes.", true);
  // elements
  public static final BooleanParameter ISO_CL = new BooleanParameter("Cl", "Search for Cl", false);
  public static final BooleanParameter ISO_BR = new BooleanParameter("Br", "Search for Br", false);
  public static final BooleanParameter ISO_FE = new BooleanParameter("Fe", "Search for Fe", false);
  public static final BooleanParameter ISO_S = new BooleanParameter("S", "Search for S", false);

  // #####################################################################################
  // Intensity profile correlation
  // intra group comparison

  // inter group comparison



  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final BooleanParameter SEARCH_ADDUCTS =
      new BooleanParameter("Search for adducts", "Search for adducts.", true);
  public static final BooleanParameter POSITIVE_MODE =
      new BooleanParameter("Positive MS mode", "Positive or negative mode?", false);
  public static final OptionalParameter<PercentParameter> ADDUCT_BONUSR =
      new OptionalParameter<>(new PercentParameter("Bonus for adduct",
          "Bonus correlation r that is added to the peak shape correlation before checking with the minimal r.",
          0.10, 0d, 1d));

  public static final IntegerParameter MAX_CHARGE = new IntegerParameter("Maximum charge",
      "Maximum charge to be used for adduct search.", 3, 1, 100);
  public static final IntegerParameter MAX_MOLECULES = new IntegerParameter(
      "Maximum molecules/cluster", "Maximum molecules per cluster (f.e. [2M+Na]+).", 3, 1, 10);
  public static final IntegerParameter MAX_COMBINATION = new IntegerParameter("Maximum combination",
      "Maximum combination of adducts (set in the list) (f.e. [M+H+Na]2+ = combination of two).", 3,
      1, 10);
  public static final IntegerParameter MAX_MODS = new IntegerParameter("Maximum modification",
      "Maximum modification of adducts (set in the list)", 3, 1, 10);

  public static final ESIAdductsParameter ADDUCTS = new ESIAdductsParameter("Adducts",
      "List of adducts, each one refers a specific distance in m/z axis between related peaks");


  // Constructor
  public MetaMSEcorrelateParameters() {
    super(new Parameter[] {PEAK_LISTS, MASS_LIST, USE_MASS_LIST_DATA, RT_TOLERANCE, MZ_TOLERANCE,
        GROUPSPARAMETER, MIN_SAMPLES, MAIN_PEAK_HEIGHT, NOISE_LEVEL_PEAK_SHAPE,
        MIN_DP_CORR_PEAK_SHAPE, MIN_R_SHAPE_INTRA, MIN_R_I_PROFILE, MIN_PEARSON_R_MSE,
        /* ISO_MIN_HEIGHT, ISO_MONOTONIC, ISO_RAW_SEARCH, */
        SEARCH_ADDUCTS, POSITIVE_MODE, ADDUCT_BONUSR, MAX_CHARGE, MAX_MOLECULES, MAX_COMBINATION,
        MAX_MODS, ADDUCTS});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    // Update the parameter choices
    UserParameter<?, ?> newChoices[] =
        MZmineCore.getProjectManager().getCurrentProject().getParameters();
    String[] choices;
    if (newChoices == null || newChoices.length == 0) {
      choices = new String[1];
      choices[0] = "No groups";
    } else {
      choices = new String[newChoices.length + 1];
      choices[0] = "No groups";
      for (int i = 0; i < newChoices.length; i++) {
        choices[i + 1] = newChoices[i].getName();
      }
    }
    getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).setChoices(choices);
    if (choices.length > 1)
      getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).setValue(choices[1]);

    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
