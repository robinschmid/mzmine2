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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import net.sf.mzmine.datamodel.identities.iontype.networks.IonNetworkSorter;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.datamodel.impl.RowGroupList;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.minfeaturefilter.MinimumFeatureFilter;
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
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Add row/ionidentites to existing networks
 * 
 * @author r_schm33
 *
 */
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
      library = new IonNetworkLibrary(
          parameters.getParameter(AddIonNetworkingParameters.LIBRARY).getEmbeddedParameters(),
          mzTolerance);
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
    LOG.info("Starting adduct detection on peaklist " + peakList.getName());
    //
    AtomicInteger compared = new AtomicInteger(0);
    AtomicInteger annotPairs = new AtomicInteger(0);

    // all networks of this group
    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(peakList, false);

    for (PeakListRow row : peakList.getRows()) {
      if (this.isCanceled()) {
        setStatus(TaskStatus.CANCELED);
        return;
      }
      // min height
      if (row.getBestPeak().getHeight() >= minHeight) {
        for (IonNetwork net : nets) {
          if (!net.isUndefined()) {
            if (rtTolerance.checkWithinTolerance(net.getAvgRT(), row.getAverageRT())) {
              // only if not already in network
              if (!net.containsKey(row)) {
                // check against existing networks
                compared.incrementAndGet();
                // check for adducts in library
                IonIdentity id = library.findAdducts(row, net);
                if (id != null)
                  annotPairs.incrementAndGet();
              }
            }
          }
        }
        stageProgress.addAndGet(1d / peakList.getNumberOfRows());
      }
      // finished.incrementAndGet();
    }
    LOG.info("Corr: A total of " + compared.get() + " row2row adduct comparisons with "
        + annotPairs.get() + " annotation pairs");

    refineAndFinishNetworks();
  }

  private void annotateGroups(IonNetworkLibrary library) {
    LOG.info("Starting adduct detection on groups of peaklist " + peakList.getName());
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
        annotateGroup(library, g, compared, annotPairs);
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
   * @param library
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(IonNetworkLibrary library, RowGroup g,
      // AtomicInteger finished,
      AtomicInteger compared, AtomicInteger annotPairs) {
    // all networks of this group
    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(g.toArray(new PeakListRow[g.size()]), false);

    for (int i = 0; i < g.size(); i++) {
      PeakListRow row = g.get(i);
      // min height
      if (g.get(i).getBestPeak().getHeight() >= minHeight) {
        for (IonNetwork net : nets) {
          if (!net.isUndefined()) {
            // only if not already in network
            if (!net.containsKey(row)) {
              // check against existing networks
              if (isCorrelated(g, g.get(i), net)) {
                compared.incrementAndGet();
                // check for adducts in library
                IonIdentity id = library.findAdducts(g.get(i), net);
                if (id != null)
                  annotPairs.incrementAndGet();
              }
            }
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
