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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class ESIAdductIdentity extends SimplePeakIdentity {

  private NumberFormat netIDForm = new DecimalFormat("000");
  // identifier like [M+H]+
  private String adduct;
  private String massDifference;
  // partner rowIDs
  private String partnerRows;
  // network id (number)
  private int netID = -1;

  /**
   * Create the identity.
   *
   * @param originalPeakListRow adduct of this peak list row.
   * @param adduct type of adduct.
   */
  public ESIAdductIdentity(final PeakListRow originalPeakListRow, final ESIAdductType adduct) {
    super("later");
    this.adduct = adduct.toString(false);
    this.massDifference = adduct.getMassDiffString();
    partnerRows = String.valueOf(originalPeakListRow.getID());
    setPropertyValue(PROPERTY_METHOD, "MS annotation");
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public String getAdduct() {
    return adduct;
  }

  public String getPartnerRows() {
    return partnerRows;
  }

  public void addPartnerRow(PeakListRow row) {
    // already a partner?
    String[] split = partnerRows.split(",");
    for (String s : split)
      if (s.equals(String.valueOf(row.getID())))
        return;
    // add new partner
    partnerRows += "," + row.getID();
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public String getIDString() {
    StringBuilder b = new StringBuilder();
    if (netID != -1) {
      b.append("Net");
      b.append(getNetIDString());
      b.append(" ");
    }
    b.append(adduct);
    b.append(" indentified by ID=");
    b.append(partnerRows);
    return b.toString();
  }

  @Override
  public String toString() {
    return getIDString();
  }

  public boolean equalsAdduct(ESIAdductType acompare) {
    return acompare.toString(false).equals(this.adduct);
  }

  public int[] getPartnerRowsID() {
    String[] split = partnerRows.split(",");
    int[] ids = new int[split.length];
    for (int i = 0; i < split.length; i++)
      ids[i] = Integer.valueOf(split[i]);

    return ids;
  }

  /**
   * Network number
   * 
   * @param id
   */
  public void setNetID(int id) {
    netID = id;
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  /**
   * Network number
   * 
   * @return
   */
  public int getNetID() {
    return netID;
  }

  public String getNetIDString() {
    return netIDForm.format(getNetID());
  }
}
