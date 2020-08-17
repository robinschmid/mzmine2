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

package net.sf.mzmine.parameters.parametertypes;

import java.awt.event.ActionListener;
import java.util.Collection;
import javax.swing.BorderFactory;
import org.w3c.dom.Element;
import net.sf.mzmine.parameters.UserParameter;

/**
 * Number parameter. Note that we prefer to use JTextField rather than JFormattedTextField, because
 * JFormattedTextField sometimes has odd behavior. For example, value reported by getValue() may be
 * different than value actually typed in the text box, because it has not been committed yet. Also,
 * when formatter is set to 1 decimal digit, it becomes impossible to enter 2 decimals etc.
 */
public class ButtonParameter implements UserParameter<String, ButtonComponent> {

  // Text field width.
  private static final int WIDTH = 100;

  private final String name;
  private final String description;

  private String[] labels;
  private ActionListener[] listener;

  public ButtonParameter(final String aName, final String aDescription, String[] labels,
      ActionListener[] listener) {
    name = aName;
    description = aDescription;
    this.labels = labels;
    this.listener = listener;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ButtonComponent createEditingComponent() {

    ButtonComponent doubleComponent = new ButtonComponent(labels, listener);
    doubleComponent.setBorder(BorderFactory.createCompoundBorder(doubleComponent.getBorder(),
        BorderFactory.createEmptyBorder(0, 3, 0, 0)));
    return doubleComponent;
  }

  @Override
  public void setValueFromComponent(final ButtonComponent component) {}

  @Override
  public void setValue(final String newValue) {}

  @Override
  public ButtonParameter cloneParameter() {
    return new ButtonParameter(name, description, labels, listener);
  }

  @Override
  public void setValueToComponent(final ButtonComponent component, final String newValue) {}

  @Override
  public String getValue() {
    return "";
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {}

  @Override
  public void saveValueToXML(final Element xmlElement) {}

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    return true;
  }

}
