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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.checkmsms;

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class IonNetworkRefinementModule implements MZmineProcessingModule {

  private static final String NAME = "Check all ion identities by MS/MS ";

  private static final String DESCRIPTION =
      "Checks ion identities (in-source fragments) and multimers) by MS/MS";

  @Override
  public @Nonnull String getName() {
    return NAME;
  }

  @Override
  public @Nonnull String getDescription() {

    return DESCRIPTION;
  }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.ION_IDENTITY_NETWORKS;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return IonNetworkMSMSCheckParameters.class;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull final ParameterSet parameters,
      @Nonnull final Collection<Task> tasks) {

    PeakList[] pkl = parameters.getParameter(IonNetworkMSMSCheckParameters.PEAK_LISTS).getValue()
        .getMatchingPeakLists();
    for (PeakList p : pkl)
      tasks.add(new IonNetworkMSMSCheckTask(project, parameters, p));

    return ExitCode.OK;
  }
}
