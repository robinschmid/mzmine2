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

package net.sf.mzmine.modules.rawdatamethods.exportscans;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.DirectoryParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;

public class ExtractAllScansParameters extends SimpleParameterSet {

  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();
  public static final BooleanParameter separateFolders = new BooleanParameter("In separate folders",
      "Either separate folders for each raw data file or in one folder with the raw data file name as a prefix",
      false);

  public static final DirectoryParameter file =
      new DirectoryParameter("Output directory", "Directory to write scans to");
  public static final BooleanParameter exportHeader =
      new BooleanParameter("Export header", "Exports a header for each scan file", false);

  public static final OptionalParameter<MassListParameter> useMassList =
      new OptionalParameter<>(new MassListParameter());

  public ExtractAllScansParameters() {
    super(new Parameter[] {useMassList, dataFiles, file, separateFolders, exportHeader});
  }

}
