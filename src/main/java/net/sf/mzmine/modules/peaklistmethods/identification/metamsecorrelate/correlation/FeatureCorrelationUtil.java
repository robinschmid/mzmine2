package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.regression.SimpleRegression;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RFullCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter;
import net.sf.mzmine.util.maths.similarity.Similarity;

public class FeatureCorrelationUtil {

  // Logger.
  private static final Logger LOG = Logger.getLogger(FeatureCorrelationUtil.class.getName());


  /**
   * Check Feature shape correlation. If it does not meet the criteria, fshape corr is removed from
   * the correlation
   * 
   * @param peakList
   * @param minFFilter Correlation in minimum number of samples
   * @param corr the correlation to check
   * @param useTotalShapeCorrFilter Filter by total shape correlation
   * @param minTotalShapeCorrR
   * @param minShapeCorrR filter by minimum correlation
   * @param shapeSimMeasure the similarity measure
   * @return
   */
  public static boolean checkFShapeCorr(PeakList peakList, MinimumFeatureFilter minFFilter,
      R2RFullCorrelationData corr, boolean useTotalShapeCorrFilter, double minTotalShapeCorrR,
      double minShapeCorrR, SimilarityMeasure shapeSimMeasure) {
    // check feature shape corr
    if (!corr.hasFeatureShapeCorrelation())
      return false;

    // deletes correlations if criteria is not met
    corr.validate(minTotalShapeCorrR, useTotalShapeCorrFilter, minShapeCorrR, shapeSimMeasure);
    // check for correlation in min samples
    if (corr.hasFeatureShapeCorrelation())
      checkMinFCorrelation(peakList, minFFilter, corr);

    return corr.hasFeatureShapeCorrelation();
  }

  /**
   * Final check if there are enough F2FCorrelations in samples and groups
   * 
   * @param minFFilter
   * @param corr
   */
  public static void checkMinFCorrelation(PeakList peakList, MinimumFeatureFilter minFFilter,
      R2RFullCorrelationData corr) {
    List<RawDataFile> raw = new ArrayList<>();
    for (Entry<RawDataFile, CorrelationData> e : corr.getCorrPeakShape().entrySet())
      if (e.getValue() != null && e.getValue().isValid())
        raw.add(e.getKey());
    boolean hasCorrInSamples = minFFilter.filterMinFeatures(peakList.getRawDataFiles(), raw);
    if (!hasCorrInSamples) {
      // delete corr peak shape
      corr.setCorrPeakShape(null);
    }
  }


  /**
   * Feature height correlation (used as a filter), feature shape correlation used to group
   * 
   * @param raw
   * @param testRow
   * @param row
   * @param useHeightCorrFilter
   * @return R2R correlation, returns null if it was filtered by height correlation. Check for
   *         validity on result
   */
  public static R2RFullCorrelationData corrR2R(RawDataFile[] raw, PeakListRow testRow,
      PeakListRow row, boolean doFShapeCorr, int minCorrelatedDataPoints,
      int minCorrDPOnFeatureEdge, int minDPFHeightCorr, double minHeight,
      double noiseLevelShapeCorr, boolean useHeightCorrFilter, SimilarityMeasure heightSimilarity,
      double minHeightCorr) throws Exception {
    // check height correlation across all samples
    // only used as exclusion filter - not to group
    CorrelationData heightCorr = null;

    if (useHeightCorrFilter) {
      heightCorr = FeatureCorrelationUtil.corrR2RFeatureHeight(raw, testRow, row, minHeight,
          noiseLevelShapeCorr, minDPFHeightCorr);

      // significance is alpha. 0 is perfect
      double maxHeightCorrSlopeSignificance = 0.3;
      double minHeightCorrFoldChange = 10;
      // do not group if slope is negative / too low
      // go on if heightCorr is null
      if (useHeightCorrFilter && heightCorr != null
          && FeatureCorrelationUtil.isNegativeRegression(heightCorr, minHeightCorrFoldChange,
              maxHeightCorrSlopeSignificance, minDPFHeightCorr, minHeightCorr, heightSimilarity))
        return null;
    }

    // feature shape correlation
    Map<RawDataFile, CorrelationData> fCorr = null;
    if (doFShapeCorr)
      fCorr = FeatureCorrelationUtil.corrR2RFeatureShapes(raw, testRow, row,
          minCorrelatedDataPoints, minCorrDPOnFeatureEdge, noiseLevelShapeCorr);

    if (fCorr != null && fCorr.isEmpty())
      fCorr = null;

    R2RFullCorrelationData corr = new R2RFullCorrelationData(testRow, row, heightCorr, fCorr);
    return corr;
  }

