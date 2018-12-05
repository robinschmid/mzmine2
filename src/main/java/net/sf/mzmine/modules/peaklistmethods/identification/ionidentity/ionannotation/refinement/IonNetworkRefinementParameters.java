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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

/**
 * Refinement to MS annotation
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IonNetworkRefinementParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();


  public static final IntegerParameter TRUE_THRESHOLD = new IntegerParameter("True link threshold",
      "links>=true threshold, then delete all other occurance in annotation networks", 4);


  public static final BooleanParameter DELETE_XMERS_ON_MSMS = new BooleanParameter(
      "Use MS/MS xmer verification to exclude",
      "If an xmer was identified by MS/MS annotation of fragment xmers, then use this identification and delete rest",
      true);


  // Constructor
  public IonNetworkRefinementParameters() {
    this(false);
  }

  public IonNetworkRefinementParameters(boolean isSub) {
    super(isSub ? // no peak list and rt tolerance
        new Parameter[] {TRUE_THRESHOLD, DELETE_XMERS_ON_MSMS}
        : new Parameter[] {PEAK_LISTS, TRUE_THRESHOLD, DELETE_XMERS_ON_MSMS});
  }

}
