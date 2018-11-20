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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.google.common.util.concurrent.AtomicDouble;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.MSAnnotationNetworkLogic;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.datamodel.impl.RowGroupList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary.CheckMode;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.MSAnnMSMSCheckParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.MSAnnMSMSCheckTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class MSAnnotationTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MSAnnotationTask.class.getName());

  private AtomicDouble stageProgress = new AtomicDouble(0);
  private final PeakList peakList;

  private final RTTolerance rtTolerance;
  private MSAnnotationLibrary library;

  private final ParameterSet parameters;
  private final MZmineProject project;
  private boolean neverStop = false;

  private double minHeight;
  private CheckMode checkMode;

  // MSMS
  private boolean doMSMSchecks;
  private MSAnnMSMSCheckParameters msmsChecks;

  // only correlate the ones correlated in a group
  private boolean limitByGroups;

  private CheckMode adductCheckMode;

  private boolean performAnnotationRefinement;
  private AnnotationRefinementParameters refineParam;


  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public MSAnnotationTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists) {
    this.project = project;
    this.peakList = peakLists;
    parameters = parameterSet;

    limitByGroups = parameterSet.getParameter(MSAnnotationParameters.LIMIT_BY_GROUPS).getValue();
    adductCheckMode = parameterSet.getParameter(MSAnnotationParameters.CHECK_MODE).getValue();
    // tolerances
    rtTolerance = parameterSet.getParameter(MSAnnotationParameters.RT_TOLERANCE).getValue();
    minHeight = parameterSet.getParameter(MSAnnotationParameters.MIN_HEIGHT).getValue();
    checkMode = parameterSet.getParameter(MSAnnotationParameters.CHECK_MODE).getValue();

    // MSMS refinement
    doMSMSchecks = parameterSet.getParameter(MSAnnotationParameters.MSMS_CHECK).getValue();
    msmsChecks =
        parameterSet.getParameter(MSAnnotationParameters.MSMS_CHECK).getEmbeddedParameters();

    performAnnotationRefinement =
        parameterSet.getParameter(MSAnnotationParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = parameterSet.getParameter(MSAnnotationParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();
  }

  @Override
  public double getFinishedPercentage() {
    return stageProgress.get();
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
      library = new MSAnnotationLibrary((MSAnnotationParameters) parameters);
      if (limitByGroups) {
        annotateGroups(library);
      } else {
        annotatePeakList(library);
      }
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }
  }

  private void annotatePeakList(MSAnnotationLibrary library) {
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

    // do MSMS check for multimers
    if (doMSMSchecks) {
      MSAnnMSMSCheckTask task = new MSAnnMSMSCheckTask(project, msmsChecks, peakList);
      task.run();
    }

    LOG.info(
        "A total of " + compared + " row2row comparisons with " + annotPairs + " annotation pairs");
    List net = MSAnnotationNetworkLogic.createAnnotationNetworksOld(peakList.getRows(), true,
        library.getMzTolerance());
    LOG.info("A total of " + net.size() + " networks");

    LOG.info("Show most likely annotations");
    MSAnnotationNetworkLogic.sortIonIdentities(peakList, true);


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

  private void annotateGroups(MSAnnotationLibrary library) {
    // get groups
    RowGroupList groups = peakList.getGroups();

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


    // create networks
    LOG.info("Corr: create annotation network numbers");
    List<AnnotationNetwork> nets =
        MSAnnotationNetworkLogic.createAnnotationNetworks(peakList, library.getMzTolerance(), true);

    // refinement of adducts
    // do MSMS check for multimers
    if (doMSMSchecks) {
      LOG.info("Corr: MSMS annotation refinement");
      MSAnnMSMSCheckTask task = new MSAnnMSMSCheckTask(project, msmsChecks, peakList);
      task.doCheck();
    }
    if (isCanceled())
      return;

    // refinement
    if (performAnnotationRefinement) {
      LOG.info("Corr: Refine annotations");
      AnnotationRefinementTask ref = new AnnotationRefinementTask(project, refineParam, peakList);
      ref.refine();
    }
    if (isCanceled())
      return;

    // recalc annotation networks
    MSAnnotationNetworkLogic.recalcAllAnnotationNetworks(nets, true);

    // show all annotations with the highest count of links
    LOG.info("Corr: show most likely annotations");
    MSAnnotationNetworkLogic.sortIonIdentities(peakList, true);
  }

  /**
   * Annotates all rows in a group
   * 
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(RowGroup g, AtomicInteger compared, AtomicInteger annotPairs) {
    for (int i = 0; i < g.size() - 1; i++) {
      // check against existing networks
      for (int k = i + 1; k < g.size(); k++) {
        // only if row i and k are correlated
        if (g.isCorrelated(i, k)) {
          compared.incrementAndGet();
          // check for adducts in library
          List<IonIdentity[]> id =
              library.findAdducts(peakList, g.get(i), g.get(k), adductCheckMode, minHeight);
          if (!id.isEmpty())
            annotPairs.incrementAndGet();
        }
      }
    }
  }
}
