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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.function.Function;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.datastructure.ResultFormula;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.datastructure.ResultWindow;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTableModel;

/**
 * Table cell renderer
 */
class FormulaCellRenderer implements TableCellRenderer {

  protected Function<PeakListRow, ResultFormula[]> formulaProvider;
  protected Function<PeakListRow, Double> neutralMassProvider;

  public FormulaCellRenderer(Function<PeakListRow, ResultFormula[]> formulaProvider,
      Function<PeakListRow, Double> neutralMassProvider) {
    super();
    this.formulaProvider = formulaProvider;
    this.neutralMassProvider = neutralMassProvider;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int trow, int tcolumn) {
    final int row = table.convertRowIndexToModel(trow);
    PeakListRow peakListRow = ((PeakListTableModel) table.getModel()).getPeakListRow(row);

    // put in panel with button next to it
    JLabel label = new JLabel(value.toString());
    JPanel panel = new JPanel(new BorderLayout());
    JButton button = new JButton("i");
    button.setPreferredSize(new Dimension(28, 28));
    button.addActionListener(e -> {
      ResultFormula[] formulas = formulaProvider.apply(peakListRow);
      if (formulas != null && formulas.length > 0) {
        double neutralMass = neutralMassProvider.apply(peakListRow);
        // open dialog
        ResultWindow resultWindow =
            new ResultWindow(MessageFormat.format("Results for neutral mass={0}", neutralMass),
                peakListRow, neutralMass);

        // add all result formulas
        Arrays.stream(formulas).forEach(f -> resultWindow.addNewListItem(f));
        resultWindow.setVisible(true);
      }
    });

    panel.add(label, BorderLayout.CENTER);
    panel.add(button, BorderLayout.WEST);

    Color bgColor = null;
    if (isSelected)
      bgColor = table.getSelectionBackground();
    else
      bgColor = table.getBackground();

    panel.setBackground(bgColor);

    if (hasFocus) {
      Border border = null;
      if (isSelected)
        border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
      if (border == null)
        border = UIManager.getBorder("Table.focusCellHighlightBorder");

      /*
       * The "border.getBorderInsets(newPanel) != null" is a workaround for OpenJDK 1.6.0 bug,
       * otherwise setBorder() may throw a NullPointerException
       */
      if ((border != null) && (border.getBorderInsets(panel) != null)) {
        panel.setBorder(border);
      }
    }

    button.setToolTipText("Show results");
    panel.setToolTipText(value.toString());
    return panel;
  }

}
