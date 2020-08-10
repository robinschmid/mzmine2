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

package net.sf.mzmine.modules.masslistmethods.exportaslibrary;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;

public class ExportScansAsLibraryParameters extends SimpleParameterSet {
  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

  public static final ScanSelectionParameter scanSelect =
      new ScanSelectionParameter(new ScanSelection(1));

  public static final MassListParameter massList = new MassListParameter();

  public static final IntegerParameter minSignals =
      new IntegerParameter("Minimum signals", "Exclude spectra with less spectral signals", 0);

  public static final FileNameParameter file =
      new FileNameParameter("Export file", "Export file path", "json");

  public static final BooleanParameter addFileNameDescription = new BooleanParameter(
      "Add file name to description", "Adds the file name to the description (start)", true);

  public static final StringParameter description =
      new StringParameter("Description", "Scan description written to the library", "", false);

  public static final BooleanParameter bestScan = new BooleanParameter("Best single spectrum",
      "The best single spectrum in clustered spectra", true);
  public static final BooleanParameter meanScan =
      new BooleanParameter("Mean spectrum", "The mean spectrum in clustered spectra", false);
  public static final BooleanParameter maxScan =
      new BooleanParameter("Maximum spectrum", "The maximum spectrum in clustered spectra", true);
  public static final BooleanParameter sumScan =
      new BooleanParameter("Sum spectrum", "The sum spectrum in clustered spectra", false);


  public ExportScansAsLibraryParameters() {
    super(new Parameter[] {dataFiles, scanSelect, massList, minSignals, file,
        addFileNameDescription, description, bestScan, meanScan, maxScan, sumScan});
  }

}
