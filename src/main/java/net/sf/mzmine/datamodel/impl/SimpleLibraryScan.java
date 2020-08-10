package net.sf.mzmine.datamodel.impl;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.LibraryScan;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;

public class SimpleLibraryScan extends SimpleScan implements LibraryScan {
  private final SpectralDBEntry entry;

  private SimpleLibraryScan(SpectralDBEntry e, int scanNumber, int msLevel, double retentionTime,
      double precursorMZ, int precursorCharge, int fragmentScans[], DataPoint[] dataPoints,
      MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition,
      Range<Double> scanMZRange) {
    super(null, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, fragmentScans,
        dataPoints, spectrumType, polarity, scanDefinition, scanMZRange);
    this.entry = e;
  }


  public static SimpleLibraryScan create(int scanNumber, SpectralDBEntry e) {
    String scanId = scanNumber + ", "
        + e.getField(DBEntryField.NAME).orElse(e.getField(DBEntryField.COMMENT).orElse(""));

    // Extract scan data
    int msLevel = (Integer) e.getField(DBEntryField.MS_LEVEL).orElse(0);
    double retentionTime = (Double) e.getField(DBEntryField.RT).orElse(0d);
    PolarityType polarity = e.getPolarity();
    double precursorMz = e.getPrecursorMZ() != null ? e.getPrecursorMZ() : 0d;
    int precursorCharge = (Integer) e.getField(DBEntryField.CHARGE).orElse(0);
    String scanDefinition = e.getField(DBEntryField.NAME).orElse("") + ";"
        + e.getField(DBEntryField.COMMENT).orElse("").toString();
    DataPoint dataPoints[] = e.getDataPoints();
    MassSpectrumType spectrumType = MassSpectrumType.CENTROIDED;

    return new SimpleLibraryScan(e, scanNumber, msLevel, retentionTime, precursorMz,
        precursorCharge, null, dataPoints, spectrumType, polarity, scanDefinition, null);
  }

  public SpectralDBEntry getEntry() {
    return entry;
  }
}
