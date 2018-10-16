package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartColor;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;

public class PKLRowGroup extends ArrayList<PeakListRow> {
  // colors
  public static final Paint[] colors = ChartColor.createDefaultPaintArray();
  // visualization
  private int lastViewedRow = 0;
  private int lastViewedRawFile = 0;
  // correlation data of all rows to this group
  private R2GroupCorrelationData[] corr;
  // running index of groups
  private int groupID = 0;
  // raw files used for peak list creation
  private final RawDataFile[] raw;
  // center RT values for each sample
  private double[] rtSum;
  private int[] rtValues;
  private double[] min, max;

  public PKLRowGroup(final RawDataFile[] raw, int groupID) {
    super();
    this.raw = raw;
    this.min = new double[raw.length];
    this.max = new double[raw.length];
    for (int i = 0; i < min.length; i++) {
      min[i] = Double.POSITIVE_INFINITY;
      max[i] = Double.NEGATIVE_INFINITY;
    }
    this.rtSum = new double[raw.length];
    this.rtValues = new int[raw.length];
    this.groupID = groupID;
  }

  /**
   * Recalculates all stats for this group
   * 
   * @param corrMap
   */
  public void recalcGroupCorrelation(R2RCorrMap corrMap) {
    // init
    corr = new R2GroupCorrelationData[this.size()];

    // test all rows against all other rows
    for (int i = 0; i < this.size(); i++) {
      List<R2RCorrelationData> rowCorr = new ArrayList<>();
      PeakListRow testRow = this.get(i);
      for (int k = 0; k < this.size(); k++) {
        if (i != k) {
          R2RCorrelationData r2r = corrMap.get(testRow, this.get(k));
          if (r2r != null)
            rowCorr.add(r2r);
        }
      }
      // create group corr object
      corr[i] = new R2GroupCorrelationData(testRow, rowCorr, testRow.getBestPeak().getHeight());
    }
  }

  /**
   * correlation of each row to the group
   * 
   * @return
   */
  public R2GroupCorrelationData[] getCorr() {
    return corr;
  }

  /**
   * correlation of a row to the group
   * 
   * @return
   */
  public R2GroupCorrelationData getCorr(int row) {
    return corr[row];
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

  public RawDataFile[] getRaw() {
    return raw;
  }

  /**
   * returns the corresponding identity with the correlation data
   * 
   * @param i
   * @return
   */
  public MSEGroupPeakIdentity getPeakIdentityOfRow(int i) {
    PeakListRow row = this.get(i);
    for (PeakIdentity ident : row.getPeakIdentities()) {
      if (MSEGroupPeakIdentity.class.isInstance(ident)) {
        MSEGroupPeakIdentity gIdent = (MSEGroupPeakIdentity) ident;
        if (gIdent.getGroup().getGroupID() == this.getGroupID()) {
          return gIdent;
        }
      }
    }
    return null;
  }

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
}
