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
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.AnnotationNetwork;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.MSMSIdentityList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.MSMSIonRelationIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.MSMSIonRelationIdentity.Relation;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.MSMSMultimerIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSIdentity;

public class ESIAdductIdentity extends SimplePeakIdentity {

  private NumberFormat netIDForm = new DecimalFormat("#000");

  private ESIAdductType a;
  // identifier like [M+H]+
  private String adduct;
  private String massDifference;
  // partner rowIDs
  private ConcurrentHashMap<PeakListRow, ESIAdductIdentity> partner = new ConcurrentHashMap<>();
  // network id (number)
  private AnnotationNetwork network;

  /**
   * List of MSMS identities. e.g., multimers/monomers that were found in MS/MS data
   */
  private MSMSIdentityList msmsIdent;

  private boolean isDeleted;

  /**
   * Create the identity.
   *
   * @param originalPeakListRow adduct of this peak list row.
   * @param adduct type of adduct.
   */
  public ESIAdductIdentity(ESIAdductType adduct) {
    super("later");
    a = adduct;
    this.adduct = adduct.toString(false);
    this.massDifference = adduct.getMassDiffString();
    setPropertyValue(PROPERTY_METHOD, "MS annotation");
    setPropertyValue(PROPERTY_NAME, getIDString());
  }


  /**
   * Adds new identities or just adds the rows to identities as links
   * 
   * @param row1 row to add the identity to
   * @param row2 identified by this row
   */
  public static void addAdductIdentityToRow(PeakListRow row1, ESIAdductType row1ID,
      PeakListRow row2, ESIAdductType row2ID) {
    ESIAdductIdentity a = getAdductEqualIdentity(row1, row1ID);
    ESIAdductIdentity b = getAdductEqualIdentity(row2, row2ID);

    // create new
    if (a == null) {
      a = new ESIAdductIdentity(row1ID);
      row1.addPeakIdentity(a, false);
    }
    if (b == null) {
      b = new ESIAdductIdentity(row2ID);
      row2.addPeakIdentity(b, false);
    }
    a.addPartnerRow(row2, b);
    b.addPartnerRow(row1, a);
  }

  /**
   * Find equal identity that was already added
   * 
   * @param row
   * @param adduct
   * @return equal identity or null
   */
  public static ESIAdductIdentity getAdductEqualIdentity(PeakListRow row, ESIAdductType adduct) {
    // is old?
    for (PeakIdentity id : row.getPeakIdentities()) {
      if (ESIAdductIdentity.class.isInstance(id)) {
        ESIAdductIdentity a = (ESIAdductIdentity) id;
        // equals? add row2 to partners
        if (a.equalsAdduct(adduct)) {
          return a;
        }
      }
    }
    return null;
  }

  /**
   * Get adduct type
   * 
   * @return
   */
  public ESIAdductType getA() {
    return a;
  }

  public String getAdduct() {
    return adduct;
  }

  /**
   * Comma separated
   * 
   * @return
   */
  public String getPartnerRows() {
    return getPartnerRows(",");
  }

  /**
   * 
   * @param delimiter
   * @return
   */
  public String getPartnerRows(String delimiter) {
    return partner.keySet().stream().map(PeakListRow::getID).map(String::valueOf)
        .collect(Collectors.joining(delimiter));
  }

  public void addPartnerRow(PeakListRow row, ESIAdductIdentity pid) {
    partner.put(row, pid);
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public void resetLinks() {
    partner.clear();
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public String getIDString() {
    StringBuilder b = new StringBuilder();
    if (getNetID() != -1) {
      b.append("Net");
      b.append(getNetIDString());
      b.append(" ");
    }
    b.append(adduct);
    b.append(" identified by ID=");
    b.append(getPartnerRows());

    // MSMS backed id for multimers
    if (getMSMSMultimerCount() > 0) {
      b.append(" (MS/MS:xmer)");
    }
    // MSMS backed id for insource frag
    if (getA().getModCount() > 0) {
      if (getMSMSModVerify() > 0) {
        b.append(" (MS/MS:insource frag)");
      }
    }
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
    if (partner.isEmpty())
      return new int[0];

    return partner.keySet().stream().mapToInt(PeakListRow::getID).toArray();
  }

  public ConcurrentHashMap<PeakListRow, ESIAdductIdentity> getPartner() {
    return partner;
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

  /**
   * 
   * @param row
   * @param link
   * @return The identity of row, determined by link or null if there is no connection
   */
  public static ESIAdductIdentity getIdentityOf(PeakListRow row, PeakListRow link) {
    for (PeakIdentity pi : row.getPeakIdentities()) {
      // identity by ms annotation module
      if (pi instanceof ESIAdductIdentity) {
        ESIAdductIdentity adduct = (ESIAdductIdentity) pi;
        if (adduct.hasPartnerID(link.getID()))
          return adduct;
      }
    }
    return null;
  }

  /**
   * Checks whether partner ids contain a certain id
   * 
   * @param id
   * @return
   */
  public boolean hasPartnerID(int id) {
    return Arrays.stream(getPartnerRowsID()).anyMatch(pid -> pid == id);
  }

  public void setMSMSIdentities(MSMSIdentityList msmsIdent) {
    this.msmsIdent = msmsIdent;
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public void addMSMSIdentity(AbstractMSMSIdentity ident) {
    if (this.msmsIdent == null)
      msmsIdent = new MSMSIdentityList();
    msmsIdent.add(ident);
    setPropertyValue(PROPERTY_NAME, getIDString());
  }

  public MSMSIdentityList getMSMSIdentities() {
    return msmsIdent;
  }

  /**
   * Count of signals that verify this multimer identity
   * 
   * @return
   */
  public int getMSMSMultimerCount() {
    if (msmsIdent == null || msmsIdent.isEmpty())
      return 0;

    return (int) msmsIdent.stream().filter(id -> id instanceof MSMSMultimerIdentity).count();
  }

  public int getMSMSModVerify() {
    if (msmsIdent == null || msmsIdent.isEmpty())
      return 0;

    return (int) msmsIdent.stream().filter(id -> id instanceof MSMSIonRelationIdentity
        && ((MSMSIonRelationIdentity) id).getRelation().equals(Relation.NEUTRAL_LOSS)).count();
  }

  public AnnotationNetwork getNetwork() {
    return network;
  }

  /**
   * deletes from network
   */
  public void delete(PeakListRow row) {
    if (isDeleted())
      return;
    setDeleted(true);
    if (network != null) {
      network.remove(row);
    }
    row.removePeakIdentity(this);
    // remove from partners
    partner.entrySet().stream().forEach(e -> e.getValue().delete(e.getKey()));
  }


  public void setDeleted(boolean state) {
    isDeleted = state;
  }

  public boolean isDeleted() {
    return isDeleted;
  }

  /**
   * Score is the network size plus MSMS verifiers
   * 
   * @return
   */
  public int getScore() {
    if (network == null)
      return partner.size();
    return network.size() + (getMSMSMultimerCount() > 0 ? 1 : 0) + (getMSMSModVerify() > 0 ? 1 : 0);
  }

}
