package net.sf.mzmine.datamodel.impl;

import com.google.common.collect.Range;

import net.sf.mzmine.datamodel.Coordinates;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.ImagingScan;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;

public class SimpleImagingScan extends SimpleScan implements ImagingScan {
	
	private Coordinates coordinates;

	public SimpleImagingScan(RawDataFile dataFile, int scanNumber, int msLevel, double retentionTime,
			double precursorMZ, int precursorCharge, int[] fragmentScans, DataPoint[] dataPoints,
			MassSpectrumType spectrumType, PolarityType polarity, String scanDefinition, Range<Double> scanMZRange, 
			Coordinates coordinates) {
		super(dataFile, scanNumber, msLevel, retentionTime, precursorMZ, precursorCharge, fragmentScans, dataPoints,
				spectrumType, polarity, scanDefinition, scanMZRange);
		this.setCoordinates(coordinates);
	}

	public SimpleImagingScan(Scan sc) {
		super(sc);
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
}
