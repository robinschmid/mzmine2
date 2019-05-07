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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionannotation.refinement;

import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;

public class IonNetworkRefinementTask extends AbstractTask {
  // Logger.
  private static final Logger LOG = Logger.getLogger(IonNetworkRefinementTask.class.getName());

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
  public IonNetworkRefinementTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists) {
    this.project = project;
    this.peakList = peakLists;
    parameters = parameterSet;

    finishedRows = 0;
    totalRows = 0;

    // tolerances
    deleteXmersOnMSMS =
        parameterSet.getParameter(IonNetworkRefinementParameters.DELETE_XMERS_ON_MSMS).getValue();
    trueThreshold =
        parameterSet.getParameter(IonNetworkRefinementParameters.TRUE_THRESHOLD).getValue();
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
    // sort and refine
    refine(peakList, trueThreshold, deleteXmersOnMSMS);
  }

  public static void refine(PeakList pkl, int trueThreshold, boolean deleteXmersOnMSMS) {
    // sort
    IonNetworkLogic.sortIonIdentities(pkl, true);

    long count = IonNetworkLogic.streamNetworks(pkl).count();
    LOG.info("Ion identity networks before refinement: " + count);

    IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, false);
    deleteAllWithoutMonomer(nets);
    deleteSmallerNetworks(nets, trueThreshold);

    // TODO new network refinement

    count = IonNetworkLogic.streamNetworks(pkl).count();
    LOG.info("Ion identity networks after refinement: " + count);
  }

  /**
   * Delete all smaller networks if one network is the preferred in all rows
   * 
   * @param nets
   * @param trueThreshold
   */
  private static void deleteSmallerNetworks(IonNetwork[] nets, int trueThreshold) {
    for (IonNetwork net : nets) {
      // not deleted
      if (net.size() > 0) {
        // delete small ones
        if (net.size() >= trueThreshold && trueThreshold > 1) {
          if (isBestNet(net)) {
            deleteAllOther(net);
          }
        }
      }
    }
  }

  /**
   * Delete all networks without monomer or with 1 monomer and >=3 multimers
   * 
   * @param nets
   */
  private static void deleteAllWithoutMonomer(IonNetwork[] nets) {
    for (IonNetwork net : nets) {
      // not deleted
      if (net.size() > 0) {
        int monomer = 0;
        int multimer = 0;
        for (Entry<PeakListRow, IonIdentity> e : net.entrySet()) {
          if (e.getValue().getIonType().getMolecules() == 1)
            monomer++;
          else if (e.getValue().getIonType().getMolecules() > 1)
            multimer++;
        }
        // no monomer
        // 1 monomer and >=3 multimers --> delete
        if (monomer == 0 || (monomer == 1 && multimer >= 3))
          net.delete();
      }
    }
  }

  /**
   * Is best network in all rows?
   * 
   * @param net
   * @return
   */
  private static boolean isBestNet(IonNetwork net) {
    for (PeakListRow row : net.keySet()) {
      IonIdentity id = row.getBestIonIdentity();
      // is best of all rows
      if (id != null && !id.getNetwork().equals(net)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Keep net but delete all other networks from all rows
   * 
   * @param net
   */
  private static void deleteAllOther(IonNetwork net) {
    for (PeakListRow row : net.keySet()) {
      Stream.of(IonNetworkLogic.getAllNetworks(row)).forEach(o -> {
        if (net.getID() != o.getID()) {
          o.delete();
        }
      });
    }
  }

  private static int getLinks(IonIdentity best) {
    int links = best.getPartnerRowsID().length;
    if (best.getMSMSMultimerCount() > 0)
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
  private static boolean deleteXmersOnMSMS(PeakListRow row, IonIdentity best, List<IonIdentity> all,
      int trueThreshold) {
    // check best first
    if (best.getMSMSMultimerCount() > 0) {
      // delete rest of annotations
      for (int i = 1; i < all.size();)
        row.getIonIdentities().get(i).delete(row);

      row.setBestIonIdentity(best);
      return true;
    } else {
      // check rest
      for (IonIdentity other : all) {
        if (other.getMSMSMultimerCount() > 0) {
          row.setBestIonIdentity(other);

          // delete rest of annotations
          for (int i = 1; i < row.getIonIdentities().size(); i++) {
            IonIdentity e = row.getIonIdentities().get(i);
            if (!other.equals(e) && (trueThreshold <= 1 || getLinks(e) < trueThreshold))
              e.delete(row);
          }
          return true;
        }
      }
    }
    return false;
  }


}
