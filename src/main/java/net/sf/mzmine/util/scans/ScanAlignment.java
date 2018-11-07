package net.sf.mzmine.util.scans;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * Scan or mass list alignment based on data points array
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class ScanAlignment {

  /**
   * 
   * @param a
   * @param b
   * @return List of aligned data points
   */
  public List<DataPoint[]> align(MZTolerance mzTol, DataPoint[] a, DataPoint[] b) {
    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dp : a)
      list.add(new DataPoint[] {dp, null});

    for (DataPoint dpb : b) {
      // find match
      DataPoint[] dp = findMatch(mzTol, list, dpb, 1);
      // add or create new
      if (dp == null)
        list.add(new DataPoint[] {null, dpb});
      else
        dp[1] = dpb;
    }
    return list;
  }

  /**
   * 
   * @param a
   * @param b
   * @return List of aligned data points
   */
  public List<DataPoint[]> align(MZTolerance mzTol, List<DataPoint[]> scans) {
    if (scans.size() < 2)
      return null;

    List<DataPoint[]> list = new ArrayList<>();
    for (DataPoint dp : scans.get(0))
      list.add(new DataPoint[] {dp, null});

    for (int i = 0; i < scans.size(); i++) {
      // find match
      DataPoint[] dp = findMatch(mzTol, list, dpb, 1);
      // add or create new
      if (dp == null)
        list.add(new DataPoint[] {null, dpb});
      else
        dp[1] = dpb;
    }
    return list;
  }

  /**
   * Most intense
   * 
   * @param mzTol
   * @param list
   * @param dpb
   * @param indexB
   * @return
   */
  public DataPoint[] findMatch(MZTolerance mzTol, List<DataPoint[]> list, DataPoint dpb,
      int indexB) {
    DataPoint[] best = null;
    for (DataPoint[] dparray : list) {
      // continue if smaller than already inserted
      if (dparray[indexB] != null && dparray[indexB].getIntensity() > dpb.getIntensity())
        continue;

      //
      boolean outOfMZTol = false;

      for (int i = 0; i < dparray.length && !outOfMZTol; i++) {
        DataPoint dp = dparray[i];
        if (dp != null && i != indexB) {
          if (mzTol.checkWithinTolerance(dp.getMZ(), dpb.getMZ()))
            best = dparray;
          else
            outOfMZTol = true;
        }
      }
    }
    return best;
  }
}
