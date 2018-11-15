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

package net.sf.mzmine.modules.peaklistmethods.filtering.msannotations;

import java.util.logging.Level;
import java.util.logging.Logger;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;

public class FilterAnnotationTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(FilterAnnotationTask.class.getName());

  private int finishedRows;
  private int totalRows;
  private final PeakList peakList;
  private PeakList resultPeakList;

  private final ParameterSet parameters;
  private final MZmineProject project;

  private int minNetworkSize;

  private String suffix;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public FilterAnnotationTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists) {
    this.project = project;
    this.peakList = peakLists;
    parameters = parameterSet;

    finishedRows = 0;
    totalRows = 0;

    minNetworkSize =
        parameterSet.getParameter(FilterAnnotationParameters.MIN_NETWORK_SIZE).getValue();
    suffix = parameterSet.getParameter(FilterAnnotationParameters.suffix).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Filtering of annotations in " + peakList.getName() + " ";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      LOG.info("Starting adduct filtering on " + peakList.getName());

      // Create a new results peakList which is added at the end
      resultPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());
      for (PeakListRow row : peakList.getRows())
        resultPeakList.addRow(copyRow(row));

      // filter
      doFiltering(resultPeakList, minNetworkSize);

      // finish
      if (!isCanceled()) {
        addResultToProject();

        // Done.
        setStatus(TaskStatus.FINISHED);
        LOG.info("Finished adducts filtereing in " + peakList);
      }
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct filtering error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }


  /**
   * Delete all networks smaller min size
   * 
   * @param pkl
   * @param minNetSize
   * @throws Exception
   */
  public static void doFiltering(PeakList pkl, int minNetSize) throws Exception {
    for (PeakListRow row : pkl.getRows()) {
      if (row.hasIonIdentity()) {
        for (IonIdentity adduct : row.getIonIdentities()) {
          AnnotationNetwork net = adduct.getNetwork();
          if (net == null) {
            row.removePeakIdentity(adduct);
          } else if (net.size() < minNetSize) {
            // delete network
            net.delete();
          }
        }
      }
    }
  }

  /**
   * Create a copy of a peak list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static PeakListRow copyRow(final PeakListRow row) {
    // Copy the peak list row.
    final PeakListRow newRow = new SimplePeakListRow(row.getID());
    PeakUtils.copyPeakListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getPeaks()) {
      final Feature newPeak = new SimpleFeature(peak);
      PeakUtils.copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getDataFile(), newPeak);
    }

    return newRow;
  }

  /**
   * Add peaklist to project, delete old if requested, add description to result
   */
  public void addResultToProject() {
    // Add new peakList to the project
    project.addPeakList(resultPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      resultPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    resultPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("Adduct filtering ", parameters));

  }
}
