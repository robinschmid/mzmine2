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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.actions;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.AbstractAction;
import net.sf.mzmine.framework.listener.DelayedDocumentListener;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductType;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.SumformulaComponent;
import net.sf.mzmine.parameters.parametertypes.SumformulaParameter;
import net.sf.mzmine.util.ExitCode;

/**
 * An action to add custom adducts.
 *
 */
public class AddESIAdductsAction extends AbstractAction {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private MultiChoiceComponent parent;

  /**
   * Create the action.
   */
  public AddESIAdductsAction(MultiChoiceComponent parent) {
    super("Add...");
    this.parent = parent;
    putValue(SHORT_DESCRIPTION, "Add a custom adduct to the set of choices");
  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    if (parent != null) {

      // Show dialog.
      final ParameterSet parameters = new AddESIAdductParameters();
      if (parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(),
          true) == ExitCode.OK) {

        //
        int charge = 0;
        Double mz = null;
        String name = parameters.getParameter(AddESIAdductParameters.NAME).getValue();
        // Create new adduct.
        SumformulaParameter form = parameters.getParameter(AddESIAdductParameters.FORMULA);
        if (form.checkValue() && !form.isEmpty()) {
          if (name.isEmpty()) {
            name = form.getValue();
          }
          double test = form.getMonoisotopicMass();
          if (test != 0) {
            mz = test;
            charge = form.getCharge();
          }
        }
        if (mz == null) {
          mz = parameters.getParameter(AddESIAdductParameters.MASS_DIFFERENCE).getValue();
          charge = parameters.getParameter(AddESIAdductParameters.CHARGE).getValue();
        }

        final ESIAdductType adduct = new ESIAdductType(name, mz, charge);

        // Add to list of choices (if not already present).
        final Collection<ESIAdductType> choices =
            new ArrayList<ESIAdductType>(Arrays.asList((ESIAdductType[]) parent.getChoices()));
        if (!choices.contains(adduct)) {

          choices.add(adduct);
          parent.setChoices(choices.toArray(new ESIAdductType[choices.size()]));
        }
      }
    }
  }

  /**
   * Represents an adduct.
   */
  private static class AddESIAdductParameters extends SimpleParameterSet {

    // Adduct name.
    private static final StringParameter NAME =
        new StringParameter("Name", "A name to identify the new adduct.", "", false);

    // Sum formula
    private static final SumformulaParameter FORMULA = new SumformulaParameter("Sum formula",
        "Put - infront of the sum formula if this is a neutral loss. Will override mass difference and charge. Is used as name if name is empty.",
        "", false);

    // Adduct mass difference.
    private static final DoubleParameter MASS_DIFFERENCE = new DoubleParameter("Mass difference",
        "Mass difference for the new adduct", MZmineCore.getConfiguration().getMZFormat(), 0d);

    private static final IntegerParameter CHARGE =
        new IntegerParameter("Charge", "Charge of adduct", 1, false);

    private AddESIAdductParameters() {
      super(new Parameter[] {NAME, FORMULA, MASS_DIFFERENCE, CHARGE});
    }

    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
      ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);

      // enable
      SumformulaComponent com = (SumformulaComponent) dialog.getComponentForParameter(FORMULA);
      com.getTextField().getDocument().addDocumentListener(new DelayedDocumentListener(100, e -> {
        dialog.getComponentForParameter(MASS_DIFFERENCE)
            .setEnabled(e.getDocument().getLength() == 0);
        dialog.getComponentForParameter(CHARGE).setEnabled(e.getDocument().getLength() == 0);
      }));

      dialog.setVisible(true);
      return dialog.getExitCode();
    }

  }
}
