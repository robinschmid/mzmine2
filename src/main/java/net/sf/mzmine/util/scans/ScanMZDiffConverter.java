package net.sf.mzmine.util.scans;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class ScanMZDiffConverter {


  /**
   * Takes all data points of a scan or masslist and converts them to a mz differences array. The
   * resulting data points contain a deltaMZ and the number n of occurance in the scan. (e.g. [18,
   * 3] would correspond to three losses of H2O)
   * 
   * @param scan
   * @param maxDiff absolute max difference of neutral loss mass (in Da)
   * @return
   */
  public static DataPoint[] getAllMZDiff(DataPoint[] scan, MZTolerance maxDiff, double minHeight,
      int maxSignals) {
    if (maxSignals <= 0 || scan.length <= maxSignals)
      return getAllMZDiff(scan, maxDiff, minHeight);
    else {
      DataPoint[] max = ScanUtils.getMostAbundantSignals(scan, maxSignals);
      return getAllMZDiff(max, maxDiff, minHeight);
    }
  }

  /**
   * Takes all data points of a scan or masslist and converts them to a mz differences array. The
   * resulting data points contain a deltaMZ and the number n of occurance in the scan. (e.g. [18,
   * 3] would correspond to three losses of H2O)
   * 
   * @param scan
   * @param maxDiff
   * @param maxDiff absolute max difference of neutral loss mass (in Da)
   * @return
   */
  public static DataPoint[] getAllMZDiff(DataPoint[] scan, MZTolerance maxDiff, double minHeight) {
    List<SimpleDataPoint> list = new ArrayList<>();

    for (int i = 0; i < scan.length - 1; i++) {
      if (scan[i].getIntensity() >= minHeight) {
        for (int j = i + 1; j < scan.length; j++) {
          if (scan[j].getIntensity() >= minHeight) {
            double delta = Math.abs(scan[i].getMZ() - scan[j].getMZ());
            // find existing
            SimpleDataPoint dp = findMatch(list, delta, maxDiff);
            if (dp != null)
              dp.setIntensity(dp.getIntensity() + 1);
            else
              list.add(new SimpleDataPoint(delta, 1));
          }
        }
      }
    }
    return list.toArray(new DataPoint[list.size()]);
  }

  /**
   * Is already in list?
   * 
   * @param mzTol
   * @param list
   * @param dpb
   * @param indexB
   * @return
   */
  public static SimpleDataPoint findMatch(List<SimpleDataPoint> list, double delta,
      MZTolerance maxDiff) {
    for (SimpleDataPoint dp : list) {
      if (maxDiff.checkWithinTolerance(dp.getMZ(), delta)) {
        return dp;
      }
    }
    return null;
  }

  /**
   * 
   * @param diffAligned list of aligned data points in scans [a, b]
   * @param indexA
   * @param indexB
   */
  public static int getOverlapOfAlignedDiff(List<DataPoint[]> diffAligned, int indexA, int indexB) {
    int overlap = 0;
    for (DataPoint[] dps : diffAligned) {
      DataPoint a = dps[indexA];
      DataPoint b = dps[indexB];
      if (a != null && b != null) {
        overlap += Math.min(a.getIntensity(), b.getIntensity());
      }
    }
    return overlap;
  }

}
