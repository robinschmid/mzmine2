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

package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.datastructure;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;

public class AverageResultFormula extends MolecularFormulaIdentity {

  private List<MolecularFormulaIdentity> formulas = new ArrayList<>();
  private Double rdbe = null;

  public AverageResultFormula(MolecularFormulaIdentity f) {
    super(f.getFormulaAsObject());
    formulas.add(f);
    if (f instanceof ResultFormula)
      rdbe = ((ResultFormula) f).getRDBE();
  }

  public Double getRDBE() {
    return rdbe;
  }

  public List<MolecularFormulaIdentity> getFormulas() {
    return formulas;
  }

  public boolean isMatching(MolecularFormulaIdentity f) {
    return f.equalFormula(formulas.get(0));
  }

  public boolean addFormula(MolecularFormulaIdentity f) {
    if (isMatching(f)) {
      formulas.add(f);
      if (rdbe == null || f instanceof ResultFormula)
        rdbe = ((ResultFormula) f).getRDBE();

      return true;
    }
    return false;
  }

  public void removeFormula(MolecularFormulaIdentity f) {
    formulas.remove(f);
  }

  @Override
  public Double getIsotopeScore() {
    double avg = formulas.stream().filter(f -> f.getIsotopeScore() != null)
        .mapToDouble(MolecularFormulaIdentity::getIsotopeScore).average().orElse(-1);
    return avg == -1 ? null : avg;
  }

  @Override
  public Double getMSMSScore() {
    double avg = formulas.stream().filter(f -> f.getMSMSScore() != null)
        .mapToDouble(MolecularFormulaIdentity::getMSMSScore).average().orElse(-1);
    return avg == -1 ? null : avg;
  }

  @Override
  public double getPPMScore(double neutralMass, double ppmMax) {
    return formulas.stream().mapToDouble(f -> f.getPPMScore(neutralMass, ppmMax)).average()
        .orElse(0);
  }


  @Override
  public double getScore(double neutralMass, double ppmMax, double fIsotopeScore,
      double fMSMSscore) {
    double ppmScore = super.getPPMScore(neutralMass, ppmMax);
    double totalScore = ppmScore;
    double div = 1;
    if (getIsotopeScore() != null) {
      totalScore += getIsotopeScore() * fIsotopeScore;
      div += fIsotopeScore;
    }
    if (getMSMSScore() != null) {
      totalScore += getMSMSScore() * fMSMSscore;
      div += fMSMSscore;
    }

    return totalScore / div;
  }

}
