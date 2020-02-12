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

package net.sf.mzmine.modules.tools.analyzelibraries;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNamesParameter;

public class AnalyzeSpectralLibraryParameters extends SimpleParameterSet {

  public static final FileNameParameter output =
      new FileNameParameter("Output directory", "csv", "csv");
  public static final FileNamesParameter dataBaseFile = new FileNamesParameter(
      "Spectral database file",
      "(GNPS json, MONA json, NIST msp, JCAMP-DX jdx) Name of file that contains information for peak identification",
      null);

  public AnalyzeSpectralLibraryParameters() {
    super(new Parameter[] {dataBaseFile, output});
  }

}