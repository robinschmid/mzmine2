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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.export;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class ExportCorrAnnotationParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Filename",
      "Name of the output MGF file. "
          + "Use pattern \"{}\" in the file name to substitute with peak list name. "
          + "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). "
          + "If the file already exists, it will be overwritten.",
      "mgf");

  public static final BooleanParameter EX_ANNOTATIONS =
      new BooleanParameter("Export annotations", "Exports annotations by MS annotate", true);
  public static final BooleanParameter EX_AVGCORR =
      new BooleanParameter("Export average correlations",
          "Exports the average row-2-row correlation of separate correlations in different files",
          true);
  public static final BooleanParameter EX_TOTALCORR = new BooleanParameter(
      "Export total correlations",
      "Exports the total row-2-row correlation of all data points of all features shapes across all samples",
      true);
  public static final BooleanParameter EX_IMAX_CORR =
      new BooleanParameter("Export max intensity correlations",
          "Exports the correlation of the maximum intensities across all samples", true);

  // Constructor
  public ExportCorrAnnotationParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, EX_ANNOTATIONS, EX_AVGCORR, EX_TOTALCORR,
        EX_IMAX_CORR});
  }

}
