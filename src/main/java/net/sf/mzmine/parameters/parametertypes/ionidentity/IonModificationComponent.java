/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package net.sf.mzmine.parameters.parametertypes.ionidentity;

import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.sf.mzmine.datamodel.identities.iontype.IonModification;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.AddIonModificationAction;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.CombineESIAdductsAction;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.DefaultAdductsNegativeAction;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.DefaultIonModificationAction;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.DefaultNeutralModificationAction;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.ExportIonModificationAction;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions.ImportIonModificationAction;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.util.components.GridBagPanel;

/**
 * A component for selecting adducts.
 *
 */
public class IonModificationComponent extends GridBagPanel {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  protected MultiChoiceComponent adducts, mods;

  /**
   * Create the component.
   *
   * @param choicesAdducts the adduct choices.
   */
  public IonModificationComponent(IonModification[] choicesAdducts, IonModification[] choicesMods) {
    adducts = new MultiChoiceComponent(choicesAdducts);
    // add top label
    adducts.add(new JLabel("Adducts"), BorderLayout.NORTH);
    // add buttons
    adducts.addButton(new JButton(new AddIonModificationAction(adducts)));
    adducts.addButton(new JButton(new CombineESIAdductsAction(adducts)));
    adducts.addButton(new JButton(new ImportIonModificationAction(adducts)));
    adducts.addButton(new JButton(new ExportIonModificationAction(adducts)));
    adducts.addButton(new JButton(new DefaultIonModificationAction(adducts)));
    adducts.addButton(new JButton(new DefaultAdductsNegativeAction(adducts)));
    add(adducts, 0, 0);

    mods = new MultiChoiceComponent(choicesMods);
    // add top label
    mods.add(new JLabel("Modifications"), BorderLayout.NORTH);
    // add buttons
    mods.addButton(new JButton(new AddIonModificationAction(mods)));
    mods.addButton(new JButton(new CombineESIAdductsAction(mods)));
    mods.addButton(new JButton(new ImportIonModificationAction(mods)));
    mods.addButton(new JButton(new ExportIonModificationAction(mods)));
    mods.addButton(new JButton(new DefaultNeutralModificationAction(mods)));
    add(mods, 1, 0);
  }

  public IonModification[][] getChoices() {
    IonModification[] ad = (IonModification[]) adducts.getChoices();
    IonModification[] md = (IonModification[]) mods.getChoices();
    IonModification[][] all = {ad, md};
    return all;
  }

  /**
   * Get the users selections.
   *
   * @return the selected choices.
   */
  public IonModification[][] getValue() {
    Object[] ad = adducts.getValue();
    Object[] md = mods.getValue();
    IonModification[][] all = {Arrays.copyOf(ad, ad.length, IonModification[].class),
        Arrays.copyOf(md, md.length, IonModification[].class)};
    return all;
  }

  public void setValue(final IonModification[][] values) {
    if (values != null) {
      if (values[0] != null)
        adducts.setValue(values[0]);
      if (values[1] != null)
        mods.setValue(values[1]);
    }
  }

  public MultiChoiceComponent getMods() {
    return mods;
  }

  public MultiChoiceComponent getAdducts() {
    return adducts;
  }
}
