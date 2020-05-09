package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MergedScan;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import net.sf.mzmine.modules.tools.msmsspectramerge.MergedDataPoint;
import net.sf.mzmine.modules.tools.msmsspectramerge.MzMergeMode;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.maths.similarity.Similarity;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.scans.ScanUtils;

public class SimpleMergedScan extends SimpleImagingScan implements MergedScan {

  public enum Result {
    MERGED, MERGED_REPLACE_BEST_SCAN, FALSE;
  }

  private MzMergeMode mzMode = MzMergeMode.WEIGHTED_AVERAGE;
  private IntensityMergeMode intensityMergeMode;
  private Scan bestScan = null;
  private MergedDataPoint[] merged;
  private double bestTIC;
  private int mergedCount = 1;
  private DataPoint[] filteredMerged;
  private double lastNoiseLevel;
  private List<Integer> mergeTags = new ArrayList<>();

  public SimpleMergedScan(SimpleMergedScan sc, IntensityMergeMode intensityMergeMode) {
    this(sc, intensityMergeMode, -1);
  }

  public SimpleMergedScan(Scan sc, IntensityMergeMode intensityMergeMode, int mergeTag) {
    super(sc);
    this.intensityMergeMode = intensityMergeMode;
    if (sc instanceof SimpleMergedScan) {
      SimpleMergedScan msc = (SimpleMergedScan) sc;
      mergeTags.addAll(msc.mergeTags);
      bestScan = msc.getBestScan();
      bestTIC = msc.bestTIC;
      mergedCount = msc.getScanCount();
      merged = new MergedDataPoint[msc.merged.length];
      for (int i = 0; i < merged.length; i++) {
        merged[i] = msc.merged[i].getInstance(mzMode, intensityMergeMode);
      }
    } else {
      mergeTags.add(mergeTag);
      bestScan = sc;
      bestTIC = 0;
      merged = new MergedDataPoint[sc.getDataPoints().length];
      for (int i = 0; i < sc.getDataPoints().length; i++) {
        DataPoint d = sc.getDataPoints()[i];
        if (d instanceof MergedDataPoint)
          merged[i] = ((MergedDataPoint) d).getInstance(mzMode, intensityMergeMode);
        else
          merged[i] = new MergedDataPoint(mzMode, intensityMergeMode, d);
        bestTIC += d.getIntensity();
      }
    }
    setDataPoints(merged);
  }

  public List<Integer> getMergeTags() {
    return mergeTags;
  }

  public void addMergeTag(int tag) {
    mergeTags.add(tag);
  }

  /**
   * This scan was already compared to msc
   * 
   * @param msc
   * @return
   */
  public boolean wasAlreadyComparedTo(SimpleMergedScan msc) {
    return mergeTags.stream()
        .anyMatch(tag -> msc.mergeTags.stream().anyMatch(tag2 -> tag.equals(tag2)));
  }

  @Override
  public @Nonnull DataPoint[] getDataPoints() {
    return merged;
  }

  public Result merge(SimpleMergedScan source, MZTolerance mzTol, double noiseLevel,
      double minCosine, int minMatch) {
    if (source.wasAlreadyComparedTo(this))
      return Result.FALSE;

    DataPoint[] dataPoints = source.getDataPoints();
    DataPoint[] filtered = source.getFilteredDataPoints(noiseLevel);

    // align
    List<DataPoint[]> aligned = ScanAlignment.align(mzTol, getFilteredDataPoints(noiseLevel),
        filtered == null ? dataPoints : filtered);

    // overlapping within mass tolerance
    int overlap = (int) aligned.stream().filter(dp -> dp[0] != null && dp[1] != null).count();

    if (overlap >= minMatch) {
      // weighted cosine
      double[][] diffArray = ScanAlignment.toIntensityArray(aligned);
      double diffCosine = Similarity.COSINE.calc(diffArray);
      if (diffCosine >= minCosine) {
        // if aligned was filtered - need to realign all data points
        if (filtered != null && filteredMerged != null)
          aligned = ScanAlignment.align(mzTol, merged, dataPoints);
        // reset filtered
        filteredMerged = null;
        // merge
        MergedDataPoint[] newMerged = new MergedDataPoint[aligned.size()];
        for (int i = 0; i < aligned.size(); i++) {
          DataPoint[] pair = aligned.get(i);
          if (pair[0] != null && pair[1] != null)
            newMerged[i] = ((MergedDataPoint) pair[0]).merge(pair[1], mzMode, intensityMergeMode);
          // new data point
          else if (pair[0] != null)
            newMerged[i] = new MergedDataPoint(mzMode, intensityMergeMode, pair[0]);
          else if (pair[1] != null)
            newMerged[i] = new MergedDataPoint(mzMode, intensityMergeMode, pair[1]);
        }
        // replace
        merged = newMerged;
        setDataPoints(merged);
        this.mergeTags.addAll(source.getMergeTags());
        mergedCount += source.getScanCount();
        double tic = source.bestTIC;
        if (bestTIC < tic) {
          bestTIC = tic;
          bestScan = source.getBestScan();
          // create scan and replace the best
          return Result.MERGED_REPLACE_BEST_SCAN;
        }
        return Result.MERGED;
      }
    }
    return Result.FALSE;
  }

