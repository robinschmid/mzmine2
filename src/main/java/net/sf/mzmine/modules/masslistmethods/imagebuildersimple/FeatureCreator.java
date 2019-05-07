package net.sf.mzmine.modules.masslistmethods.imagebuildersimple;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.Range;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleFeature;

public class FeatureCreator {

  public static Feature createFeature(RawDataFile dataFile, String massListName, Scan[] scans,
      double meanMZ, double sigma) throws Exception {
    // collect data points
    List<DataPoint> data = new ArrayList<>();
    IntList scanNumbers = new IntArrayList();

    // stats
    double mz = 0;
    double rt = 0;
    double height = 0;
    double minHeight = Double.MAX_VALUE;
    double area = 0;
    int representativeScan = 0;
    int fragmentScanNumber = 0;
    Range<Double> rtRange = null;
    double rtMin = -1, rtMax = -1;
    Range<Double> mzRange = null;
    Range<Double> intensityRange = null;

    Scan lastAddedScan = null;

    // create chrom. add all dp
    for (Scan scan : scans) {
      // go through mass list
      MassList massList = scan.getMassList(massListName);
      DataPoint dps[] = massList.getDataPoints();
      DataPoint best = null;

      // find mz range and best dp
      for (int i = 0; i < dps.length; i++) {
        DataPoint dp = dps[i];
        if (i == dps.length - 1 || dp.getMZ() > meanMZ + sigma) {
          if (best != null) {
            // add best dp
            data.add(best);
            scanNumbers.add(scan.getScanNumber());
            // stats
            if (best.getIntensity() > height) {
              height = best.getIntensity();
              representativeScan = scan.getScanNumber();
              rt = scan.getRetentionTime();
            }
            if (best.getIntensity() < minHeight)
              minHeight = best.getIntensity();

            mz += best.getMZ();

            // first dp
            if (lastAddedScan == null) {
              intensityRange = Range.singleton(best.getIntensity());
              mzRange = Range.singleton(best.getMZ());
              rtMin = scan.getRetentionTime();
            } else {
              // For area calculation, we use retention time in seconds
              double previousRT = lastAddedScan.getRetentionTime() * 60d;
              double currentRT = scan.getRetentionTime() * 60d;
              double previousHeight = data.get(data.size() - 1).getIntensity();
              double currentHeight = best.getIntensity();
              area += (currentRT - previousRT) * (currentHeight + previousHeight) / 2;

              // ranges
              intensityRange = intensityRange.span(Range.singleton(best.getIntensity()));
              mzRange = mzRange.span(Range.singleton(best.getMZ()));

              rtMax = scan.getRetentionTime();
            }
            lastAddedScan = scan;
          }
          break;
        } else if (dp.getMZ() >= meanMZ - sigma) {
          // find best mz (highest, closest to mean) TODO
          if (best == null || dp.getIntensity() > best.getIntensity()) {
            best = dp;
          }
        }
      }
    }
    if (data.size() > 4) {
      // array
      DataPoint[] dataArray = data.toArray(new DataPoint[data.size()]);
      // stats
      mz = mz / data.size();

      rtRange = Range.closed(rtMin, rtMax);

      // TODO get all fragment scan numbers
      int[] allFragmentScans = null;
      // create
      return new SimpleFeature(dataFile, mz, rt, height, area, scanNumbers.toIntArray(), dataArray,
          FeatureStatus.DETECTED, representativeScan, fragmentScanNumber, allFragmentScans, rtRange,
          mzRange, intensityRange);
    } else {
      throw new Exception("Not enough data points");
    }
  }

}
