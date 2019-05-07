package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;

public class RowGroup extends ArrayList<PeakListRow> {
  // visualization
  private int lastViewedRow = 0;
  private int lastViewedRawFile = 0;

  // running index of groups
  protected int groupID = 0;
  // raw files used for peak list creation
  protected final RawDataFile[] raw;
  // center RT values for each sample
  private double[] rtSum;
  private int[] rtValues;
  private double[] min, max;

  public RowGroup(final RawDataFile[] raw, int groupID) {
    super();
    this.raw = raw;
    this.groupID = groupID;
    this.min = new double[raw.length];
    this.max = new double[raw.length];
    for (int i = 0; i < min.length; i++) {
      min[i] = Double.POSITIVE_INFINITY;
      max[i] = Double.NEGATIVE_INFINITY;
    }
    this.rtSum = new double[raw.length];
    this.rtValues = new int[raw.length];
  }

  public void setGroupToAllRows() {
    this.forEach(r -> r.setGroup(this));
  }

  /**
   * Insert sort by ascending avg mz
   */
  @Override
  public synchronized boolean add(PeakListRow e) {
    for (int i = 0; i < rtSum.length; i++) {
      Feature f = e.getPeak(raw[i]);
      if (f != null) {
        rtSum[i] = (rtSum[i] + f.getRT());
        rtValues[i]++;
        // min max
        if (f.getRT() < min[i])
          min[i] = f.getRT();
        if (f.getRT() > max[i])
          max[i] = f.getRT();
      }
    }
    // insert sort find position
    for (int i = 0; i < size(); i++) {
      if (e.getAverageMZ() <= get(i).getAverageMZ()) {
        super.add(i, e);
        return true;
      }
    }
    // last position
    return super.add(e);
  }

  public RawDataFile[] getRaw() {
    return raw;
  }

  /**
   * checks for the same ID
   * 
   * @param row
   * @return
   */
  public boolean contains(PeakListRow row) {
    return contains(row.getID());
  }

  /**
   * checks for the same ID
   * 
   * @param row
   * @return
   */
  public boolean contains(int id) {
    for (PeakListRow r : this)
      if (r.getID() == id)
        return true;
    return false;
  }

  /**
   * Center retention time in raw file[i]
   * 
   * @param rawi
   * @return
   */
  public double getCenterRT(int rawi) {
    if (rtValues[rawi] == 0)
      return -1;
    return rtSum[rawi] / rtValues[rawi];
  }

  /**
   * center retention time of this group
   * 
   * @param rawi
   * @return
   */
  public double getCenterRT() {
    double center = 0;
    int counter = 0;
    for (int i = 0; i < rtSum.length; i++)
      if (rtValues[i] > 0) {
        center += rtSum[i] / rtValues[i];
        counter++;
      }
    return center / counter;
  }

  /**
   * checks if a feature is in range either between min and max or in range of avg+-tolerance
   * 
   * @param f
   * @return
   */
  public boolean isInRange(int rawi, Feature f, RTTolerance tol) {
    return hasPeak(rawi) && ((f.getRT() >= min[rawi] && f.getRT() <= max[rawi])
        || (tol.checkWithinTolerance(getCenterRT(rawi), f.getRT())));
  }

  /**
   * checks if this group has a feature in rawfile[i]
   * 
   * @param rawi
   * @return
   */
  public boolean hasPeak(int rawi) {
    return rtValues[rawi] > 0;
  }

  public int getGroupID() {
    return groupID;
  }

  public void setGroupID(int groupID) {
    this.groupID = groupID;
  }

  // ###########################################
  // for visuals

  public int getLastViewedRowI() {
    return lastViewedRow;
  }

  public PeakListRow getLastViewedRow() {
    return get(lastViewedRow);
  }

  public void setLastViewedRowI(int lastViewedRow) {
    this.lastViewedRow = lastViewedRow;
  }

  public int getLastViewedRawFileI() {
    return lastViewedRawFile;
  }

  public RawDataFile getLastViewedRawFile() {
    return raw[lastViewedRawFile];
  }

  public void setLastViewedRawFileI(int lastViewedRawFile) {
    if (lastViewedRawFile < 0)
      lastViewedRawFile = 0;
    else if (lastViewedRawFile >= raw.length)
      lastViewedRawFile = raw.length - 1;
    this.lastViewedRawFile = lastViewedRawFile;
  }

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   * 
   * @param i index in group
   * @param k index in group
   * @return
   */
  public boolean isCorrelated(int i, int k) {
    if (i == -1 || k == -1)
      return false;
    return true;
  }

  /**
   * Not all rows in this group need to be really correlated. Override in specialized RowGroup
   * classes
   * 
   * @param a
   * @param b
   * @return
   */
  public boolean isCorrelated(PeakListRow a, PeakListRow b) {
    int ia = indexOf(a);
    int ib = indexOf(b);
    if (ia == -1 || ib == -1)
      return false;
    return isCorrelated(ia, ib);
  }
}
