package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.Map;
import java.util.Map.Entry;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData.SimilarityMeasure;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 * 
 * @author Robin Schmid
 *
 */
public class R2RFullCorrelationData extends R2RCorrelationData {

  public enum NegativMarker {
    // intensity range is not shared between these two rows
    // at least in one raw data file: the features are out of RT range
    // the features do not overlap with X % of their intensity
    FeaturesDoNotOverlap, //
    MinFeaturesRequirementNotMet; //
  }

  // correlation of all data points in one total correlation
  private CorrelationData corrTotal;
  // correlation to all peaks
  private CorrelationData heightCorr;

  /**
   * Feature shape correlation in RawDataFiles
   */
  private Map<RawDataFile, CorrelationData> corrPeakShape;
  // min max avg
  private double minShapeR, maxShapeR, avgShapeR, avgDPCount;
  // cosine
  private double avgShapeCosineSim;

  public R2RFullCorrelationData(PeakListRow a, PeakListRow b, CorrelationData corrIProfile,
      Map<RawDataFile, CorrelationData> corrPeakShape) {
    super(a, b);
    this.heightCorr = corrIProfile;
    setCorrPeakShape(corrPeakShape);
  }

  public Map<RawDataFile, CorrelationData> getCorrPeakShape() {
    return corrPeakShape;
  }

  public CorrelationData getCorrPeakShape(RawDataFile raw) {
    return corrPeakShape == null ? null : corrPeakShape.get(raw);
  }

  /**
   * 
   * @param corrPeakShape
   */
  public void setCorrPeakShape(Map<RawDataFile, CorrelationData> corrPeakShape) {
    // set
    this.corrPeakShape = corrPeakShape;
    if (!hasFeatureShapeCorrelation()) {
      minShapeR = 0;
      maxShapeR = 0;
      avgShapeR = 0;
      avgShapeCosineSim = 0;
      avgDPCount = 0;
      corrTotal = null;
      return;
    }
    // min max
    minShapeR = 1;
    maxShapeR = -1;
    avgShapeR = 0;
    avgShapeCosineSim = 0;
    avgDPCount = 0;
    int c = corrPeakShape.size();

    for (Entry<RawDataFile, CorrelationData> e : corrPeakShape.entrySet()) {
      CorrelationData corr = e.getValue();
      avgShapeR += corr.getR();
      avgShapeCosineSim += corr.getCosineSimilarity();
      avgDPCount += corr.getDPCount();
      if (corr.getR() < minShapeR)
        minShapeR = corr.getR();
      if (corr.getR() > maxShapeR)
        maxShapeR = corr.getR();
    }

    avgDPCount = avgDPCount / c;
    avgShapeR = avgShapeR / c;
    avgShapeCosineSim = avgShapeCosineSim / c;

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

  public double getMinShapeR() {
    return minShapeR;
  }

  public double getMaxShapeR() {
    return maxShapeR;
  }

  /**
   * Get average similarity score
   * 
   * @param measure
   * @return
   */
  public double getSimilarity(SimilarityMeasure measure) {
    switch (measure) {
      case COSINE_SIM:
        return getAvgShapeCosineSim();
      case PEARSON:
        return getAvgShapeR();
    }
    return 0;
  }

  @Override
  public double getAvgShapeR() {
    return avgShapeR;
  }

  @Override
  public double getAvgShapeCosineSim() {
    return avgShapeCosineSim;
  }

  public double getAvgDPcount() {
    return avgDPCount;
  }

  public CorrelationData getHeightCorr() {
    return heightCorr;
  }

  public void setCorrIProfileR(CorrelationData corrIProfile) {
    this.heightCorr = corrIProfile;
  }

  public boolean hasHeightCorr() {
    return heightCorr != null && heightCorr.getReg() != null && heightCorr.getReg().getN() > 0;
  }

  @Override
  public boolean hasFeatureShapeCorrelation() {
    return (corrPeakShape != null && !corrPeakShape.isEmpty());
  }

  /**
   * Either has Imax correlation or feature shape correlation
   * 
   * @return
   */
  public boolean isValid() {
    return hasFeatureShapeCorrelation() || hasHeightCorr();
  }

  /**
   * Validate this correlation data. If criteria is not met for ImaxCorrelation or
   * featureShapeCorrelation - the specific correlation is deleted
   * 
   * @param minMaxICorr
   * @param minShapeCorrR
   */
  public void validate(double minMaxICorr, double minShapePearsonR, double minShapeCosineSim) {
    if (hasFeatureShapeCorrelation()
        && (avgShapeR < minShapePearsonR || avgShapeCosineSim < minShapeCosineSim)) {
      // delete peak shape corr
      setCorrPeakShape(null);
    }
  }

  public double getCosineHeightCorr() {
    return hasHeightCorr() ? heightCorr.getCosineSimilarity() : 0;
  }
}
