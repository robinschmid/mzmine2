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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation;

import java.awt.Window;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.checkmsms.IonNetworkMSMSCheckParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.IonNetworkLibrary.CheckMode;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkRefinementParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class IonNetworkingParameters extends SimpleParameterSet {
  // different depth of settings
  public enum Setup {
    FULL, SUB, SIMPLE;
  }

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  // INCLUDED in sub
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between peaks");

  public static final ComboParameter<CheckMode> CHECK_MODE =
      new ComboParameter<IonNetworkLibrary.CheckMode>("Check",
          "The modes to check for adduct identities. Average compares only the average m/z values (without min. height).\n "
              + "ALL features and SINGLE feature compares the m/z values of features with height>minHeight in raw data files",
          CheckMode.values(), CheckMode.ALL_FEATURES);

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height of feature shape (not used for average mode)",
      MZmineCore.getConfiguration().getIntensityFormat());


  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final SubModuleParameter<IonLibraryParameterSet> LIBRARY =
      new SubModuleParameter<>("Ion identity library", "Adducts, in-source fragments and multimers",
          new IonLibraryParameterSet());

  // MS MS
  // check for truth MS/MS
  public static final OptionalModuleParameter<IonNetworkMSMSCheckParameters> MSMS_CHECK =
      new OptionalModuleParameter<IonNetworkMSMSCheckParameters>("Check MS/MS",
          "Check MS/MS for truth of multimers", new IonNetworkMSMSCheckParameters(true));

  public static final OptionalModuleParameter<IonNetworkRefinementParameters> ANNOTATION_REFINEMENTS =
      new OptionalModuleParameter<IonNetworkRefinementParameters>("Annotation refinement", "",
          new IonNetworkRefinementParameters(true), true);

  // setup
  private Setup setup;

  // Constructor
  public IonNetworkingParameters() {
    this(Setup.FULL);
  }

  public IonNetworkingParameters(Setup setup) {
    super(createParam(setup));
    this.setup = setup;
  }

  private static Parameter[] createParam(Setup setup) {
    switch (setup) {
      case FULL:
        return new Parameter[] {PEAK_LISTS, MZ_TOLERANCE, CHECK_MODE, MIN_HEIGHT, MSMS_CHECK,
            ANNOTATION_REFINEMENTS, LIBRARY};
      case SUB:
        return new Parameter[] {MZ_TOLERANCE, CHECK_MODE, MSMS_CHECK, ANNOTATION_REFINEMENTS,
            LIBRARY};
      case SIMPLE:
        return new Parameter[] {CHECK_MODE, LIBRARY};
    }
    return new Parameter[0];
  }

  /**
   * Create full set of parameters
   * 
   * @param param
   * @param rtTol
   * @return
   */
  public static IonNetworkingParameters createFullParamSet(IonNetworkingParameters param,
      double minHeight) {
    return createFullParamSet(param, null, minHeight);
  }

  /**
   * Create full set of parameters
   * 
   * @param param
   * @param rtTol
   * @param mzTol
   * @return
   */
  public static IonNetworkingParameters createFullParamSet(IonNetworkingParameters param,
      MZTolerance mzTol, double minHeight) {
    IonNetworkingParameters full = new IonNetworkingParameters();
    for (Parameter p : param.getParameters()) {
      full.getParameter(p).setValue(p.getValue());
    }
    if (mzTol != null)
      full.getParameter(IonNetworkingParameters.MZ_TOLERANCE).setValue(mzTol);

    full.getParameter(IonNetworkingParameters.MIN_HEIGHT).setValue(minHeight);
    return full;
  }

  /**
   * The setup mode
   * 
   * @return
   */
  public Setup getSetup() {
    return setup;
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }

}
