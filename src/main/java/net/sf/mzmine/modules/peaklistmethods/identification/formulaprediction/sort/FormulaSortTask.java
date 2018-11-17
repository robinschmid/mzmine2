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
package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.sort;

import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.FormulaUtils;

public class FormulaSortTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private PeakList peakList;
  private String message;
  private int totalRows;
  private int finishedRows = 0;
  private Double weightIsotopeScore;
  private Double ppmMaxWeight;
  private Double weightMSMSscore;

  /**
   *
   * @param parameters
   * @param peakList
   * @param peakListRow
   * @param peak
   */
  FormulaSortTask(PeakList peakList, ParameterSet parameters) {

    /*
     * searchedMass = parameters.getParameter(
     * FormulaPredictionPeakListParameters.neutralMass).getValue();
     */
    this.peakList = peakList;
    weightIsotopeScore =
        parameters.getParameter(FormulaSortParameters.ISOTOPE_SCORE_WEIGHT).getValue();
    ppmMaxWeight = parameters.getParameter(FormulaSortParameters.MAX_PPM_WEIGHT).getValue();
    weightMSMSscore = parameters.getParameter(FormulaSortParameters.MSMS_SCORE_WEIGHT).getValue();

    message = "Sorting formula lists of peak list " + peakList.getName();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0.0;
    return finishedRows / (double) totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return message;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    for (PeakListRow row : peakList.getRows()) {
      if (row.hasIonIdentity()) {
        for (IonIdentity ion : row.getIonIdentities()) {
          double neutralMass = ion.getIonType().getMass(row.getAverageMZ());
          sort(ion.getMolFormulas(), neutralMass);
          if (ion.getNetwork() != null)
            sort(ion.getNetwork().getMolFormulas(), ion.getNetwork().getNeutralMass());
        }
      }

      finishedRows++;
    }

    logger.finest("Finished formula search for all networks");
    setStatus(TaskStatus.FINISHED);
  }

  private void sort(List<MolecularFormulaIdentity> list, double neutralMass) {
    FormulaUtils.sortFormulaList(list, neutralMass, ppmMaxWeight, weightIsotopeScore,
        weightMSMSscore);
  }

}
