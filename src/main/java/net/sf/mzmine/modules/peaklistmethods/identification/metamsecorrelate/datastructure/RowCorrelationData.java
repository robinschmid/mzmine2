package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 * 
 * @author RibRob
 *
 */
public class RowCorrelationData {
  // correlation to all peaks
  private FeatureShapeCorrelationData corrIProfile;
  //
  private boolean hasPeakShapeCorr = false;
  private FeatureShapeCorrelationData[] corrPeakShape;
  // correlation of all data points in one total correlation
  private FeatureShapeCorrelationData corrTotal;
  // min max avg
  private double minPeakShapeR, maxPeakShapeR, avgPeakShapeR, avgDPCount;

  // indices of the two rows
  // x: testedRow
  // y: row
  private int xRow, yRow;

  public RowCorrelationData(int xRow, int yRow, FeatureShapeCorrelationData corrIProfile,
      FeatureShapeCorrelationData[] corrPeakShape) {
    super();
    this.corrIProfile = corrIProfile;
    setCorrPeakShape(corrPeakShape);
    this.xRow = xRow;
    this.yRow = yRow;
  }

  public FeatureShapeCorrelationData[] getCorrPeakShape() {
    return corrPeakShape;
  }

  public boolean hasPeakShapeCorrelation() {
    return hasPeakShapeCorr;
  }

  public void setCorrPeakShape(FeatureShapeCorrelationData[] corrPeakShape) {
    // set
    this.corrPeakShape = corrPeakShape;
    if (corrPeakShape == null) {
      hasPeakShapeCorr = false;
      minPeakShapeR = 0;
      maxPeakShapeR = 0;
      return;
    }
    // min max
    minPeakShapeR = 1;
    maxPeakShapeR = -1;
    avgPeakShapeR = 0;
    avgDPCount = 0;
    int c = 0;

    for (int i = 0; i < corrPeakShape.length; i++) {
      if (corrPeakShape[i] != null && corrPeakShape[i].getReg() != null) {
        c++;
        avgPeakShapeR += corrPeakShape[i].getR();
        avgDPCount += corrPeakShape[i].getDPCount();
        if (corrPeakShape[i].getR() < minPeakShapeR)
          minPeakShapeR = corrPeakShape[i].getR();
        if (corrPeakShape[i].getR() > maxPeakShapeR)
          maxPeakShapeR = corrPeakShape[i].getR();
      }
    }
    if (c == 0 || maxPeakShapeR == -1) {
      hasPeakShapeCorr = false;
      minPeakShapeR = 0;
      maxPeakShapeR = 0;
    } else {
      avgDPCount = avgDPCount / c;
      avgPeakShapeR = avgPeakShapeR / c;
      hasPeakShapeCorr = true;
    }

    // create new total corr
    corrTotal = FeatureShapeCorrelationData.create(corrPeakShape);
  }

  /**
   * Correlation between two rows with all data points of all raw data files
   * 
   * @return
   */
  public FeatureShapeCorrelationData getTotalCorrelation() {
    return corrTotal;
  }

  public double getMinPeakShapeR() {
    return minPeakShapeR;
  }

  public void setMinPeakShapeR(double minPeakShapeR) {
    this.minPeakShapeR = minPeakShapeR;
  }

  public double getMaxPeakShapeR() {
    return maxPeakShapeR;
  }

  public void setMaxPeakShapeR(double maxPeakShapeR) {
    this.maxPeakShapeR = maxPeakShapeR;
  }

  public double getAvgPeakShapeR() {
    return avgPeakShapeR;
  }

  public double getAvgDPcount() {
    return avgDPCount;
  }

  public FeatureShapeCorrelationData getCorrIProfile() {
    return corrIProfile;
  }

  public void setCorrIProfileR(FeatureShapeCorrelationData corrIProfile) {
    this.corrIProfile = corrIProfile;
  }

  public int getXRow() {
    return xRow;
  }

  public void setXRow(int xRow) {
    this.xRow = xRow;
  }

  public int getYRow() {
    return yRow;
  }

  public void setYRow(int yRow) {
    this.yRow = yRow;
  }
}
