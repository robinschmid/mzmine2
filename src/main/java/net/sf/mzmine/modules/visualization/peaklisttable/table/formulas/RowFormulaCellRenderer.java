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

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;

/**
 * Renderer for formula of peakListRow.getBestIonIdentity().getMolFormula
 */
public class RowFormulaCellRenderer extends FormulaCell {

  public RowFormulaCellRenderer(JTable table) {
    super();
  }

  @Override
  protected double getMass(PeakListRow row) {
    IonIdentity best = row.getBestIonIdentity();
    if (best != null && best.getBestMolFormula() != null) {
      return best.getIonType().getMass(row.getAverageMZ());
    }
    return -1d;
  }

  @Override
  protected MolecularFormulaIdentity[] getFormulas(PeakListRow row) {
    IonIdentity best = row.getBestIonIdentity();
    if (best != null && best.getBestMolFormula() != null) {
      return best.getMolFormulas().stream().filter(MolecularFormulaIdentity.class::isInstance)
          .map(MolecularFormulaIdentity.class::cast).toArray(MolecularFormulaIdentity[]::new);
    }
    return new MolecularFormulaIdentity[0];
  }

  @Override
  protected void setSelectedFormula(JTable table, int rowi, PeakListRow row,
      MolecularFormulaIdentity formula) {
    IonIdentity bestIon = row.getBestIonIdentity();
    if (bestIon != null) {
      bestIon.setBestMolFormula(formula);
      AbstractTableModel model = ((AbstractTableModel) table.getModel());
      model.fireTableRowsUpdated(rowi, rowi);
    }
  }

}
