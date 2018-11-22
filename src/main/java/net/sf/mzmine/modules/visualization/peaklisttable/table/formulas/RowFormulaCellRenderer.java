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

package net.sf.mzmine.modules.visualization.peaklisttable.table.formulas;

import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.datastructure.ResultFormula;

/**
 * Renderer for formula of peakListRow.getBestIonIdentity().getMolFormula
 */
class RowFormulaCellRenderer extends FormulaCellRenderer {

  public RowFormulaCellRenderer() {
    super(
        // provide list of formulas
        row -> {
          IonIdentity best = row.getBestIonIdentity();
          if (best != null && best.getBestMolFormula() != null) {
            return best.getMolFormulas().stream().filter(ResultFormula.class::isInstance)
                .map(ResultFormula.class::cast).toArray(ResultFormula[]::new);
          }
          return new ResultFormula[0];
        },
        // provide neutral mass
        row -> {
          IonIdentity best = row.getBestIonIdentity();
          if (best != null && best.getBestMolFormula() != null) {
            return best.getIonType().getMass(row.getAverageMZ());
          }
          return -1d;
        });
  }

}
