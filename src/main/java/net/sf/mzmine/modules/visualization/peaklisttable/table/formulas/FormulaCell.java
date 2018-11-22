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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.MessageFormat;
import java.util.Arrays;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.datastructure.ResultWindow;
import net.sf.mzmine.modules.visualization.peaklisttable.table.PeakListTableModel;

/**
 * Table cell renderer
 */
public abstract class FormulaCell extends AbstractCellEditor
    implements TableCellRenderer, TableCellEditor, ActionListener, MouseListener {

  private int mnemonic;
  private Border originalBorder;
  private Border focusBorder;

  private JPanel renderPanel;
  private JLabel renderLabel;
  private JButton renderButton;
  private JButton editButton;
  private JPanel editPanel;
  private JComboBox<String> editCombo;
  private Object editorValue;

  public FormulaCell() {
    this(null, -1);
  }

  /**
   * Create the ButtonColumn to be used as a renderer and editor. The renderer and editor will
   * automatically be installed on the TableColumn of the specified column.
   *
   * @param table the table containing the button renderer/editor
   * @param action the Action to be invoked when the button is invoked
   * @param column the column to which the button renderer/editor is added
   */
  public FormulaCell(JTable table, int column) {
    renderPanel = new JPanel(new BorderLayout());
    renderButton = new JButton();
    renderPanel.add(renderButton, BorderLayout.WEST);
    renderLabel = new JLabel();
    renderPanel.add(renderLabel, BorderLayout.CENTER);

    if (table != null) {
      if (column != -1) {
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
      }
      table.addMouseListener(this);
    }
  }


  /**
   * Get foreground color of the button when the cell has focus
   *
   * @return the foreground color
   */
  public Border getFocusBorder() {
    return focusBorder;
  }

  /**
   * The foreground color of the button when the cell has focus
   *
   * @param focusBorder the foreground color
   */
  public void setFocusBorder(Border focusBorder) {
    this.focusBorder = focusBorder;
    editButton.setBorder(focusBorder);
  }

  public int getMnemonic() {
    return mnemonic;
  }

  /**
   * The mnemonic to activate the button when the cell has focus
   *
   * @param mnemonic the mnemonic
   */
  public void setMnemonic(int mnemonic) {
    this.mnemonic = mnemonic;
    renderButton.setMnemonic(mnemonic);
    editButton.setMnemonic(mnemonic);
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
      int trow, int tcolumn) {
    if (value == null)
      return null;

    final int row = table.convertRowIndexToModel(trow);
    PeakListRow peakListRow = ((PeakListTableModel) table.getModel()).getPeakListRow(row);
    MolecularFormulaIdentity[] formulas = getFormulas(peakListRow);

    if (formulas == null || formulas.length == 0)
      return null;

    // put in panel with button next to it
    editPanel = new JPanel(new BorderLayout());
    editButton = new JButton();
    editPanel.add(editButton, BorderLayout.WEST);
    editCombo = new JComboBox<>();
    editPanel.add(editCombo, BorderLayout.CENTER);
    editButton.setFocusPainted(false);
    editButton.addActionListener(this);
    originalBorder = editButton.getBorder();
    setFocusBorder(new LineBorder(Color.BLUE));
    editButton.setPreferredSize(new Dimension(40, 28));

    editButton.addActionListener(e -> {
      if (formulas != null && formulas.length > 0) {
        double neutralMass = getMass(peakListRow);
        // open dialog
        ResultWindow resultWindow =
            new ResultWindow(MessageFormat.format("Results for neutral mass={0}", neutralMass),
                peakListRow, neutralMass);

        // add all result formulas
        Arrays.stream(formulas).forEach(f -> resultWindow.addNewListItem(f));
        resultWindow.setVisible(true);
      }
    });

    // put all values in combobox
    editCombo.setModel(new DefaultComboBoxModel(formulas));
    editCombo.setSelectedIndex(0);

    editCombo.addItemListener(e -> {
      JComboBox<?> combo = (JComboBox<?>) e.getSource();
      Object item = combo.getSelectedItem();
      if (item instanceof MolecularFormulaIdentity) {
        setSelectedFormula(table, row, peakListRow, (MolecularFormulaIdentity) item);
      }
    });

    this.editorValue = value;
    return editPanel;
  }

  protected abstract double getMass(PeakListRow peakListRow);

  protected abstract MolecularFormulaIdentity[] getFormulas(PeakListRow peakListRow);

  protected abstract void setSelectedFormula(JTable table, int rowi, PeakListRow row,
      MolecularFormulaIdentity formula);

  @Override
  public Object getCellEditorValue() {
    return editorValue;
  }

  //
  // Implement TableCellRenderer interface
  //
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
      boolean hasFocus, int row, int column) {
    if (isSelected) {
      renderPanel.setForeground(table.getSelectionForeground());
      renderPanel.setBackground(table.getSelectionBackground());
    } else {
      renderPanel.setForeground(table.getForeground());
      renderPanel.setBackground(UIManager.getColor("Button.background"));
    }

    if (hasFocus) {
      renderPanel.setBorder(focusBorder);
    } else {
      renderPanel.setBorder(originalBorder);
    }

    // renderButton.setText( (value == null) ? "" : value.toString() );
    if (value == null) {
      renderLabel.setText("");
    } else {
      renderLabel.setText(value.toString());
    }

    return renderPanel;
  }

  //
  // Implement ActionListener interface
  //
  /*
   * The button has been pressed. Stop editing and invoke the custom Action
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    // int row = table.convertRowIndexToModel(table.getEditingRow());
    // fireEditingStopped();
  }

  //
  // Implement MouseListener interface
  //
  /*
   * When the mouse is pressed the editor is invoked. If you then then drag the mouse to another
   * cell before releasing it, the editor is still active. Make sure editing is stopped when the
   * mouse is released.
   */
  @Override
  public void mousePressed(MouseEvent e) {
    // if (table.isEditing() && table.getCellEditor() == this)
    // isButtonColumnEditor = true;
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    // if (isButtonColumnEditor && table.isEditing())
    // table.getCellEditor().stopCellEditing();
    //
    // isButtonColumnEditor = false;
  }

  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}
}

