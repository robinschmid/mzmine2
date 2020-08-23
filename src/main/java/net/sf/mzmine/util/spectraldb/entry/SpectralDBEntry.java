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

package net.sf.mzmine.util.spectraldb.entry;

import java.util.Map;
import java.util.Optional;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.PolarityType;

public class SpectralDBEntry {

  private final Map<DBEntryField, Object> fields;
  private final DataPoint[] dps;

  public SpectralDBEntry(Map<DBEntryField, Object> fields, DataPoint[] dps) {
    this.fields = fields;
    this.dps = dps;
  }

  public Double getPrecursorMZ() {
    return (Double) fields.get(DBEntryField.MZ);
  }

  public Optional<Object> getField(DBEntryField f) {
    return Optional.ofNullable(fields.get(f));
  }

  public DataPoint[] getDataPoints() {
    return dps;
  }

  public PolarityType getPolarity() {
    String polarity = getField(DBEntryField.ION_MODE).orElse("").toString().toLowerCase();
    if (polarity.contains("+") || polarity.contains("pos"))
      return PolarityType.POSITIVE;
    if (polarity.contains("-") || polarity.contains("neg"))
      return PolarityType.NEGATIVE;
    if (polarity.contains("p"))
      return PolarityType.POSITIVE;
    if (polarity.contains("n"))
      return PolarityType.NEGATIVE;
    return PolarityType.UNKNOWN;
  }

  public void setField(DBEntryField field, Object value) {
    fields.put(field, value);
  }

  public Map<DBEntryField, Object> getFields() {
    return fields;
  }
}
