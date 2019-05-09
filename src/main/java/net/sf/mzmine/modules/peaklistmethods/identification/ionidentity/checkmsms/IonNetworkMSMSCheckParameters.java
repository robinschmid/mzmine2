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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.checkmsms;

import java.awt.Window;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.checkmsms.IonNetworkMSMSCheckTask.NeutralLossCheck;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * Refinement to MS annotation
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IonNetworkMSMSCheckParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final MassListParameter MASS_LIST =
      new MassListParameter("Mass lists (MS2)", "MS2 mass lists");

  // INCLUDED in sub
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      "m/z tolerance (MS2)",
      "Tolerance value of the m/z difference between MS2 signals (and the precursor, if selected)");

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height (in MS2)",
      "Minimum height of signal", MZmineCore.getConfiguration().getIntensityFormat(), 1E3);


  public static final BooleanParameter CHECK_MULTIMERS = new BooleanParameter("Check for multimers",
      "Checks the truth of the multimer identification by searching the MS/MS spectra for the connection yM -> xM (x<y)",
      true);

  public static final OptionalParameter<ComboParameter<NeutralLossCheck>> CHECK_NEUTRALLOSSES =
      new OptionalParameter<ComboParameter<NeutralLossCheck>>(new ComboParameter<NeutralLossCheck>(
          "Check neutral losses (MS1->MS2)",
          "If M-H2O was detected in MS1 this modification is searched for the precursor m/z or any signal (+precursor)",
          NeutralLossCheck.values(), NeutralLossCheck.PRECURSOR), true);

  // Constructor
  public IonNetworkMSMSCheckParameters() {
    this(false);
  }

  public IonNetworkMSMSCheckParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MASS_LIST, MZ_TOLERANCE, MIN_HEIGHT, CHECK_MULTIMERS, CHECK_NEUTRALLOSSES}
        : new Parameter[] {PEAK_LISTS, MASS_LIST, MZ_TOLERANCE, MIN_HEIGHT, CHECK_MULTIMERS,
            CHECK_NEUTRALLOSSES});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
