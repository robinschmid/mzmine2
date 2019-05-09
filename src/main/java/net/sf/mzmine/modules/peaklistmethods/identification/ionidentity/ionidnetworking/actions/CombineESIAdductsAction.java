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

package net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.actions;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.AbstractAction;
import net.sf.mzmine.datamodel.identities.iontype.IonModification;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;

/**
 * An action to add custom adducts.
 *
 */
public class CombineESIAdductsAction extends AbstractAction {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private MultiChoiceComponent parent;

  /**
   * Create the action.
   */
  public CombineESIAdductsAction(MultiChoiceComponent parent) {
    super("Combine...");
    this.parent = parent;
    putValue(SHORT_DESCRIPTION, "Combine adducts to a new custom adduct to the set of choices");
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (parent != null) {
      // Show dialog.
      CombineIonModificationDialog dialog =
          new CombineIonModificationDialog((IonModification[]) parent.getChoices());

      dialog.setVisible(true);
      List<IonModification> add = dialog.getNewTypes();
      if (!add.isEmpty())
        addAll(add);
    }
  }

  private void addAll(List<IonModification> add) {
    // Add to list of choices (if not already present).
    List<IonModification> choices =
        new ArrayList<IonModification>(Arrays.asList((IonModification[]) parent.getChoices()));

    add.stream().filter(a -> !choices.contains(a)).forEach(a -> choices.add(a));

    parent.setChoices(choices.toArray(new IonModification[choices.size()]));
  }

}
