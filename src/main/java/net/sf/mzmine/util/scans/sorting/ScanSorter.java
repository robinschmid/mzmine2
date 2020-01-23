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

package net.sf.mzmine.util.scans.sorting;

import java.util.Comparator;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.scans.ScanUtils;

/**
 * Scan needs at least one MassList
 * 
 * @author Robin Schmid
 *
 */
public class ScanSorter implements Comparator<Scan> {

  private final String massListName;
  private final MassListSorter comp;

  /**
   * Scans need at least one MassList (define name or use empty/null massListName for first
   * MassList)
   * 
   * @param massListName any name or null/empty to use the first MassList of each scan
   * @param noiseLevel
   * @param sort sorting mode
   */
  public ScanSorter(String massListName, double noiseLevel, ScanSortMode sortMode) {
    this.massListName = massListName;
    comp = new MassListSorter(noiseLevel, sortMode);
  }

  @Override
  public int compare(Scan a, Scan b) {
    MassList ma = ScanUtils.getMassListOrFirst(a, massListName);
    MassList mb = ScanUtils.getMassListOrFirst(b, massListName);
    if (ma == null || mb == null)
      throw new RuntimeException(new MissingMassListException(massListName));
    return comp.compare(ma.getDataPoints(), mb.getDataPoints());
  }


}
