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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.relations;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ionidentity.IonModificationParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class IonNetRelationsParameters extends SimpleParameterSet {
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final MZToleranceParameter MZ_TOL = new MZToleranceParameter();

  public static final IonModificationParameter ADDUCTS = new IonModificationParameter("Adducts",
      "List of adducts, each one refers a specific distance in m/z axis between related peaks");

  public static final BooleanParameter SEARCH_CONDENSED_MOL = new BooleanParameter(
      "Search condensed",
      "Searches for condensed structures (loss of water) with regards to possible structure modifications",
      true);

  public IonNetRelationsParameters() {
    super(new Parameter[] {PEAK_LISTS, MZ_TOL, SEARCH_CONDENSED_MOL, ADDUCTS});
  }

}
