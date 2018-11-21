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
package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.formula.createavgformulas;

import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.sort.FormulaSortParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;

public class CreateAvgNetworkFormulasParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final OptionalModuleParameter<FormulaSortParameters> sorting =
      new OptionalModuleParameter<FormulaSortParameters>("Sorting",
          "Apply sorting to all resulting lists", new FormulaSortParameters(true));


  public CreateAvgNetworkFormulasParameters() {
    this(false);
  }

  public CreateAvgNetworkFormulasParameters(boolean isSub) {
    super(isSub ? //
        new Parameter[] {sorting} : new Parameter[] {PEAK_LISTS, sorting});
  }
}
