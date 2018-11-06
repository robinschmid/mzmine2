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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement;

import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;

public class AnnotationRefinementTask extends AbstractTask {
  // Logger.
  private static final Logger LOG = Logger.getLogger(AnnotationRefinementTask.class.getName());

  private int finishedRows;
  private int totalRows;
  private final PeakList peakList;

  private final ParameterSet parameters;
  private final MZmineProject project;

  // >= trueThreshold delete all other occurance in networks
  private int trueThreshold = 4;
  // delete all other xmers when one was confirmed in MSMS
  private boolean deleteXmersOnMSMS = true;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public AnnotationRefinementTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists) {
    this.project = project;
    this.peakList = peakLists;
    parameters = parameterSet;

    finishedRows = 0;
    totalRows = 0;

    // tolerances
    deleteXmersOnMSMS =
        parameterSet.getParameter(AnnotationRefinementParameters.DELETE_XMERS_ON_MSMS).getValue();
    trueThreshold =
        parameterSet.getParameter(AnnotationRefinementParameters.TRUE_THRESHOLD).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows / totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Refinement of annotations " + peakList.getName() + " ";
  }

  @Override
  public void run() {
    refine();
  }


  public void refine() {
    refine(peakList, trueThreshold, deleteXmersOnMSMS);
  }

  public static void refine(PeakList pkl, int trueThreshold, boolean deleteXmersOnMSMS) {
    for (PeakListRow row : pkl.getRows()) {
      ESIAdductIdentity best = MSAnnotationNetworkLogic.getMostLikelyAnnotation(row, true);
      if (best == null)
        continue;

      List<ESIAdductIdentity> all = MSAnnotationNetworkLogic.getAllAnnotationsSorted(row);
      if (deleteXmersOnMSMS && all.size() > 1) {
        // xmers
        if (deleteXmersOnMSMS(row, best, all, trueThreshold)) {
          best = MSAnnotationNetworkLogic.getMostLikelyAnnotation(row, true);
          all = MSAnnotationNetworkLogic.getAllAnnotationsSorted(row);
        }
      }

      if (best == null)
        continue;

      if (trueThreshold > 1) {
        int links = getLinks(best);

        if (links >= trueThreshold) {
          for (ESIAdductIdentity other : all)
            if (!other.equals(best))
              other.delete(row);
        }
      }
    }
  }

  private static int getLinks(ESIAdductIdentity best) {
    int links = best.getPartnerRowsID().length;
    if (best.getMSMSModVerify() > 0)
      links++;
    return links;
  }

  /**
   * Delete xmers if one was verified by msms
   * 
   * @param row
   * @param best
   * @param all
   * @return
   */
  private static boolean deleteXmersOnMSMS(PeakListRow row, ESIAdductIdentity best,
      List<ESIAdductIdentity> all, int trueThreshold) {
    // check best first
    if (best.getMSMSMultimerCount() > 0) {
      // delete rest of annotations
      for (ESIAdductIdentity other : all)
        if (!other.equals(best))
          other.delete(row);

      row.setPreferredPeakIdentity(best);
      return true;
    } else {
      // check rest
      for (ESIAdductIdentity other : all) {
        if (other.getMSMSMultimerCount() > 0) {
          row.setPreferredPeakIdentity(other);

          // delete rest of annotations
          for (ESIAdductIdentity e : all)
            if (!other.equals(e) && (trueThreshold <= 1 || getLinks(e) < trueThreshold))
              e.delete(row);
          return true;
        }
      }
    }
    return false;
  }


}