  /**
   * Correlation of feature to feature shapes in all RawDataFiles of two rows
   * 
   * @param raw
   * @param row
   * @param g
   * @return Map of feature shape correlation data (can be empty NON null)
   * @throws Exception
   */
  public static Map<RawDataFile, CorrelationData> corrR2RFeatureShapes(final RawDataFile raw[],
      PeakListRow row, PeakListRow g, int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge,
      double noiseLevelShapeCorr) throws Exception {
    HashMap<RawDataFile, CorrelationData> corrData = new HashMap<>();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // peak shape correlation
        CorrelationData data = corrFeatureShape(f1, f2, true, minCorrelatedDataPoints,
            minCorrDPOnFeatureEdge, noiseLevelShapeCorr);

        // if correlation is really bad return null
        if (isNegativeRegression(data, 5, 0.2, 7, 0.5, SimilarityMeasure.PEARSON))
          return null;
        // enough data points
        if (data != null && data.getDPCount() >= minCorrelatedDataPoints)
          corrData.put(raw[r], data);
      }
    }
    return corrData;
  }

  /**
   * feature shape correlation
   * 
   * @param f1
   * @param f2
   * @return feature shape correlation or null if not possible not enough data points for a
   *         correlation
   * @throws Exception
   */
  public static CorrelationData corrFeatureShape(Feature f1, Feature f2, boolean sameRawFile,
      int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge, double noiseLevelShapeCorr)
      throws Exception {
    // Range<Double> rt1 = f1.getRawDataPointsRTRange();
    // Range<Double> rt2 = f2.getRawDataPointsRTRange();
    if (sameRawFile) {
      // scan numbers (not necessary 1,2,3...)
      int[] sn1 = f1.getScanNumbers();
      int[] sn2 = f2.getScanNumbers();

      if (sn1.length < minCorrelatedDataPoints || sn2.length < minCorrelatedDataPoints)
        return null;

      // find max of sn1
      int maxI = 0;
      double max = 0;
      for (int i = 0; i < sn1.length; i++) {
        double val = f1.getDataPoint(sn1[i]).getIntensity();
        if (val > max) {
          maxI = i;
          max = val;
        }
      }

      int offset2 = 0;
      // find corresponding scan in sn2
      for (int i = 0; i < sn2.length; i++) {
        if (sn1[maxI] == sn2[i]) {
          offset2 = i;
          break;
        }
      }

      // save max and min of intensity of val1(x)
      List<double[]> data = new ArrayList<double[]>();

      // add all data points <=max
      int i1 = maxI;
      int i2 = offset2;
      while (i1 >= 0 && i2 >= 0) {
        int s1 = sn1[i1];
        int s2 = sn2[i2];
        // add point, if not break
        if (s1 == s2) {
          if (!addDataPointToCorr(data, f1.getDataPoint(s1), f2.getDataPoint(s2),
              noiseLevelShapeCorr))
            break;
        }
        // end of peak found
        else
          break;
        i1--;
        i2--;
      }

      // check min data points left from apex
      int left = data.size() - 1;
      if (left < minCorrDPOnFeatureEdge)
        return null;

      // add all dp>max
      i1 = maxI + 1;
      i2 = offset2 + 1;
      while (i1 < sn1.length && i2 < sn2.length) {
        int s1 = sn1[i1];
        int s2 = sn2[i2];
        if (s1 == s2) {
          if (!addDataPointToCorr(data, f1.getDataPoint(s1), f2.getDataPoint(s2),
              noiseLevelShapeCorr))
            break;
        }
        // end of peak found
        else
          break;
        i1++;
        i2++;
      }
      // check right and total dp
      int right = data.size() - 1 - left;
      // return pearson r
      if (data.size() >= minCorrelatedDataPoints && right >= minCorrDPOnFeatureEdge) {
        return CorrelationData.create(data);
      }
    } else {
      // TODO if different raw file search for same rt
      // impute rt/I values if between 2 data points
    }
    return null;
  }

  private static boolean addDataPointToCorr(List<double[]> data, DataPoint a, DataPoint b,
      double noiseLevel) {
    // add all data points over a given threshold
    // TODO raw data (not smoothed)
    if (a != null && b != null) {
      // raw data
      double val1 = a.getIntensity();
      double val2 = b.getIntensity();

      if (val1 >= noiseLevel && val2 >= noiseLevel) {
        data.add(new double[] {val1, val2});
        return true;
      } else
        return false;
    } else
      return false;
  }

  /**
   * correlates the height profile of one row to another NO escape routine
   * 
   * @param raw
   * @param row
   * @param g
   * @return Correlation data of i profile of max i (or null if no correlation)
   */
  public static CorrelationData corrR2RFeatureHeight(final RawDataFile raw[], PeakListRow row,
      PeakListRow g, double minHeight, double noiseLevel, int minDPFHeightCorr) {
    // minimum of two
    minDPFHeightCorr = Math.min(minDPFHeightCorr, 2);

    List<double[]> data = new ArrayList<>();
    // calc ratio
    double ratio = 0;
    SimpleRegression reg = new SimpleRegression();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // I profile correlation
        double a = f1.getHeight();
        double b = f2.getHeight();
        if (a >= minHeight && b >= minHeight) {
          data.add(new double[] {a, b});
          ratio += a / b;
          reg.addData(a, b);
        }
      }
    }

    ratio = ratio / data.size();
    if (ratio != 0) {
      // estimate missing values as noise level if > minHeight
      for (int r = 0; r < raw.length; r++) {
        Feature f1 = row.getPeak(raw[r]);
        Feature f2 = g.getPeak(raw[r]);

        boolean amissing = (f1 == null || f1.getHeight() < minHeight);
        boolean bmissing = (f2 == null || f2.getHeight() < minHeight);
        // xor
        if (amissing ^ bmissing) {
          double a = amissing ? f2.getHeight() * ratio : f1.getHeight();
          double b = bmissing ? f1.getHeight() / ratio : f2.getHeight();

          // only if both are >= min height
          if (a >= minHeight && b >= minHeight) {
            if (amissing)
              a = Math.max(noiseLevel, f1 == null ? 0 : f1.getHeight());
            if (bmissing)
              b = Math.max(noiseLevel, f2 == null ? 0 : f2.getHeight());
            data.add(new double[] {a, b});
            reg.addData(a, b);
          }
        }
      }
    }

    // TODO weighting of intensity corr
    if (data.size() < 2)
      return null;
    else
      return CorrelationData.create(data);
  }


  /**
   * Only true if this should be filtered out. Need to have a minimum fold change to be significant.
   * 
   * @param data
   * @param reg
   * @param minFoldChange
   * @param maxSlopeSignificance
   * @return
   */
  public static boolean isNegativeRegression(CorrelationData corr, double minFoldChange,
      double maxSlopeSignificance, int minDP, double minSimilarity,
      SimilarityMeasure heightSimilarity) {
    // do not check if data is not sufficient
    if (!isSufficientData(corr, minDP, minFoldChange))
      return false;

    double significantSlope = 0;
    try {
      significantSlope = corr.getReg().getSignificance();
    } catch (MathException e) {
      LOG.log(Level.SEVERE, "slope significance cannot be calculated", e);
    }
    // if slope is negative
    // slope significance is low (alpha is high)
    // similarity is low
    return (corr.getReg().getSlope() <= 0
        || (!Double.isNaN(significantSlope) && significantSlope > maxSlopeSignificance)
        || corr.getSimilarity(heightSimilarity) < minSimilarity);
  }

  /**
   * 
   * @param corr
   * @param minDP minimum correlated data points
   * @param minFoldChange at least one data axis need to have >=maxFoldChange from min to max data
   * @return true if the correlation matches the rules false otherwise
   */
  public static boolean isSufficientData(CorrelationData corr, int minDP, double minFoldChange) {
    if (corr == null || (corr.getDPCount() < 3 || corr.getDPCount() < minDP))
      return false;

    double maxFC = Math.max(Similarity.maxFoldChange(corr.getData(), 0),
        Similarity.maxFoldChange(corr.getData(), 1));
    // do not use as filter if
    if (maxFC < minFoldChange)
      return false;

    // is sufficient
    return true;
  }

}
