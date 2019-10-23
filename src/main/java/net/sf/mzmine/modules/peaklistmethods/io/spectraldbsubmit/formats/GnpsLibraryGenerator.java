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
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;

/**
 * Json for GNPS library entry submission
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsLibraryGenerator {
  private static final String tab = "\t";
  private static final NumberFormat mzForm = new DecimalFormat("0.0000");
  private static final String NA = "N/A";

  /**
   * Whole JSON entry
   * 
   * @param param
   * @param dps
   * @return
   */
  public static String generateJSON(LibrarySubmitIonParameters param, DataPoint[] dps) {
    LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();

    boolean exportRT = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getValue();

    JsonObjectBuilder json = Json.createObjectBuilder();
    // tag spectrum from mzmine2
    json.add(DBEntryField.SOFTWARE.getGnpsJsonID(), "mzmine2");
    // ion specific
    Double precursorMZ = param.getParameter(LibrarySubmitIonParameters.MZ).getValue();
    if (precursorMZ != null)
      json.add(DBEntryField.MZ.getGnpsJsonID(), precursorMZ);

    Integer charge = param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue();
    if (charge != null)
      json.add(DBEntryField.CHARGE.getGnpsJsonID(), charge);

    String adduct = param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue();
    if (adduct != null && !adduct.trim().isEmpty())
      json.add(DBEntryField.ION_TYPE.getGnpsJsonID(), adduct);

    if (exportRT) {
      Double rt =
          meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getEmbeddedParameter().getValue();
      if (rt != null)
        json.add(DBEntryField.RT.getGnpsJsonID(), rt);
    }

    // add data points array
    json.add("peaks", genJSONData(dps));

    // add meta data
    for (Parameter<?> p : meta.getParameters()) {
      if (!p.getName().equals(LibraryMetaDataParameters.EXPORT_RT.getName())) {
        String key = p.getName();
        Object value = p.getValue();
        if (value instanceof Double) {
          if (Double.compare(0d, (Double) value) == 0)
            json.add(key, 0);
          else
            json.add(key, (Double) value);
        } else if (value instanceof Float) {
          if (Float.compare(0f, (Float) value) == 0)
            json.add(key, 0);
          else
            json.add(key, (Float) value);
        } else if (value instanceof Integer)
          json.add(key, (Integer) value);
        else {
          if (value == null || (value instanceof String && ((String) value).isEmpty()))
            value = "N/A";
          json.add(key, value.toString());
        }
      }
    }

    // return Json.createObjectBuilder().add("spectrum", json.build()).build().toString();
    return json.build().toString();
  }

  /**
   * JSON of data points array
   * 
   * @param dps
   * @return
   */
  private static JsonArray genJSONData(DataPoint[] dps) {
    JsonArrayBuilder data = Json.createArrayBuilder();
    JsonArrayBuilder signal = Json.createArrayBuilder();
    for (DataPoint dp : dps) {
      // round to five digits. thats more than enough
      signal.add(((int) (dp.getMZ() * 1000000)) / 1000000.0);
      signal.add(dp.getIntensity());
      data.add(signal.build());
    }
    return data.build();
  }

  /**
   * Generate string for GNPS batch upload of library spectra. One entry = one row.
   * 
   * @param param
   * @param mgfName mgf file name
   * @param specIndex specturm index in mgf
   * @return
   */
  public static String generateBatchRow(LibrarySubmitIonParameters param, String mgfName,
      int specIndex) {
    LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();
    StringBuilder s = new StringBuilder();
    String v = "";
    s.append(mgfName);
    s.append(tab);
    s.append("*..*");
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.COMPOUND_NAME).getValue()).isEmpty() ? NA
            : v);
    s.append(tab);
    // MOLECULEMASS = exact ion m/z
    s.append(
        (v = mzForm.format(param.getParameter(LibrarySubmitIonParameters.MZ).getValue())).isEmpty()
            ? "0"
            : v);
    s.append(tab);
    s.append((v = meta.getParameter(LibraryMetaDataParameters.INSTRUMENT).getValue().toString())
        .isEmpty() ? NA : v);
    s.append(tab);
    s.append((v = meta.getParameter(LibraryMetaDataParameters.ION_SOURCE).getValue().toString())
        .isEmpty() ? NA : v);
    s.append(tab);
    // spectrum index in mgf
    s.append(String.valueOf(specIndex));
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.SMILES).getValue()).isEmpty() ? NA : v);
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.INCHI).getValue()).isEmpty() ? NA : v);
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.INCHI_AUX).getValue()).isEmpty() ? NA : v);
    s.append(tab);
    s.append(
        (v = param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue().toString()).isEmpty()
            ? "0"
            : v);
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.IONMODE).getValue().toString()).isEmpty()
            ? "Positive"
            : v);
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.PUBMED).getValue()).isEmpty() ? NA : v);
    s.append(tab);
    s.append((v = meta.getParameter(LibraryMetaDataParameters.ACQUISITION).getValue().toString())
        .isEmpty() ? "Crude" : v);
    s.append(tab);
    // exact neutral mass
    s.append((v = mzForm.format(meta.getParameter(LibraryMetaDataParameters.EXACT_MASS).getValue()))
        .isEmpty() ? "0" : v);
    s.append(tab);
    s.append(
        (v = meta.getParameter(LibraryMetaDataParameters.DATA_COLLECTOR).getValue()).isEmpty() ? NA
            : v);
    s.append(tab);
    s.append(
        (v = param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue()).isEmpty() ? NA : v);
    s.append(tab);
    // interest is always NA
    s.append(NA);
    s.append(tab);
    // quality is always 3
    s.append("3");
    s.append(tab);
    // TODO add genus species strain
    s.append(NA);
    s.append(tab);
    s.append(NA);
    s.append(tab);
    s.append(NA);
    s.append(tab);
    // cas and pi
    s.append((v = meta.getParameter(LibraryMetaDataParameters.CAS).getValue()).isEmpty() ? NA : v);
    s.append(tab);
    s.append((v = meta.getParameter(LibraryMetaDataParameters.PI).getValue()).isEmpty() ? NA : v);

    return s.toString();
  }
}
