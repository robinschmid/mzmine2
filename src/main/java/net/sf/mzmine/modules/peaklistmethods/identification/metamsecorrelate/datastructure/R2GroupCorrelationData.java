package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.List;
import net.sf.mzmine.datamodel.PeakListRow;

/**
 * correlation of one row to a group
 * 
 * @author RibRob
 */
public class R2GroupCorrelationData {
  private PeakListRow row;
  // row index is xRow in corr data
  private List<R2RCorrelationData> corr;
  private double maxHeight;
  // averages are calculated by dividing by the row count
  private double minIProfileR, avgIProfileR, maxIProfileR;
  private double minPeakShapeR, avgPeakShapeR, maxPeakShapeR, avgDPCount;
  // total peak shape r
  private double avgTotalPeakShapeR;

  public R2GroupCorrelationData(PeakListRow row, List<R2RCorrelationData> corr, double maxHeight) {
    super();
    this.row = row;
    setCorr(corr);
    this.maxHeight = maxHeight;
  }

  public void setCorr(List<R2RCorrelationData> corr) {
    this.corr = corr;
    recalcCorr();
  }

  /**
   * Recalc correlation
   */
  public void recalcCorr() {
    // min max
    minIProfileR = 1;
    maxIProfileR = -1;
    avgIProfileR = 0;
    minPeakShapeR = 1;
    maxPeakShapeR = -1;
    avgPeakShapeR = 0;
    avgDPCount = 0;
    avgTotalPeakShapeR = 0;
    int cImax = 0;
    int cPeakShape = 0;

    for (R2RCorrelationData r2r : corr) {
      if (r2r.hasIMaxCorr()) {
        cImax++;
        double iProfileR = r2r.getCorrIProfile().getR();
        avgIProfileR += iProfileR;
        if (iProfileR < minIProfileR)
          minIProfileR = iProfileR;
        if (iProfileR > maxIProfileR)
          maxIProfileR = iProfileR;
      }

      // peak shape correlation
      if (r2r.hasFeatureShapeCorrelation()) {
        cPeakShape++;
        avgTotalPeakShapeR += r2r.getTotalCorrelation().getR();
        avgPeakShapeR += r2r.getAvgPeakShapeR();
        avgDPCount += r2r.getAvgDPcount();
        if (r2r.getMinPeakShapeR() < minPeakShapeR)
          minPeakShapeR = r2r.getMinPeakShapeR();
        if (r2r.getMaxPeakShapeR() > maxPeakShapeR)
          maxPeakShapeR = r2r.getMaxPeakShapeR();
      }

      avgTotalPeakShapeR = avgTotalPeakShapeR / cPeakShape;
      avgIProfileR = avgIProfileR / cImax;
      avgDPCount = avgDPCount / cPeakShape;
      avgPeakShapeR = avgPeakShapeR / cPeakShape;
    }
  }

  public double getMaxHeight() {
    return maxHeight;
  }

  public void setMaxHeight(double maxHeight) {
    this.maxHeight = maxHeight;
  }

  public List<R2RCorrelationData> getCorr() {
    return corr;
  }

  public double getMinIProfileR() {
    return minIProfileR;
  }

  public void setMinIProfileR(double minIProfileR) {
    this.minIProfileR = minIProfileR;
  }

  public double getAvgIProfileR() {
    return avgIProfileR;
  }

  public void setAvgIProfileR(double avgIProfileR) {
    this.avgIProfileR = avgIProfileR;
  }

  public double getMaxIProfileR() {
    return maxIProfileR;
  }

  public void setMaxIProfileR(double maxIProfileR) {
    this.maxIProfileR = maxIProfileR;
  }

  public double getMinPeakShapeR() {
    return minPeakShapeR;
  }

  public void setMinPeakShapeR(double minPeakShapeR) {
    this.minPeakShapeR = minPeakShapeR;
  }

  public double getAvgPeakShapeR() {
    return avgPeakShapeR;
  }

  public void setAvgPeakShapeR(double avgPeakShapeR) {
    this.avgPeakShapeR = avgPeakShapeR;
  }

  public double getMaxPeakShapeR() {
    return maxPeakShapeR;
  }

  public void setMaxPeakShapeR(double maxPeakShapeR) {
    this.maxPeakShapeR = maxPeakShapeR;
  }

  public double getAvgDPCount() {
    return avgDPCount;
  }

  public void setAvgDPCount(double avgDPCount) {
    this.avgDPCount = avgDPCount;
  }

  public double getAvgTotalPeakShapeR() {
    return avgTotalPeakShapeR;
  }

  /**
   * 
   * @param rowI
   * @return the correlation data of this row to row[rowI]
   * @throws Exception (should not happen, only if processing was corrupt)
   */
  public R2RCorrelationData getCorrelationToRowI(int rowI) throws Exception {
    if (row.getID() == rowI)
      throw new Exception("No correlation of row to itself");
    for (R2RCorrelationData c : corr) {
      if (c.getIDA() == rowI || c.getIDB() == rowI)
        return c;
    }
    return null;
  }

  /**
   * 
   * @param rowI
   * @return the correlation data of this row to row[rowI]
   * @throws Exception
   */
  public R2RCorrelationData getCorrelationToRow(PeakListRow row) throws Exception {
    return getCorrelationToRowI(row.getID());
  }

}
