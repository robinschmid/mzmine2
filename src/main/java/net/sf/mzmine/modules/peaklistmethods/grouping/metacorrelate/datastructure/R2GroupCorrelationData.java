package net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.CorrelationData.SimilarityMeasure;

/**
 * correlation of one row to a group
 * 
 * @author RibRob
 */
public class R2GroupCorrelationData {
  private PeakListRow row;
  // row index is xRow in corr data
  private List<R2RFullCorrelationData> corr;
  private double maxHeight;
  // averages are calculated by dividing by the row count
  private double minHeightR, avgHeightR, maxHeightR;
  private double minShapeR, avgShapeR, maxShapeR, avgDPCount;

  // average cosine
  private double avgShapeCosineSim;
  private double avgCosineHeightCorr;

  // total peak shape r
  private double avgTotalPeakShapeR;

  public R2GroupCorrelationData(PeakListRow row, List<R2RFullCorrelationData> corr,
      double maxHeight) {
    super();
    this.row = row;
    setCorr(corr);
    this.maxHeight = maxHeight;
  }

  /**
   * 
   * @param peakList
   * @return
   */
  public static Stream<R2GroupCorrelationData> streamFrom(PeakList peakList) {
    if (peakList.getGroups() == null)
      return Stream.empty();
    return peakList.getGroups().stream().filter(g -> g instanceof CorrelationRowGroup)
        .map(g -> ((CorrelationRowGroup) g).getCorr()).flatMap(Arrays::stream);

  }


  public void setCorr(List<R2RFullCorrelationData> corr) {
    this.corr = corr;
    recalcCorr();
  }

  /**
   * Recalc correlation
   */
  public void recalcCorr() {
    // min max
    minHeightR = 1;
    maxHeightR = -1;
    avgHeightR = 0;
    minShapeR = 1;
    maxShapeR = -1;
    avgShapeR = 0;
    avgShapeCosineSim = 0;
    avgDPCount = 0;
    avgTotalPeakShapeR = 0;
    avgCosineHeightCorr = 0;
    int cImax = 0;
    int cPeakShape = 0;

    for (R2RFullCorrelationData r2r : corr) {
      if (r2r.hasHeightCorr()) {
        cImax++;
        avgCosineHeightCorr += r2r.getCosineHeightCorr();
        double iProfileR = r2r.getHeightCorr().getR();
        avgHeightR += iProfileR;
        if (iProfileR < minHeightR)
          minHeightR = iProfileR;
        if (iProfileR > maxHeightR)
          maxHeightR = iProfileR;
      }

      // peak shape correlation
      if (r2r.hasFeatureShapeCorrelation()) {
        cPeakShape++;
        avgTotalPeakShapeR += r2r.getTotalCorr().getR();
        avgShapeR += r2r.getAvgShapeR();
        avgShapeCosineSim += r2r.getAvgShapeCosineSim();
        avgDPCount += r2r.getAvgDPcount();
        if (r2r.getMinShapeR() < minShapeR)
          minShapeR = r2r.getMinShapeR();
        if (r2r.getMaxShapeR() > maxShapeR)
          maxShapeR = r2r.getMaxShapeR();
      }
    }
    avgTotalPeakShapeR = avgTotalPeakShapeR / cPeakShape;
    avgHeightR = avgHeightR / cImax;
    avgCosineHeightCorr = avgCosineHeightCorr / cImax;
    avgDPCount = avgDPCount / cPeakShape;
    avgShapeR = avgShapeR / cPeakShape;
    avgShapeCosineSim = avgShapeCosineSim / cPeakShape;
  }


  /**
   * The similarity or NaN if data is null or empty
   * 
   * @param type
   * @return
   */
  public double getAvgHeightSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RFullCorrelationData r2r : corr) {
      double v = r2r.getHeightSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   * 
   * @param type
   * @return
   */
  public double getAvgTotalSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RFullCorrelationData r2r : corr) {
      double v = r2r.getTotalSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  /**
   * The similarity or NaN if data is null or empty
   * 
   * @param type
   * @return
   */
  public double getAvgPeakShapeSimilarity(SimilarityMeasure type) {
    double mean = 0;
    int n = 0;
    for (R2RFullCorrelationData r2r : corr) {
      double v = r2r.getAvgPeakShapeSimilarity(type);
      if (!Double.isNaN(v)) {
        mean += v;
        n++;
      }
    }
    return n > 0 ? mean / n : Double.NaN;
  }

  public double getAvgShapeCosineSim() {
    return avgShapeCosineSim;
  }

  public double getMaxHeight() {
    return maxHeight;
  }

  public List<R2RFullCorrelationData> getCorr() {
    return corr;
  }

  public double getMinIProfileR() {
    return minHeightR;
  }

  public double getAvgIProfileR() {
    return avgHeightR;
  }

  public double getMaxIProfileR() {
    return maxHeightR;
  }

  public double getMinPeakShapeR() {
    return minShapeR;
  }

  public double getAvgPeakShapeR() {
    return avgShapeR;
  }

  public double getMaxPeakShapeR() {
    return maxShapeR;
  }

  public double getAvgDPCount() {
    return avgDPCount;
  }

  public double getAvgTotalPeakShapeR() {
    return avgTotalPeakShapeR;
  }

  /**
   * Height correlation across samples
   * 
   * @return
   */
  public double getAvgCosineHeightCorr() {
    return avgCosineHeightCorr;
  }


  /**
   * 
   * @param rowI
   * @return the correlation data of this row to row[rowI]
   */
  public R2RFullCorrelationData getCorrelationToRowID(int rowI) {
    if (row.getID() == rowI)
      return null;
    for (R2RFullCorrelationData c : corr) {
      if (c.getIDA() == rowI || c.getIDB() == rowI)
        return c;
    }
    return null;
  }

  public PeakListRow getRow() {
    return row;
  }

  /**
   * 
   * @param rowI
   * @return the correlation data of this row to row[rowI]
   * @throws Exception
   */
  public R2RFullCorrelationData getCorrelationToRow(PeakListRow row) {
    return getCorrelationToRowID(row.getID());
  }

}
