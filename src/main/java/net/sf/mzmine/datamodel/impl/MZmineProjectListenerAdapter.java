package net.sf.mzmine.datamodel.impl;

import net.sf.mzmine.datamodel.MZmineProjectListener;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;

public abstract class MZmineProjectListenerAdapter implements MZmineProjectListener {

  public static enum Operation {
    ADDED, REMOVED, NOT_SPECIFIED;
  }

  @Override
  public void dataFileAdded(RawDataFile raw) {
    dataFilesChanged(raw, Operation.ADDED);
  }

  @Override
  public void peakListAdded(PeakList pkl) {
    peakListsChanged(pkl, Operation.ADDED);
  }

  @Override
  public void dataFileRemoved(RawDataFile raw) {
    dataFilesChanged(raw, Operation.REMOVED);
  }

  @Override
  public void peakListRemoved(PeakList pkl) {
    peakListsChanged(pkl, Operation.REMOVED);
  }

  public abstract void peakListsChanged(PeakList pkl, Operation op);

  public abstract void dataFilesChanged(RawDataFile raw, Operation op);
}
