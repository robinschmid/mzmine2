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
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class ExportCorrAnnotationParameters extends SimpleParameterSet {

  // NOT INCLUDED in sub
  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final FileNameParameter FILENAME =
      new FileNameParameter("Filename", "File name", "csv");

  public static final BooleanParameter EX_ANNOTATIONS_FILE = new BooleanParameter(
      "Create annotations file", "Exports all annotations by MS annotate to a csv file", true);
  public static final BooleanParameter EX_AVGCORR_FILE = new BooleanParameter(
      "Create average correlations file",
      "Exports the average row-2-row correlation to a csv file (also exports annotations, but only those with r>minR)",
      true);

  public static final PercentParameter MIN_AVGCORR =
      new PercentParameter("Min avg corr", "Minimum r of avg corr", 0.85);

  public static final BooleanParameter EX_MZ = new BooleanParameter("Export m/z", "", true);
  public static final BooleanParameter EX_DMZ = new BooleanParameter("Export dm/z", "", true);
  public static final BooleanParameter EX_DRT = new BooleanParameter("Export dRT", "", true);
  public static final BooleanParameter EX_RT = new BooleanParameter("Export RT", "", true);
  public static final BooleanParameter EX_I = new BooleanParameter("Export intensity", "", true);
  public static final BooleanParameter EX_AREA = new BooleanParameter("Export  area", "", true);

  // Constructor
  public ExportCorrAnnotationParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, EX_ANNOTATIONS_FILE, EX_AVGCORR_FILE, MIN_AVGCORR,
        EX_MZ, EX_DMZ, EX_DRT, EX_RT, EX_I, EX_AREA});
  }

}
