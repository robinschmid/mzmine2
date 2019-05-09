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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.addionannotations;

import java.awt.Window;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkMSMSCheckParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkRefinementParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class AddIonNetworkingParameters extends SimpleParameterSet {
  // different depth of settings
  public enum Setup {
    FULL, SUB, SIMPLE;
  }

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter("m/z tolerance",
      "Tolerance value of the m/z difference between peaks");

  public static final BooleanParameter LIMIT_BY_GROUPS =
      new BooleanParameter("Limit by feature groups",
          "Only annotate features which where correlated or grouped otherwise.", true);

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height of feature shape (not used for average mode)",
      MZmineCore.getConfiguration().getIntensityFormat());

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
  public AddIonNetworkingParameters() {
    this(Setup.FULL);
  }

  public AddIonNetworkingParameters(Setup setup) {
    super(createParam(setup));
    this.setup = setup;
  }

  private static Parameter[] createParam(Setup setup) {
    switch (setup) {
      case FULL:
        return new Parameter[] {PEAK_LISTS, RT_TOLERANCE, MZ_TOLERANCE, LIMIT_BY_GROUPS, MIN_HEIGHT,
            MSMS_CHECK, ANNOTATION_REFINEMENTS, LIBRARY};
      case SUB:
        return new Parameter[] {MZ_TOLERANCE, LIMIT_BY_GROUPS, MSMS_CHECK, ANNOTATION_REFINEMENTS};
      case SIMPLE:
        return new Parameter[] {LIMIT_BY_GROUPS, LIBRARY};
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
  public static AddIonNetworkingParameters createFullParamSet(AddIonNetworkingParameters param,
      RTTolerance rtTol, double minHeight) {
    return createFullParamSet(param, rtTol, null, minHeight);
  }

  /**
   * Create full set of parameters
   * 
   * @param param
   * @param rtTol
   * @param mzTol
   * @return
   */
  public static AddIonNetworkingParameters createFullParamSet(AddIonNetworkingParameters param,
      RTTolerance rtTol, MZTolerance mzTol, double minHeight) {
    AddIonNetworkingParameters full = new AddIonNetworkingParameters();
    for (Parameter p : param.getParameters()) {
      full.getParameter(p).setValue(p.getValue());
    }
    full.getParameter(AddIonNetworkingParameters.RT_TOLERANCE).setValue(rtTol);
    if (mzTol != null)
      full.getParameter(AddIonNetworkingParameters.MZ_TOLERANCE).setValue(mzTol);

    full.getParameter(AddIonNetworkingParameters.MIN_HEIGHT).setValue(minHeight);
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
