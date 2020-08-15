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

package net.sf.mzmine.modules.visualization.spectra.spectralmatchresults;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;

/**
 * 
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SpectralMatchCompareParameters extends SimpleParameterSet {

  public static final DBEntryField[] DEFAULT = new DBEntryField[] {DBEntryField.NAME,
      DBEntryField.COMMENT, DBEntryField.MZ, DBEntryField.COLLISION_ENERGY, DBEntryField.ION_TYPE};

  public static final MultiChoiceParameter<DBEntryField> fields =
      new MultiChoiceParameter<DBEntryField>("Fields to compare",
          "Spectral library entry fields to compare", DBEntryField.values(), DEFAULT);

  public static final BooleanParameter dataPoints = new BooleanParameter("Compare # data points",
      "Compare number of data points of spectra library entries", false);


  public SpectralMatchCompareParameters() {
    super(new Parameter[] {fields, dataPoints});
  }

}
