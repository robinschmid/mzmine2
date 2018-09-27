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

package net.sf.mzmine.parameters.parametertypes.esiadducts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.mzmine.modules.peaklistmethods.identification.adductsearch.AdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;

/**
 * Adducts parameter.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ESIAdductsParameter implements UserParameter<ESIAdductType[][], ESIAdductsComponent> {

	// Logger.
	private static final Logger LOG = Logger.getLogger(ESIAdductsParameter.class.getName());

	// XML tags.
	private static final String MODIFICTAION_TAG = "ESImodification";
	private static final String ADDUCTS_TAG = "ESIadduct";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String MASS_ATTRIBUTE = "mass_difference";
	private static final String CHARGE_ATTRIBUTE = "charge";
	private static final String MOLECULES_ATTRIBUTE = "molecules";
	private static final String SELECTED_ATTRIBUTE = "selected";

	private MultiChoiceParameter<ESIAdductType> adducts, modification;

	private ESIAdductsComponent comp;
	/**
	 * Create the parameter.
	 *
	 * @param name
	 *            name of the parameter.
	 * @param description
	 *            description of the parameter.
	 */
	public ESIAdductsParameter(final String name, final String description) { 
		super();
		adducts = new MultiChoiceParameter<ESIAdductType>(name, description, new ESIAdductType[0]);
		modification = new MultiChoiceParameter<ESIAdductType>("Modifications", "Modifications on adducts", new ESIAdductType[0]);
	}

	@Override
	public ESIAdductsComponent createEditingComponent() {
		comp = new ESIAdductsComponent(adducts.getChoices(), modification.getChoices()); 
		return comp;
	} 

	@Override
	public void loadValueFromXML(final Element xmlElement) {
		// Start with current choices and empty selections.
		final ArrayList<ESIAdductType> newChoices = new ArrayList<ESIAdductType>(
				Arrays.asList(adducts.getChoices()));
		final ArrayList<ESIAdductType> selections = new ArrayList<ESIAdductType>();
		// load all adducts
		loadAdducts(xmlElement, ADDUCTS_TAG, newChoices, selections);
		// Set choices and selections (value).
		adducts.setChoices(newChoices.toArray(new ESIAdductType[newChoices.size()]));
		adducts.setValue(selections.toArray(new ESIAdductType[selections.size()]));

		// Start with current choices and empty selections.
		final ArrayList<ESIAdductType> newChoicesMod = new ArrayList<ESIAdductType>(
				Arrays.asList(modification.getChoices()));
		final ArrayList<ESIAdductType> selectionsMod = new ArrayList<ESIAdductType>();
		// load all modification
		loadAdducts(xmlElement, MODIFICTAION_TAG, newChoicesMod, selectionsMod);
		// Set choices and selections (value).
		modification.setChoices(newChoicesMod.toArray(new ESIAdductType[newChoicesMod.size()]));
		modification.setValue(selectionsMod.toArray(new ESIAdductType[selectionsMod.size()]));
	}
	private void loadAdducts(final Element xmlElement, String TAG,
			ArrayList<ESIAdductType> newChoices, ArrayList<ESIAdductType> selections) {
		// Get the XML tag.
		final NodeList items = xmlElement.getElementsByTagName(TAG);
		final int length = items.getLength();
		// Process each adduct.
		for (int i = 0; i < length; i++) {
			final Node item = items.item(i);

			// Get attributes.
			final NamedNodeMap attributes = item.getAttributes();
			final Node nameNode = attributes.getNamedItem(NAME_ATTRIBUTE);
			final Node massNode = attributes.getNamedItem(MASS_ATTRIBUTE);
			final Node chargeNode = attributes.getNamedItem(CHARGE_ATTRIBUTE);
			final Node moleculesNode = attributes.getNamedItem(MOLECULES_ATTRIBUTE);
			final Node selectedNode = attributes.getNamedItem(SELECTED_ATTRIBUTE);

			// Valid attributes?
			if (nameNode != null && massNode != null && chargeNode!=null && moleculesNode!=null) {

				try {
					// Create new adduct.
					final ESIAdductType adduct = new ESIAdductType(
							nameNode.getNodeValue(),
							Double.parseDouble(massNode.getNodeValue()),
							Integer.parseInt(chargeNode.getNodeValue()),
							Integer.parseInt(moleculesNode.getNodeValue()));

					// A new choice?
					if (!newChoices.contains(adduct)) {
						newChoices.add(adduct);
					}

					// Selected?
					if (!selections.contains(adduct)
							&& selectedNode != null
							&& Boolean.parseBoolean(selectedNode.getNodeValue())) {

						selections.add(adduct);
					}
				} catch (NumberFormatException ex) {

					// Ignore.
					LOG.warning("Illegal mass difference attribute in "
							+ item.getNodeValue());
				}
			}
		}
	}

	/* TODO old
	private boolean isContainedIn(ArrayList<ESIAdductType> adducts, ESIAdductType na) {
		for(ESIAdductType a : adducts) {
			if(a.equals(na))
				return true;
		}
		return false;
	} 
	*/

	@Override
	public void saveValueToXML(final Element xmlElement) {

		// Get choices and selections.
		for(int i=0; i<2; i++) {
			final ESIAdductType[] choices = i==0? adducts.getChoices() : modification.getChoices();
			final ESIAdductType[] value = i==0? adducts.getValue() : modification.getValue();
			final List<ESIAdductType> selections = Arrays
					.asList(value == null ? new ESIAdductType[] {} : value);
	
			if (choices != null) {
	
				final Document parent = xmlElement.getOwnerDocument();
				for (final ESIAdductType item : choices) {
	
					final Element element = parent.createElement( i==0? ADDUCTS_TAG : MODIFICTAION_TAG);
					element.setAttribute(NAME_ATTRIBUTE, item.getRawName());
					element.setAttribute(MASS_ATTRIBUTE,
							Double.toString(item.getMassDifference()));
					element.setAttribute(CHARGE_ATTRIBUTE,
							Integer.toString(item.getCharge()));
					element.setAttribute(MOLECULES_ATTRIBUTE,
							Integer.toString(item.getMolecules()));
					element.setAttribute(SELECTED_ATTRIBUTE,
							Boolean.toString(selections.contains(item)));
					xmlElement.appendChild(element);
				}
			}
		}
	}

	@Override
	public ESIAdductsParameter cloneParameter() {
		final ESIAdductsParameter copy = new ESIAdductsParameter(getName(),
				getDescription());
		copy.setChoices(adducts.getChoices(), modification.getChoices());
		copy.setValue(getValue());
		return copy;
	}

	private void setChoices(ESIAdductType[] ad, ESIAdductType[] mods) {
		adducts.setChoices(ad);
		modification.setChoices(mods);
	}

	@Override
	public String getName() {
		return "Adducts";
	}

	@Override
	public boolean checkValue(Collection<String> errorMessages) {
		if (getValue() == null) {
		    errorMessages.add("Adducts is not set properly");
		    return false;
		} 
		return true;
	}

	@Override
	public String getDescription() { 
		return "Adducts and modifications";
	}

	@Override
	public void setValueFromComponent(ESIAdductsComponent component) {
		adducts.setValueFromComponent(component.getAdducts());
		modification.setValueFromComponent(component.getMods());
		ESIAdductType[][] choices = component.getChoices();
		adducts.setChoices(choices[0]);
		modification.setChoices(choices[1]);
		choices = component.getValue();
		adducts.setValue(choices[0]);
		modification.setValue(choices[1]);
	} 

	@Override
	public ESIAdductType[][] getValue() {
		ESIAdductType[][] ad = {adducts.getValue(), modification.getValue()};
		return ad;
	}

	@Override
	public void setValue(ESIAdductType[][] newValue) {
		adducts.setValue(newValue[0]);
		modification.setValue(newValue[1]);
	}

	@Override
	public void setValueToComponent(ESIAdductsComponent component, ESIAdductType[][] newValue) {
		component.setValue(newValue);
	}
}
