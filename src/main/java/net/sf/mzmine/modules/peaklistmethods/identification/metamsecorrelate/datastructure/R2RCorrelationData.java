package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.Map;
import java.util.Map.Entry;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrelationData {

  // correlation of a to b
  private PeakListRow a, b;

  // correlation of all data points in one total correlation
  private CorrelationData corrTotal;
  // correlation to all peaks
  private CorrelationData corrIProfile;

  /**
   * Feature shape correlation in RawDataFiles
   */
  private Map<RawDataFile, CorrelationData> corrPeakShape;
  // min max avg
  private double minPeakShapeR, maxPeakShapeR, avgPeakShapeR, avgDPCount;

  public R2RCorrelationData(PeakListRow a, PeakListRow b, CorrelationData corrIProfile,
      Map<RawDataFile, CorrelationData> corrPeakShape) {
    super();
    this.a = a;
    this.b = b;
    this.corrIProfile = corrIProfile;
    setCorrPeakShape(corrPeakShape);
  }

  public Map<RawDataFile, CorrelationData> getCorrPeakShape() {
    return corrPeakShape;
  }

  public CorrelationData getCorrPeakShape(RawDataFile raw) {
    return corrPeakShape.get(raw);
  }

  /**
   * 
   * @param corrPeakShape
   */
  public void setCorrPeakShape(Map<RawDataFile, CorrelationData> corrPeakShape) {
    // set
    this.corrPeakShape = corrPeakShape;
    if (!hasFeatureShapeCorrelation()) {
      minPeakShapeR = 0;
      maxPeakShapeR = 0;
      avgPeakShapeR = 0;
      avgDPCount = 0;
      corrTotal = null;
      return;
    }
    // min max
    minPeakShapeR = 1;
    maxPeakShapeR = -1;
    avgPeakShapeR = 0;
    avgDPCount = 0;
    int c = corrPeakShape.size();

    for (Entry<RawDataFile, CorrelationData> e : corrPeakShape.entrySet()) {
      CorrelationData corr = e.getValue();
      avgPeakShapeR += corr.getR();
      avgDPCount += corr.getDPCount();
      if (corr.getR() < minPeakShapeR)
        minPeakShapeR = corr.getR();
      if (corr.getR() > maxPeakShapeR)
        maxPeakShapeR = corr.getR();
    }

    avgDPCount = avgDPCount / c;
    avgPeakShapeR = avgPeakShapeR / c;

    // create new total corr
    corrTotal = CorrelationData.create(corrPeakShape.values());
  }

  /**
   * Correlation between two rows with all data points of all raw data files
   * 
   * @return
   */
  public CorrelationData getTotalCorrelation() {
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

  public CorrelationData getCorrIProfile() {
    return corrIProfile;
  }

  public void setCorrIProfileR(CorrelationData corrIProfile) {
    this.corrIProfile = corrIProfile;
  }


  public PeakListRow getRowA() {
    return a;
  }

  public PeakListRow getRowB() {
    return b;
  }

  public int getIDA() {
    return a == null ? 0 : a.getID();
  }

  public int getIDB() {
    return b == null ? 0 : b.getID();
  }

  public boolean hasIMaxCorr() {
    return corrIProfile != null && corrIProfile.getReg() != null
        && corrIProfile.getReg().getN() > 0;
  }

  public boolean hasFeatureShapeCorrelation() {
    return (corrTotal != null && corrTotal.getReg() != null && corrTotal.getReg().getN() > 0);
  }

  /**
   * Either has Imax correlation or feature shape correlation
   * 
   * @return
   */
  public boolean isValid() {
    return hasFeatureShapeCorrelation() || hasIMaxCorr();
  }

  /**
   * Validate this correlation data. If criteria is not met for ImaxCorrelation or
   * featureShapeCorrelation - the specific correlation is deleted
   * 
   * @param minMaxICorr
   * @param minShapeCorrR
   */
  public void validate(double minMaxICorr, double minShapeCorrR) {
    if (hasIMaxCorr() && (corrIProfile.getR() < minMaxICorr)) {
      // delete maxIcorr
      corrIProfile = null;
    }
    if (hasFeatureShapeCorrelation() && (avgPeakShapeR < minShapeCorrR)) {
      // delete peak shape corr
      setCorrPeakShape(null);
    }
  }
}
