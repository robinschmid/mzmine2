package net.sf.mzmine.modules.masslistmethods.clusterspectra;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

public class FilteredScan implements Scan {
  private final Scan parentScan;
  private @Nonnull DataPoint[] filtered;

  public FilteredScan(Scan parentScan, @Nonnull DataPoint[] filtered) {
    this.parentScan = parentScan;
    this.filtered = filtered;
  }

  public Scan getParentScan() {
    return parentScan;
  }

  public DataPoint[] getFilteredData() {
    return getDataPoints();
  }

  public DataPoint[] getOriginalData() {
    return parentScan.getDataPoints();
  }

  @Override
  @Nonnull
  public Range<Double> getDataPointMZRange() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  @Nullable
  public DataPoint getHighestDataPoint() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public double getTIC() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return parentScan.getSpectrumType();
  }

  @Override
  public int getNumberOfDataPoints() {
    return filtered.length;
  }

  @Override
  @Nonnull
  public DataPoint[] getDataPoints() {
    return filtered;
  }

  @Override
  @Nonnull
  public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  @Nonnull
  public DataPoint[] getDataPointsOverIntensity(double intensity) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  @Nonnull
  public RawDataFile getDataFile() {
    return parentScan.getDataFile();
  }

  @Override
  public int getScanNumber() {
    return parentScan.getScanNumber();
  }

  @Override
  public String getScanDefinition() {
    return parentScan.getScanDefinition();
  }

  @Override
  public int getMSLevel() {
    return parentScan.getMSLevel();
  }

  @Override
  public double getRetentionTime() {
    return parentScan.getRetentionTime();
  }

  @Override
  public @Nonnull Range<Double> getScanningMZRange() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public double getPrecursorMZ() {
    return parentScan.getPrecursorMZ();
  }

  @Override
  public @Nonnull PolarityType getPolarity() {
    return parentScan.getPolarity();
  }

  @Override
  public int getPrecursorCharge() {
    return parentScan.getPrecursorCharge();
  }

  @Override
  public int[] getFragmentScanNumbers() {
    return parentScan.getFragmentScanNumbers();
  }

  @Override
  @Nonnull
  public MassList[] getMassLists() {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  @Nullable
  public MassList getMassList(@Nonnull String name) {
    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void addMassList(@Nonnull MassList massList) {

    throw new UnsupportedOperationException("not implemented");
  }

  @Override
  public void removeMassList(@Nonnull MassList massList) {
    throw new UnsupportedOperationException("not implemented");
  }

}
