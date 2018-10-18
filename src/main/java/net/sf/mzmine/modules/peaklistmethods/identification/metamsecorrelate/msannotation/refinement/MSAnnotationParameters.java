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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement;

import java.awt.Window;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary.CheckMode;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.esiadducts.ESIAdductsParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class MSAnnotationParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  // INCLUDED in sub
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between peaks");

  public static final ComboParameter<CheckMode> CHECK_MODE =
      new ComboParameter<MSAnnotationLibrary.CheckMode>("Check",
          "The modes to check for adduct identities. Average compares only the average m/z values (without min. height).\n "
              + "ALL features and SINGLE feature compares the m/z values of features with height>minHeight in raw data files",
          CheckMode.values(), CheckMode.ALL_FEATURES);

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height of feature shape (not used for average mode)",
      MZmineCore.getConfiguration().getIntensityFormat());


  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final ComboParameter<String> POSITIVE_MODE = new ComboParameter<>("MS mode",
      "Positive or negative mode?", new String[] {"POSITIVE", "NEGATIVE"}, "POSITIVE");

  public static final IntegerParameter MAX_CHARGE = new IntegerParameter("Maximum charge",
      "Maximum charge to be used for adduct search.", 2, 1, 100);
  public static final IntegerParameter MAX_MOLECULES = new IntegerParameter(
      "Maximum molecules/cluster", "Maximum molecules per cluster (f.e. [2M+Na]+).", 3, 1, 10);
  public static final IntegerParameter MAX_COMBINATION = new IntegerParameter("Maximum combination",
      "Maximum combination of adducts (set in the list) (f.e. [M+H+Na]2+ = combination of two).", 3,
      1, 10);
  public static final IntegerParameter MAX_MODS = new IntegerParameter("Maximum modification",
      "Maximum modification of adducts (set in the list)", 2, 0, 10);

  public static final ESIAdductsParameter ADDUCTS = new ESIAdductsParameter("Adducts",
      "List of adducts, each one refers a specific distance in m/z axis between related peaks");

  // MSMS part
  public static final OptionalParameter<MZToleranceParameter> MSMS_MZ_TOL =
      new OptionalParameter<MZToleranceParameter>(new MZToleranceParameter(
          "Check multimers MSMS m/z tolerance", "Checks multimers in MSMS "));


  // Constructor
  public MSAnnotationParameters() {
    this(false);
  }

  public MSAnnotationParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MZ_TOLERANCE, CHECK_MODE, MIN_HEIGHT, POSITIVE_MODE, MAX_CHARGE,
            MAX_MOLECULES, MAX_COMBINATION, MAX_MODS, ADDUCTS}
        : new Parameter[] {PEAK_LISTS, RT_TOLERANCE, MZ_TOLERANCE, CHECK_MODE, MIN_HEIGHT,
            POSITIVE_MODE, MAX_CHARGE, MAX_MOLECULES, MAX_COMBINATION, MAX_MODS, ADDUCTS});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
