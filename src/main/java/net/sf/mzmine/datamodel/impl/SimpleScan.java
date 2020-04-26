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

package net.sf.mzmine.datamodel.impl;

import java.util.TreeSet;
import java.util.Vector;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.util.scans.ScanUtils;

/**
 * Simple implementation of the Scan interface.
 */
public class SimpleScan implements Scan {

  private RawDataFile dataFile;
  private int scanNumber;
  private int msLevel;
  private int fragmentScans[];
  private DataPoint dataPoints[];
  private double precursorMZ;
  private int precursorCharge;
  private double retentionTime;
  private Range<Double> mzRange;
  private DataPoint basePeak;
  private double totalIonCurrent;
  private MassSpectrumType spectrumType;
  private PolarityType polarity;
  private String scanDefinition;
  private Range<Double> scanMZRange;

  /**
   * Clone constructor
   */
  public SimpleScan(Scan sc) {
    this(sc.getDataFile(), sc.getScanNumber(), sc.getMSLevel(), sc.getRetentionTime(),
        sc.getPrecursorMZ(), sc.getPrecursorCharge(), sc.getFragmentScanNumbers(),
        sc.getDataPoints(), sc.getSpectrumType(), sc.getPolarity(), sc.getScanDefinition(),
        sc.getScanningMZRange());
  }

  /**
   * Constructor for creating scan with given data
   */
  public SimpleScan(RawDataFile dataFile, int scanNumber, int msLevel, double retentionTime,
      double precursorMZ, int precursorCharge, int fragmentScans[], DataPoint[] dataPoints,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {

    // save scan data
    this.dataFile = dataFile;
    this.scanNumber = scanNumber;
    this.msLevel = msLevel;
    this.retentionTime = retentionTime;
    this.precursorMZ = precursorMZ;
    this.fragmentScans = fragmentScans;
    this.spectrumType = spectrumType;
    this.precursorCharge = precursorCharge;
    this.polarity = polarity;
    this.scanDefinition = scanDefinition;
    this.scanMZRange = scanMZRange;

    if (dataPoints != null)
      setDataPoints(dataPoints);
  }

  /**
   * @return Returns scan datapoints
   */
  @Override
  public @Nonnull DataPoint[] getDataPoints() {
    return dataPoints;
  }

  /**
   * @return Returns scan datapoints within a given range
   */
  @Override
  public @Nonnull DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {

    int startIndex, endIndex;
    for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
      if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint())
        break;
    }

    for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
      if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint())
        break;
    }

    DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

    // Copy the relevant points
    System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex - startIndex);

    return pointsWithinRange;
  }

  /**
   * @return Returns scan datapoints over certain intensity
   */
  @Override
  public @Nonnull DataPoint[] getDataPointsOverIntensity(double intensity) {
    int index;
    Vector<DataPoint> points = new Vector<DataPoint>();

    for (index = 0; index < dataPoints.length; index++) {
      if (dataPoints[index].getIntensity() >= intensity)
        points.add(dataPoints[index]);
    }

    DataPoint pointsOverIntensity[] = points.toArray(new DataPoint[0]);

    return pointsOverIntensity;
  }

  /**
   * @param mzValues m/z values to set
   * @param intensityValues Intensity values to set
   */
  public void setDataPoints(DataPoint[] dataPoints) {

    this.dataPoints = dataPoints;
    mzRange = Range.singleton(0.0);
    basePeak = null;
    totalIonCurrent = 0;

    // find m/z range and base peak
    if (dataPoints.length > 0) {

      basePeak = dataPoints[0];
      mzRange = Range.singleton(dataPoints[0].getMZ());

      for (DataPoint dp : dataPoints) {

        if (dp.getIntensity() > basePeak.getIntensity())
          basePeak = dp;

        mzRange = mzRange.span(Range.singleton(dp.getMZ()));
        totalIonCurrent += dp.getIntensity();

      }

    }

  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getNumberOfDataPoints()
   */
  @Override
  public int getNumberOfDataPoints() {
    return dataPoints.length;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getScanNumber()
   */
  @Override
  public int getScanNumber() {
    return scanNumber;
  }

  /**
   * @param scanNumber The scanNumber to set.
   */
  public void setScanNumber(int scanNumber) {
    this.scanNumber = scanNumber;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getMSLevel()
   */
  @Override
  public int getMSLevel() {
    return msLevel;
  }

  /**
   * @param msLevel The msLevel to set.
   */
  public void setMSLevel(int msLevel) {
    this.msLevel = msLevel;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getPrecursorMZ()
   */
  @Override
  public double getPrecursorMZ() {
    return precursorMZ;
  }

  /**
   * @param precursorMZ The precursorMZ to set.
   */
  public void setPrecursorMZ(double precursorMZ) {
    this.precursorMZ = precursorMZ;
  }

  /**
   * @return Returns the precursorCharge.
   */
  @Override
  public int getPrecursorCharge() {
    return precursorCharge;
  }

  /**
   * @param precursorCharge The precursorCharge to set.
   */
  public void setPrecursorCharge(int precursorCharge) {
    this.precursorCharge = precursorCharge;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getScanAcquisitionTime()
   */
  @Override
  public double getRetentionTime() {
    return retentionTime;
  }

  /**
   * @param retentionTime The retentionTime to set.
   */
  public void setRetentionTime(double retentionTime) {
    this.retentionTime = retentionTime;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getMZRangeMax()
   */
  @Override
  public @Nonnull Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getBasePeakMZ()
   */
  @Override
  public DataPoint getHighestDataPoint() {
    return basePeak;
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getFragmentScanNumbers()
   */
  @Override
  public int[] getFragmentScanNumbers() {
    return fragmentScans;
  }

  /**
   * @param fragmentScans The fragmentScans to set.
   */
  public void setFragmentScanNumbers(int[] fragmentScans) {
    this.fragmentScans = fragmentScans;
  }

  public void addFragmentScan(int fragmentScan) {
    TreeSet<Integer> fragmentsSet = new TreeSet<Integer>();
    if (fragmentScans != null) {
      for (int frag : fragmentScans)
        fragmentsSet.add(frag);
    }
    fragmentsSet.add(fragmentScan);
    fragmentScans = Ints.toArray(fragmentsSet);
  }

  /**
   * @see net.sf.mzmine.datamodel.Scan#getSpectrumType()
   */
  @Override
  public MassSpectrumType getSpectrumType() {
    return spectrumType;
  }

  /**
   * @param centroided The centroided to set.
   */
  public void setSpectrumType(MassSpectrumType spectrumType) {
    this.spectrumType = spectrumType;
  }

  @Override
  public double getTIC() {
    return totalIonCurrent;
  }

  @Override
  public String toString() {
    return ScanUtils.scanToString(this, false);
  }

  @Override
  public @Nonnull RawDataFile getDataFile() {
    return dataFile;
  }

  @Override
  public synchronized void addMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException();
  }


  @Override
  public synchronized void removeMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nonnull MassList[] getMassLists() {
    throw new UnsupportedOperationException();
  }

  @Override
  public MassList getMassList(@Nonnull String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @Nonnull PolarityType getPolarity() {
    if (polarity == null)
      polarity = PolarityType.UNKNOWN;
    return polarity;
  }

  @Override
  public String getScanDefinition() {
    if (scanDefinition == null)
      scanDefinition = "";
    return scanDefinition;
  }

  @Override
  public @Nonnull Range<Double> getScanningMZRange() {
    if (scanMZRange == null)
      scanMZRange = getDataPointMZRange();
    return scanMZRange;
  }
}
