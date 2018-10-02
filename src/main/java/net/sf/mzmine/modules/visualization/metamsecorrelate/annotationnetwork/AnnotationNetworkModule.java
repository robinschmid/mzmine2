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

package net.sf.mzmine.modules.visualization.metamsecorrelate.annotationnetwork;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineRunnableModule;
import net.sf.mzmine.modules.visualization.metamsecorrelate.annotationnetwork.visual.AnnotationNetworkFrame;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

/**
 * TIC/XIC visualizer using JFreeChart library
 */
public class AnnotationNetworkModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "MS annotation networks";
  private static final String MODULE_DESCRIPTION =
      "Visualise the results of the MS annotation module";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    PeakList[] pkls = parameters.getParameter(AnnotationNetworkParameters.PEAK_LISTS).getValue()
        .getMatchingPeakLists();
    if (pkls != null && pkls.length > 0) {
      AnnotationNetworkFrame f = new AnnotationNetworkFrame(pkls[0]);
      f.setVisible(true);
      return ExitCode.OK;
    }
    return ExitCode.ERROR;
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.VISUALIZATIONPEAKLIST;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return AnnotationNetworkParameters.class;
  }
}
