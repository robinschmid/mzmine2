package net.sf.mzmine.project.impl;

import java.io.IOException;

import net.sf.mzmine.datamodel.impl.ImagingParameters;

public class ImagingRawDataFileImpl extends RawDataFileImpl {

	// imaging parameters
	private ImagingParameters param;
	
	public ImagingRawDataFileImpl(String dataFileName) throws IOException {
		super(dataFileName);
	}

	public void setImagingParam(ImagingParameters imagingParameters) {
		param = imagingParameters;
	}
	
	public ImagingParameters getImagingParam() {
		return param;
	}

}
