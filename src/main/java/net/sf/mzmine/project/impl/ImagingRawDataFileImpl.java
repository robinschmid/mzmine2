package net.sf.mzmine.project.impl;

import java.io.IOException;

import net.sf.mzmine.datamodel.ImagingRawData;
import net.sf.mzmine.datamodel.impl.ImagingParameters;

public class ImagingRawDataFileImpl extends RawDataFileImpl implements ImagingRawData {

	// imaging parameters
	private ImagingParameters param;
	
	public ImagingRawDataFileImpl(String dataFileName) throws IOException {
		super(dataFileName);
	}

	@Override
	public void setImagingParam(ImagingParameters imagingParameters) {
		param = imagingParameters;
	}

	@Override
	public ImagingParameters getImagingParam() {
		return param;
	}

}
