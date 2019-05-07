package net.sf.mzmine.datamodel;

import java.util.ArrayList;

import net.sf.mzmine.datamodel.impl.ImagingParameters;

/**
 * mark data as imaging raw data
 * @author r_schm33
 *
 */
public interface ImagingRawData {

	public void setImagingParam(ImagingParameters imagingParameters);
	public ImagingParameters getImagingParam();
	
	public Scan getScan(float x, float y);
	
	/**
	 * all scans in this area
	 * @param x
	 * @param y
	 * @param x2 inclusive
	 * @param y2 inclusive
	 * @return
	 */
	public ArrayList<Scan> getScans(float x, float y, float x2, float y2);
	
}
