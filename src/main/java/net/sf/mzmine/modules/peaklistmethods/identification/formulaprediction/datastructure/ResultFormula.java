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

import java.util.Map;
import org.openscience.cdk.interfaces.IMolecularFormula;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;

public class ResultFormula extends MolecularFormulaIdentity {

  private Double rdbeValue, isotopeScore, msmsScore;
  private IsotopePattern predictedIsotopePattern;
  private Map<DataPoint, String> msmsAnnotation;

  public ResultFormula(IMolecularFormula cdkFormula, IsotopePattern predictedIsotopePattern,
      Double rdbeValue, Double isotopeScore, Double msmsScore,
      Map<DataPoint, String> msmsAnnotation) {
    super(cdkFormula);
    this.predictedIsotopePattern = predictedIsotopePattern;
    this.isotopeScore = isotopeScore;
    this.msmsScore = msmsScore;
    this.msmsAnnotation = msmsAnnotation;
    this.rdbeValue = rdbeValue;
  }

  public Double getRDBE() {
    return rdbeValue;
  }

  public Map<DataPoint, String> getMSMSannotation() {
    return msmsAnnotation;
  }

  public IsotopePattern getPredictedIsotopes() {
    return predictedIsotopePattern;
  }

  @Override
  public Double getIsotopeScore() {
    return isotopeScore;
  }

  @Override
  public Double getMSMSScore() {
    return msmsScore;
  }

  @Override
  public double getScore(double neutralMass, double ppmMax, double fIsotopeScore,
      double fMSMSscore) {
    double ppmScore = super.getPPMScore(neutralMass, ppmMax);
    double totalScore = ppmScore;
    double div = 1;
    if (isotopeScore != null) {
      totalScore += isotopeScore * fIsotopeScore;
      div += fIsotopeScore;
    }
    if (msmsScore != null) {
      totalScore += msmsScore * fMSMSscore;
      div += fMSMSscore;
    }

    return totalScore / div;
  }


}
