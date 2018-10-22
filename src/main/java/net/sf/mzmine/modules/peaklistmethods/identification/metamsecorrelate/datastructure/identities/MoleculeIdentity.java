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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.apache.commons.lang3.StringUtils;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.AnnotationNetwork;

public class MoleculeIdentity extends SimplePeakIdentity {

  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat rtForm = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat netIDForm = new DecimalFormat("000");

  private AnnotationNetwork network;

  private double neutralMass;
  private double avgRT;

  /**
   * Create the identity.
   *
   * @param originalPeakListRow adduct of this peak list row.
   * @param adduct type of adduct.
   */
  public MoleculeIdentity(AnnotationNetwork network) {
    super("later");
    neutralMass = network.calcNeutralMass();
    avgRT = network.calcAvgRT();
    setPropertyValue(PROPERTY_METHOD, "MS annotation");
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public String getIDString() {
    StringBuilder b = new StringBuilder();
    if (getNetID() != -1) {
      b.append("Net");
      b.append(getNetIDString());
      b.append(" ");
    }
    b.append("mass=");
    b.append(mzForm.format(neutralMass));
    b.append(" (at ");
    b.append(rtForm.format(avgRT));
    b.append(" min) indentified by ID=");
    b.append(StringUtils.join(",", network.getAllIDs()));
    return b.toString();
  }

  @Override
  public String toString() {
    return getIDString();
  }

  /**
   * Network number
   * 
   * @param id
   */
  public void setNetwork(AnnotationNetwork net) {
    network = net;
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  /**
   * Network number
   * 
   * @return
   */
  public int getNetID() {
    return network == null ? -1 : network.getID();
  }

  public String getNetIDString() {
    return netIDForm.format(getNetID());
  }

  public AnnotationNetwork getNetwork() {
    return network;
  }

}
