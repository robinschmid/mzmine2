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

package net.sf.mzmine.project.impl;

import net.sf.mzmine.datamodel.LibraryScan;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleLibraryScan;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

/**
 * Implementation of the Scan interface which stores raw data points in a temporary file, accessed
 * by RawDataFileImpl.readFromFloatBufferFile()
 */
public class StorableLibraryScan extends StorableScan implements LibraryScan {

  private SpectralDBEntry entry;

  /**
   * Constructor for creating a storable scan from a given scan
   */
  public StorableLibraryScan(Scan originalScan, RawDataFileImpl rawDataFile, int numberOfDataPoints,
      int storageID) {
    super(originalScan, rawDataFile, numberOfDataPoints, storageID);
    if (SimpleLibraryScan.class.isInstance(originalScan))
      entry = (((SimpleLibraryScan) originalScan).getEntry());
  }

  @Override
  public SpectralDBEntry getEntry() {
    return entry;
  }

  @Override
  public String toString() {
    if (entry == null)
      return super.toString();
    String s = super.toString() + ", " + entry.getField(DBEntryField.NAME).orElse("") + ", "
        + entry.getField(DBEntryField.COMMENT).orElse("");
    if (getMSLevel() > 1)
      s += ", " + entry.getField(DBEntryField.ION_TYPE).orElse("");
    return s;
  }
}
