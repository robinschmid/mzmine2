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
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * Refinement to MS annotation
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSAnnMSMSCheckParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final MassListParameter MASS_LIST = new MassListParameter();

  // INCLUDED in sub
  // MZ-tolerance: deisotoping, adducts
  public static final MZToleranceParameter MZ_TOLERANCE = new MZToleranceParameter(
      "m/z tolerance (MS/MS)", "Tolerance value of the m/z difference between peaks");

  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Minimum height of signal", MZmineCore.getConfiguration().getIntensityFormat());


  public static final BooleanParameter CHECK_MULTIMERS = new BooleanParameter("Check for multimers",
      "Checks the truth of the multimer identification by searching the MS/MS spectra for the connection yM -> xM (x<y)");

  // Constructor
  public MSAnnMSMSCheckParameters() {
    this(false);
  }

  public MSAnnMSMSCheckParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MASS_LIST, MZ_TOLERANCE, MIN_HEIGHT, CHECK_MULTIMERS}
        : new Parameter[] {PEAK_LISTS, MASS_LIST, MZ_TOLERANCE, MIN_HEIGHT, CHECK_MULTIMERS});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}