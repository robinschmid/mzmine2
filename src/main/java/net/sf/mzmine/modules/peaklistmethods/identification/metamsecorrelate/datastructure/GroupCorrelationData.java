package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

/**
 * correlation of one row to a group
 * 
 * @author RibRob
 */
public class GroupCorrelationData {
  // row index is xRow in corr data
  private RowCorrelationData[] corr;
  private double maxHeight;
  // averages are calculated by dividing by the row count
  private double minIProfileR, avgIProfileR, maxIProfileR;
  private double minPeakShapeR, avgPeakShapeR, maxPeakShapeR, avgDPCount;
  // total peak shape r
  private double avgTotalPeakShapeR;

  public GroupCorrelationData(RowCorrelationData[] corr, double maxHeight) {
    super();
    setCorr(corr);
    this.maxHeight = maxHeight;
  }

  public void setCorr(RowCorrelationData[] corr) {
    this.corr = corr;
    // min max
    minIProfileR = 1;
    maxIProfileR = -1;
    avgIProfileR = 0;
    minPeakShapeR = 1;
    maxPeakShapeR = -1;
    avgPeakShapeR = 0;
    avgDPCount = 0;
    avgTotalPeakShapeR = 0;
    int c = 0;
    int cR2R = 0;
    for (int i = 0; i < corr.length; i++) {
      if (corr[i] != null) {
        cR2R++;
        double iProfileR = corr[i].getCorrIProfile().getR();
        avgIProfileR += iProfileR;
        if (iProfileR < minIProfileR)
          minIProfileR = iProfileR;
        if (iProfileR > maxIProfileR)
          maxIProfileR = iProfileR;

        // peak shape correlation
        if (corr[i].hasPeakShapeCorrelation()) {
          c++;
          avgTotalPeakShapeR += corr[i].getTotalCorrelation().getR();
          avgPeakShapeR += corr[i].getAvgPeakShapeR();
          avgDPCount += corr[i].getAvgDPcount();
          if (corr[i].getMinPeakShapeR() < minPeakShapeR)
            minPeakShapeR = corr[i].getMinPeakShapeR();
          if (corr[i].getMaxPeakShapeR() > maxPeakShapeR)
            maxPeakShapeR = corr[i].getMaxPeakShapeR();
        }
      }
      avgTotalPeakShapeR = avgTotalPeakShapeR / c;
      avgIProfileR = avgIProfileR / corr.length;
      avgDPCount = avgDPCount / c;
      avgPeakShapeR = avgPeakShapeR / c;
    }
  }

  public double getMaxHeight() {
    return maxHeight;
  }

  public void setMaxHeight(double maxHeight) {
    this.maxHeight = maxHeight;
  }

  public RowCorrelationData[] getCorr() {
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
   */
  public RowCorrelationData getCorrelationToRowI(int rowI) {
    for (int i = 0; i < corr.length; i++)
      if (corr[i] != null && corr[i].getYRow() == rowI)
        return corr[i];
    return null;
  }

}
