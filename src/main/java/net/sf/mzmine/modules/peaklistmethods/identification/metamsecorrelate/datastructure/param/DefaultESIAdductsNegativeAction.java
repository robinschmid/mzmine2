/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

/* Code created was by or on behalf of Syngenta and is released under the open source license in use for the
 * pre-existing code or project. Syngenta does not assert ownership or copyright any over pre-existing work.
 */

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.esiadducts.ESIAdductsComponent;

/**
 * An action to handle resetting the adducts list.
 * 
 */
public class DefaultESIAdductsNegativeAction extends AbstractAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private MultiChoiceComponent parent;
	/**
	 * Create the action.
	 */
	public DefaultESIAdductsNegativeAction(MultiChoiceComponent parent) {
		super("Reset negative");
		putValue(SHORT_DESCRIPTION, "Reset adduct choices to default set for negative mode");
		this.parent = parent;
	}
	@Override
	public void actionPerformed(final ActionEvent e) { 
		if (parent != null) {
			// Reset default choices.
			parent.setChoices(ESIAdductType.getDefaultValuesNeg());
		}
	}
}