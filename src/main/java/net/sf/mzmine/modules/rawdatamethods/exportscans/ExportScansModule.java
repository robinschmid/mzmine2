/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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

import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;

/**
 * Module for identifying peaks by searching custom databases file.
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster)
 */
public class ExportScansModule implements MZmineModule {

  private static final String MODULE_NAME = "Export spectra module";
  private static final String MODULE_DESCRIPTION = "Export spectra to different formats";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  /**
   * Show dialog for identifying a single peak-list row.
   * 
   */
  public static void showSetupDialog(final Scan scan) {
    showSetupDialog(new Scan[] {scan});
  }

  public static void showSetupDialog(Scan[] scans) {
    final ExportScansParameters parameters = (ExportScansParameters) MZmineCore
        .getConfiguration().getModuleParameters(ExportScansModule.class);;

    // Run task.
    if (parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true) == ExitCode.OK) {
      MZmineCore.getTaskController().addTask(new ExportScansTask(scans, parameters));
    }
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return ExportScansParameters.class;
  }

}
