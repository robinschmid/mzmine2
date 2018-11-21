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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.filterionidnet;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class FilterIonIdNetworksParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final StringParameter suffix =
      new StringParameter("Name suffix", "Suffix to be added to peak list name", "filtered");

  // sub
  public static final IntegerParameter MIN_NETWORK_SIZE = new IntegerParameter("Min network size",
      "Minimum number of ions that point to the same neutral mass molecule", 3);


  // Constructor
  public FilterIonIdNetworksParameters() {
    this(false);
  }

  public FilterIonIdNetworksParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {MIN_NETWORK_SIZE}
        : new Parameter[] {PEAK_LISTS, MIN_NETWORK_SIZE, suffix});
  }

}
