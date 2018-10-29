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

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import org.w3c.dom.Element;
import net.sf.mzmine.parameters.UserParameter;

/**
 * Button Parameter: is an empty convenience parameter. It holds no value. It generates a button
 * with an action consumer
 */
public class ButtonParameter implements UserParameter<Consumer<ActionEvent>, ButtonComponent> {

  // Text field width.
  private static final int WIDTH = 100;

  private final String name, description;

  private Consumer<ActionEvent> consumer;

  public ButtonParameter(final String aName, final String aDescription,
      Consumer<ActionEvent> consumer) {
    name = aName;
    description = aDescription;
    setValue(consumer);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ButtonComponent createEditingComponent() {
    ButtonComponent integerComponent = new ButtonComponent(name, consumer);
    integerComponent.setBorder(BorderFactory.createCompoundBorder(integerComponent.getBorder(),
        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return integerComponent;
  }

  @Override
  public void setValueFromComponent(final ButtonComponent component) {}

  @Override
  public void setValue(final Consumer<ActionEvent> newValue) {
    consumer = newValue;
  }

  @Override
  public ButtonParameter cloneParameter() {
    return new ButtonParameter(name, description, consumer);
  }

  @Override
  public void setValueToComponent(final ButtonComponent component,
      final Consumer<ActionEvent> newValue) {
    component.setConsumer(newValue);
  }

  @Override
  public Consumer<ActionEvent> getValue() {
    return consumer;
  }

  @Override
  public void loadValueFromXML(final Element xmlElement) {}

  @Override
  public void saveValueToXML(final Element xmlElement) {}

  @Override
  public boolean checkValue(final Collection<String> errorMessages) {
    return true;
  }
}
