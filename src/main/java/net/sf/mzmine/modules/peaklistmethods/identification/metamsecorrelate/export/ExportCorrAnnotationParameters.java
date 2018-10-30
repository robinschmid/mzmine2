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
  public static final BooleanParameter EX_ANNOT =
      new BooleanParameter("Export MS1 annotation edges", "", true);
  public static final BooleanParameter EX_CORR =
      new BooleanParameter("Export correlation edges", "", true);
  public static final BooleanParameter EX_ONLY_ANNOTATED =
      new BooleanParameter("Export only annotated rows in correlation edge file", "", true);
  public static final PercentParameter MIN_R =
      new PercentParameter("Min correlation (r)", "Minimum Pearson correlation", 0.9);

  // Constructor
  public ExportCorrAnnotationParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, EX_ANNOT, EX_CORR, EX_ONLY_ANNOTATED, MIN_R});
  }

}
