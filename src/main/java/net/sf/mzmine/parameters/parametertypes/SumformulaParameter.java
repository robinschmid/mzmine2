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

import java.util.Collection;
import javax.swing.BorderFactory;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.w3c.dom.Element;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.parameters.UserParameter;

public class SumformulaParameter implements UserParameter<String, SumformulaComponent> {

  public static final double ELECTRON_MASS = 5.4857990943E-4;
  private IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
  private String name, description, value;
  private int inputsize = 20;
  private boolean valueRequired = true;

  public SumformulaParameter(String name, String description) {
    this(name, description, null);
  }

  public SumformulaParameter(String name, String description, int inputsize) {
    this.name = name;
    this.description = description;
    this.inputsize = inputsize;
  }

  public SumformulaParameter(String name, String description, String defaultValue) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
  }

  public SumformulaParameter(String name, String description, String defaultValue,
      boolean valueRequired) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.valueRequired = valueRequired;
  }


  /**
   * @see net.sf.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public SumformulaComponent createEditingComponent() {
    SumformulaComponent stringComponent = new SumformulaComponent(inputsize);
    stringComponent.setBorder(BorderFactory.createCompoundBorder(stringComponent.getBorder(),
        BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return stringComponent;
  }

  @Override
  public String getValue() {
    return getNameWithoutCharge();
  }

  public String getNameWithoutCharge() {
    if (value == null || value.isEmpty())
      return "";

    int l = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
    if (l == -1)
      return value;
    else
      return value.substring(0, l);
  }

  /**
   * Full name with charge
   * 
   * @return
   */
  public String getFullName() {
    return name;
  }



  public int getCharge() {
    if (value == null || value.isEmpty())
      return 0;
    // cutoff first -
    String value = this.value.substring(1);
    int l = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
    if (l == -1)
      return 0;
    else {
      try {
        String s = value.substring(l, value.length());
        if (s.length() == 1)
          s += "1";
        return Integer.parseInt(s);
      } catch (Exception e) {
        throw new MSDKRuntimeException("Could not set up formula. Invalid input.");
      }
    }
  }

  /**
   * Monoisotopic mass of sum formula (not mass to charge!). Mass of electrons is subtracted/added
   * 
   * @return
   */
  public double getMonoisotopicMass() throws MSDKRuntimeException {
    if (value != null && !value.isEmpty()) {
      try {
        double mz = MolecularFormulaManipulator.getMajorIsotopeMass(getFormula());
        mz -= getCharge() * ELECTRON_MASS;
        if (value.startsWith("-"))
          mz = -mz;
        return mz;
      } catch (Exception e) {
        throw e;
      }
    } else if (valueRequired)
      throw new MSDKRuntimeException("Could not set up formula. Invalid input.");
    return 0;
  }

  public IMolecularFormula getFormula() throws MSDKRuntimeException {
    if (value != null && !value.isEmpty()) {
      try {
        String formString = this.value;
        // cutoff first - (negative mz)
        if (formString.startsWith("-"))
          formString = formString.substring(1);

        //
        int l = Math.max(formString.lastIndexOf('+'), formString.lastIndexOf('-'));
        if (l == -1) {
          return MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(formString, builder);
        } else {
          String f = formString.substring(0, l);
          String charge = formString.substring(l, formString.length());
          return MolecularFormulaManipulator.getMajorIsotopeMolecularFormula("[" + f + "]" + charge,
              builder);
        }
      } catch (Exception e) {
        throw new MSDKRuntimeException("Could not set up formula. Invalid input.");
      }
    } else if (valueRequired)
      throw new MSDKRuntimeException("Could not set up formula. Invalid input.");
    return null;
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public SumformulaParameter cloneParameter() {
    SumformulaParameter copy = new SumformulaParameter(name, description);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(SumformulaComponent component) {
    value = component.getText();
  }

  @Override
  public void setValueToComponent(SumformulaComponent component, String newValue) {
    component.setText(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = xmlElement.getTextContent();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value);
  }

  public boolean isEmpty() {
    return value == null || value.isEmpty();
  }

  public boolean checkValue() {
    return checkValue(null);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired)
      return true;
    if ((value == null) || (value.trim().length() == 0)) {
      if (errorMessages != null)
        errorMessages.add(name + " is not set properly");
      return false;
    }
    try {
      getMonoisotopicMass();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

}
