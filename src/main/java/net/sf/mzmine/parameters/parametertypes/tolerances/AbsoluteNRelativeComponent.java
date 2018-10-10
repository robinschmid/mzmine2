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

package net.sf.mzmine.parameters.parametertypes.tolerances;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AbsoluteNRelativeComponent extends JPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private final JTextField absField, relField;

  public AbsoluteNRelativeComponent() {

    setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));

    add(new JLabel("abs="));

    absField = new JTextField();
    absField.setColumns(8);
    add(absField);


    add(new JLabel("rel="));

    relField = new JTextField();
    relField.setColumns(5);
    add(relField);
    add(new JLabel("%"));
  }

  public void setValue(AbsoluteNRelative value) {
    absField.setText(String.valueOf(value.getAbsolute()));
    relField.setText(String.valueOf(value.getRelative() * 100.0));
  }

  public AbsoluteNRelative getValue() {
    try {
      double abs = Double.parseDouble(absField.getText().trim());
      double rel = Double.parseDouble(relField.getText().trim()) / 100.0;
      AbsoluteNRelative value = new AbsoluteNRelative(abs, rel);
      return value;
    } catch (NumberFormatException e) {
      return null;
    }

  }

  @Override
  public void setToolTipText(String toolTip) {
    absField.setToolTipText(toolTip);
    relField.setToolTipText(toolTip);
  }

}
