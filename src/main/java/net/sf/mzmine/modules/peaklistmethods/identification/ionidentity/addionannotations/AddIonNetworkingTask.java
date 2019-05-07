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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.addionannotations;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import net.sf.mzmine.datamodel.identities.iontype.networks.IonNetworkSorter;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.datamodel.impl.RowGroupList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.minfeaturefilter.MinimumFeatureFilter;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.minfeaturefilter.MinimumFeatureFilter.OverlapResult;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.IonNetworkLibrary;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.IonNetworkLibrary.CheckMode;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkMSMSCheckParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkMSMSCheckTask;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkRefinementParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement.IonNetworkRefinementTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class AddIonNetworkingTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(AddIonNetworkingTask.class.getName());

  private AtomicDouble stageProgress = new AtomicDouble(0);
  private final PeakList peakList;

  private final RTTolerance rtTolerance;
  private IonNetworkLibrary library;

  private final ParameterSet parameters;
  private final MZmineProject project;
  private boolean neverStop = false;

  private double minHeight;
  private CheckMode checkMode;

  // MSMS
  private boolean doMSMSchecks;
  private IonNetworkMSMSCheckParameters msmsChecks;

  // only correlate the ones correlated in a group
  private boolean limitByGroups;

  private CheckMode adductCheckMode;

  private boolean performAnnotationRefinement;
  private IonNetworkRefinementParameters refineParam;

  private MZTolerance mzTolerance;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public AddIonNetworkingTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists, MinimumFeatureFilter minFeaturesFilter) {
    this.project = project;
    this.peakList = peakLists;
    parameters = parameterSet;

    limitByGroups =
        parameterSet.getParameter(AddIonNetworkingParameters.LIMIT_BY_GROUPS).getValue();
    // tolerances
    rtTolerance = parameterSet.getParameter(AddIonNetworkingParameters.RT_TOLERANCE).getValue();
    mzTolerance = parameterSet.getParameter(AddIonNetworkingParameters.MZ_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(AddIonNetworkingParameters.MIN_HEIGHT).getValue();

    // MSMS refinement
    doMSMSchecks = parameterSet.getParameter(AddIonNetworkingParameters.MSMS_CHECK).getValue();
    msmsChecks =
        parameterSet.getParameter(AddIonNetworkingParameters.MSMS_CHECK).getEmbeddedParameters();

    performAnnotationRefinement =
        parameterSet.getParameter(AddIonNetworkingParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = parameterSet.getParameter(AddIonNetworkingParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();
  }

  public AddIonNetworkingTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists) {
    this(project, parameterSet, peakLists, null);
  }

  @Override
  public double getFinishedPercentage() {
    return getStatus().equals(TaskStatus.FINISHED) ? 1 : stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Identification of adducts, in-source fragments and clusters in " + peakList.getName()
        + " ";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      // create library
      LOG.info("Creating annotation library");
      library =
          parameters.getParameter(AddIonNetworkingParameters.LIBRARY).createLibrary(mzTolerance);
      if (limitByGroups) {
        annotateGroups(library);
      } else {
        annotatePeakList(library);
      }
      setStatus(TaskStatus.FINISHED);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  private void annotatePeakList(IonNetworkLibrary library) {
    LOG.info("Starting adduct detection on " + peakList.getName());
    // use average RT
    boolean useAvgRT = CheckMode.AVGERAGE.equals(checkMode);

    // work
    RawDataFile[] raw = peakList.getRawDataFiles();
    PeakListRow[] rows = peakList.getRows();
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));
    int totalRows = rows.length;
    // for all rows
    int compared = 0;
    int annotPairs = 0;
    for (int i = 0; i < rows.length; i++) {
      boolean stopSearch = false;
      for (int k = i + 1; k < rows.length && (neverStop || !stopSearch); k++) {
        PeakListRow p0 = rows[i];
        PeakListRow p1 = rows[k];
        // is within retention time in all raw data files
        boolean inRange = true;

        // check average
        if (useAvgRT) {
          double rt0 = p0.getAverageRT();
          double rt1 = p1.getAverageRT();
          // upper end of search at 3 times tolerance to safe processing time
          if (!rtTolerance.checkWithinTolerance(rt0, rt1)) {
            inRange = false;
            stopSearch = true;
          }
        } else {
          if (minFeaturesFilter != null) {
            inRange = minFeaturesFilter.filterMinFeaturesOverlap(raw, rows[i], rows[k])
                .equals(OverlapResult.TRUE);
          } else {
            // check all raw data files with both peaks
            for (int r = 0; r < raw.length && inRange; r++) {
              if (p0.hasPeak(raw[r]) && p1.hasPeak(raw[r])) {
                double rt0 = p0.getPeak(raw[r]).getRT();
                double rt1 = p1.getPeak(raw[r]).getRT();
                // upper end of search at 3 times tolerance to safe processing time
                double upperEndSearch =
                    (rtTolerance.getToleranceRange(rt0).upperEndpoint() - rt0) * 3;
                if (rt1 > upperEndSearch)
                  stopSearch = true;
                if (!rtTolerance.checkWithinTolerance(rt0, rt1))
                  inRange = false;
              }
            }
          }
        }


        // check row against row
        if (inRange) {
          // check for adducts in library
          List<IonIdentity[]> id = library.findAdducts(peakList, p0, p1, p0.getRowCharge(),
              p1.getRowCharge(), checkMode, minHeight);
          compared++;
          if (!id.isEmpty())
            annotPairs++;
        }
      }
      stageProgress.set(i / (double) totalRows);
    }

    //
    refineAndFinishNetworks();

    // finish
    if (!isCanceled()) {
      peakList.addDescriptionOfAppliedTask(
          new SimplePeakListAppliedMethod("Identification of adducts", parameters));

      // Repaint the window to reflect the change in the peak list
      Desktop desktop = MZmineCore.getDesktop();
      if (!(desktop instanceof HeadLessDesktop))
        desktop.getMainWindow().repaint();

      // Done.
      setStatus(TaskStatus.FINISHED);
      LOG.info("Finished adducts search in " + peakList);
    }
  }

  private void annotateGroups(IonNetworkLibrary library) {
    // get groups
    RowGroupList groups = peakList.getGroups();

    if (groups == null || groups.isEmpty())
      throw new MSDKRuntimeException(
          "Run grouping before: No groups found for peakList + " + peakList.getName());
    //
    AtomicInteger compared = new AtomicInteger(0);
    AtomicInteger annotPairs = new AtomicInteger(0);
    // for all groups
    groups.parallelStream().forEach(g -> {
      if (!this.isCanceled()) {
        annotateGroup(g, compared, annotPairs);
        stageProgress.addAndGet(1d / groups.size());
      }
    });
    LOG.info("Corr: A total of " + compared.get() + " row2row adduct comparisons with "
        + annotPairs.get() + " annotation pairs");

    refineAndFinishNetworks();
  }

  /**
   * Annotates all rows in a group
   * 
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(RowGroup g,
      // AtomicInteger finished,
      AtomicInteger compared, AtomicInteger annotPairs) {
    // all networks of this group
    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(g.toArray(new PeakListRow[g.size()]), false);

    for (int i = 0; i < g.size(); i++) {
      // min height
      if (g.get(i).getBestPeak().getHeight() >= minHeight) {
        for (IonNetwork net : nets) {
          // check against existing networks
          if (isCorrelated(g, g.get(i), net)) {
            compared.incrementAndGet();
            // check for adducts in library
            List<IonIdentity[]> id = library.findAdducts(peakList, g.get(i), net);
            if (!id.isEmpty())
              annotPairs.incrementAndGet();
          }
        }
      }
      // finished.incrementAndGet();
    }
  }


  /**
   * minimum correlation between row and network
   * 
   * @param g
   * @param a
   * @param net
   * @return
   */
  private boolean isCorrelated(RowGroup g, PeakListRow a, IonNetwork net) {
    int n = net.size();
    int correlated = 0;
    for (PeakListRow b : net.keySet()) {
      if (g.isCorrelated(a, b))
        correlated++;
    }
    return (double) correlated / (double) n >= 0.5;
  }

  private void refineAndFinishNetworks() {
    // create network IDs
    LOG.info("Corr: create annotation network numbers");
    AtomicInteger netID = new AtomicInteger(0);
    IonNetworkLogic
        .streamNetworks(peakList,
            new IonNetworkSorter(SortingProperty.RT, SortingDirection.Ascending), false)
        .forEach(n -> {
          n.setMzTolerance(library.getMzTolerance());
          n.setID(netID.getAndIncrement());
        });

    // recalc annotation networks
    IonNetworkLogic.recalcAllAnnotationNetworks(peakList, true);

    // refinement of adducts
    // do MSMS check for multimers
    if (doMSMSchecks) {
      LOG.info("Corr: MSMS annotation checks");
      IonNetworkMSMSCheckTask task = new IonNetworkMSMSCheckTask(project, msmsChecks, peakList);
      task.doCheck();
    }
    if (isCanceled())
      return;

    // refinement
    if (performAnnotationRefinement) {
      LOG.info("Corr: Refine annotations");
      IonNetworkRefinementTask ref = new IonNetworkRefinementTask(project, refineParam, peakList);
      ref.refine();
    }
    if (isCanceled())
      return;

    // recalc annotation networks
    IonNetworkLogic.recalcAllAnnotationNetworks(peakList, true);

    // show all annotations with the highest count of links
    LOG.info("Corr: show most likely annotations");
    IonNetworkLogic.sortIonIdentities(peakList, true);
  }
}
