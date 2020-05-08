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

import net.sf.mzmine.datamodel.Coordinates;
import net.sf.mzmine.datamodel.ImagingScan;
import net.sf.mzmine.datamodel.MergedScan;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleImagingScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;

/**
 * Implementation of the Scan interface which stores raw data points in a temporary file, accessed
 * by RawDataFileImpl.readFromFloatBufferFile()
 */
public class StorableMergedScan extends StorableScan implements ImagingScan, MergedScan {

  private Coordinates coordinates;
  private int mergedScans;
  private IntensityMergeMode intensityMode;
  private Scan best;

  /**
   * Constructor for creating a storable scan from a given scan
   */
  public StorableMergedScan(SimpleMergedScan originalScan, RawDataFileImpl rawDataFile,
      int numberOfDataPoints, int storageID) {
    super(originalScan, rawDataFile, numberOfDataPoints, storageID);
    if (SimpleImagingScan.class.isInstance(originalScan))
      this.setCoordinates(((SimpleImagingScan) originalScan).getCoordinates());
    mergedScans = originalScan.getScanCount();
    intensityMode = originalScan.getIntensityMode();
    best = originalScan.getBestScan();
  }

  /**
   * 
   * @return the xyz coordinates. null if no coordinates were specified
   */
  @Override
  public Coordinates getCoordinates() {
    return coordinates;
  }

  @Override
  public void setCoordinates(Coordinates coordinates) {
    this.coordinates = coordinates;
  }

  @Override
  public String toString() {
    return super.toString() + " merged scans: " + mergedScans + " (" + intensityMode.toString()
        + ")";
  }

  @Override
  public int getScanCount() {
    return mergedScans;
  }

  @Override
  public Scan getBestScan() {
    return best;
  }

  @Override
  public IntensityMergeMode getIntensityMode() {
    return intensityMode;
  }
}
