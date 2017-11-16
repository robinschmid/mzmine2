package net.sf.mzmine.datamodel;

import net.sf.mzmine.datamodel.impl.ImagingParameters;

/**
 * mark data as imaging raw data
 * @author r_schm33
 *
 */
public interface ImagingRawData {

	public void setImagingParam(ImagingParameters imagingParameters);
	public ImagingParameters getImagingParam();
}
