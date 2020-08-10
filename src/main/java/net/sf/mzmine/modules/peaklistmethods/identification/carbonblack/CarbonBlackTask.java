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

package net.sf.mzmine.modules.peaklistmethods.identification.carbonblack;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

class CarbonBlackTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private static final double ELECTRON_MASS = 0.00054858;
  private static final double C13 = 1.0033548;

  private PeakList peakList;

  private AtomicInteger finished = new AtomicInteger(0);
  private int total = 0;
  private MZTolerance mzTolerance;
  private ParameterSet parameters;
  private int minConsecutive;
  private final double ionDiff;
  private String masses;

  CarbonBlackTask(PeakList peakList, ParameterSet parameters) {
    this.peakList = peakList;
    this.parameters = parameters;
    total = peakList.getRows().length;

    minConsecutive = parameters.getParameter(CarbonBlackParameters.minConsecutive).getValue();
    mzTolerance = parameters.getParameter(CarbonBlackParameters.mzTolerance).getValue();
    masses = parameters.getParameter(CarbonBlackParameters.masses).getValue();

    switch (parameters.getParameter(CarbonBlackParameters.polarity).getValue()) {
      case NEGATIVE:
        ionDiff = ELECTRON_MASS;
        break;
      case POSITIVE:
        ionDiff = -ELECTRON_MASS;
        break;
      case UNKNOWN:
      case NEUTRAL:
      default:
        ionDiff = 0d;
        break;
    }
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (total == 0)
      return 0;
    return ((double) finished.get()) / total;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Searching for carbon black signals in " + peakList;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    peakList.parallelStream().forEach(row -> {
      if (getStatus().equals(TaskStatus.PROCESSING)) {
        MassList scan = row.getBestPeak().getRepresentativeScan().getMassList(masses);
        if (scan == null) {
          logger.log(Level.WARNING, "No mass list with name " + masses);
          setErrorMessage("No mass list with name " + masses);
          setStatus(TaskStatus.ERROR);
          return;
        }
        DataPoint[] dps = scan.getDataPoints();
        DataPointSorter sorter =
            new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending);
        Arrays.sort(dps, sorter);

        // try to find carbon signals in mass list
        // C0 to C100
        DataPoint[] carbon = new DataPoint[100];

        for (DataPoint dp : dps) {
          int c = Math.round((float) dp.getMZ() / 12.f);
          if (mzTolerance.checkWithinTolerance(dp.getMZ(), c * 12.d + ionDiff)) {
            if (carbon[c] == null || carbon[c].getIntensity() < dp.getIntensity())
              carbon[c] = dp;
          }
        }


        finished.getAndIncrement();
      }
    });

    setStatus(TaskStatus.FINISHED);
  }

  private void addIdentitiy(PeakListRow row) {
    // add new identity to the row
    row.addPeakIdentity(newIdentity, false);

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
  }
}