  public Result merge(DataPoint[] dataPoints, DataPoint[] filtered, MZTolerance mzTol,
      double noiseLevel, double minCosine, int minMatch) {
    // align
    List<DataPoint[]> aligned = ScanAlignment.align(mzTol, getFilteredDataPoints(noiseLevel),
        filtered == null ? dataPoints : filtered);

    // overlapping within mass tolerance
    int overlap = (int) aligned.stream().filter(dp -> dp[0] != null && dp[1] != null).count();

    if (overlap >= minMatch) {
      // weighted cosine
      double[][] diffArray = ScanAlignment.toIntensityArray(aligned);
      double diffCosine = Similarity.COSINE.calc(diffArray);
      if (diffCosine >= minCosine) {
        // if aligned was filtered - need to realign all data points
        if (filtered != null && filteredMerged != null)
          aligned = ScanAlignment.align(mzTol, merged, dataPoints);
        // reset filtered
        filteredMerged = null;
        // merge
        MergedDataPoint[] newMerged = new MergedDataPoint[aligned.size()];
        for (int i = 0; i < aligned.size(); i++) {
          DataPoint[] pair = aligned.get(i);
          if (pair[0] != null && pair[1] != null)
            newMerged[i] = ((MergedDataPoint) pair[0]).merge(pair[1], mzMode, intensityMergeMode);
          // new data point
          else if (pair[0] != null)
            newMerged[i] = new MergedDataPoint(mzMode, intensityMergeMode, pair[0]);
          else if (pair[1] != null)
            newMerged[i] = new MergedDataPoint(mzMode, intensityMergeMode, pair[1]);
        }
        // replace
        merged = newMerged;
        setDataPoints(merged);
        mergedCount++;
        double tic = Arrays.stream(dataPoints).mapToDouble(DataPoint::getIntensity).sum();
        if (bestTIC < tic) {
          bestTIC = tic;
          // create scan and replace the best
          return Result.MERGED_REPLACE_BEST_SCAN;
        }
        return Result.MERGED;
      }
    }
    return Result.FALSE;
  }

  public DataPoint[] getFilteredDataPoints(double noiseLevel) {
    if (noiseLevel == 0d)
      return merged;
    if (filteredMerged == null || noiseLevel != lastNoiseLevel) {
      lastNoiseLevel = noiseLevel;
      filteredMerged = ScanUtils.getFiltered(merged, noiseLevel);
    }
    return filteredMerged;
  }

  @Override
  public int getScanCount() {
    return mergedCount;
  }

  public void setBestScan(Scan scan) {
    bestScan = scan;
  }

  @Override
  public Scan getBestScan() {
    return bestScan;
  }

  @Override
  public IntensityMergeMode getIntensityMode() {
    return intensityMergeMode;
  }



  @Override
  public String toString() {
    return super.toString() + " merged: " + mergedCount;
  }

  public void clean(double minPercentSpectra, int minSpectra) {
    if (mergedCount <= 4)
      return;
    filteredMerged = null;
    merged = Arrays.stream(merged)
        .filter(dp -> dp.getN() >= minSpectra
            && minPercentSpectra <= (double) dp.getN() / (double) mergedCount)
        .toArray(MergedDataPoint[]::new);
  }

}
