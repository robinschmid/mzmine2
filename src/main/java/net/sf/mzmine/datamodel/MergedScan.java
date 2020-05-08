package net.sf.mzmine.datamodel;

import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;

public interface MergedScan {

  public int getScanCount();

  public Scan getBestScan();

  public IntensityMergeMode getIntensityMode();

}
