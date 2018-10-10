package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter;

import java.util.HashMap;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.AbsoluteNRelative;

public class MinimumFeatureFilter {

  /**
   * Minimum percentage of samples (in group if useGroup) that have to contain a feature
   */
  private final AbsoluteNRelative minFInSamples;
  private final AbsoluteNRelative minFInGroups;
  private final double minFeatureHeight;

  // sample group size
  private UserParameter<?, ?> sgroupPara;
  private HashMap<String, Integer> sgroupSize;
  private boolean filterGroups = false;


  public MinimumFeatureFilter(AbsoluteNRelative minFInSamples, AbsoluteNRelative minFInGroups,
      double minFeatureHeight) {
    super();
    this.minFInSamples = minFInSamples;
    this.minFInGroups = minFInGroups;
    this.minFeatureHeight = minFeatureHeight;
  }


  public void setGroupFilterEnabled(boolean state) {
    filterGroups = state;
  }


  /**
   * only keep rows which contain features in at least X % samples in a set called before starting
   * row processing
   * 
   * @param raw
   * @param row
   * @return
   */
  public boolean filterMinFeatures(MZmineProject project, final RawDataFile raw[],
      PeakListRow row) {
    // filter min samples in all
    if (minFInSamples.isGreaterZero()) {
      int n = 0;
      for (RawDataFile file : raw) {
        Feature f = row.getPeak(file);
        if (f != null && f.getHeight() >= minFeatureHeight) {
          n++;
        }
      }
      // stop if <n
      if (!minFInSamples.checkGreaterEqualMax(raw.length, n))
        return false;
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero())
      return true;

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<String, MutableInt>();
    for (RawDataFile file : raw) {
      Feature f = row.getPeak(file);
      if (f != null && f.getHeight() >= minFeatureHeight) {
        String sgroup = sgroupOf(project, file);

        MutableInt count = counter.get(sgroup);
        if (count == null) {
          // new counter with total N for group size
          counter.put(sgroup, new MutableInt(sgroupSize.get(sgroup)));
        } else {
          count.increment();
        }
      }
    }
    // only needs a minimum number of features in at least one sample group
    // only go on if feature was present in X % of the samples
    for (MutableInt v : counter.values()) {
      // at least one
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value))
        return true;
    }
    // no fit
    return false;
  }


  public boolean filterMinFeaturesOverlap(MZmineProject project, final RawDataFile raw[],
      PeakListRow row, PeakListRow row2) {
    // filter min samples in all
    if (minFInSamples.isGreaterZero()) {
      int n = 0;
      for (RawDataFile file : raw) {
        Feature a = row.getPeak(file);
        Feature b = row2.getPeak(file);
        if (a != null && a.getHeight() >= minFeatureHeight && b != null
            && b.getHeight() >= minFeatureHeight) {
          n++;
        }
      }
      // stop if <n
      if (!minFInSamples.checkGreaterEqualMax(raw.length, n))
        return false;
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero())
      return true;

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<String, MutableInt>();
    for (RawDataFile file : raw) {
      Feature a = row.getPeak(file);
      Feature b = row2.getPeak(file);
      if (a != null && a.getHeight() >= minFeatureHeight && b != null
          && b.getHeight() >= minFeatureHeight) {
        String sgroup = sgroupOf(project, file);

        MutableInt count = counter.get(sgroup);
        if (count == null) {
          // new counter with total N for group size
          counter.put(sgroup, new MutableInt(sgroupSize.get(sgroup)));
        } else {
          count.increment();
        }
      }
    }
    // only needs a minimum number of features in at least one sample group
    // only go on if feature was present in X % of the samples
    for (MutableInt v : counter.values()) {
      // at least one
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value))
        return true;
    }
    // no fit
    return false;
  }

  /**
   * gets called to initialise group variables
   * 
   * @param peakList
   */
  public void setSampleGroups(MZmineProject project, PeakList peakList, String groupingParameter) {
    if (groupingParameter == null || groupingParameter.length() == 0) {
      this.sgroupSize = null;
      setGroupFilterEnabled(false);
    } else {
      sgroupSize = new HashMap<String, Integer>();
      UserParameter<?, ?> params[] = project.getParameters();
      for (UserParameter<?, ?> p : params) {
        if (groupingParameter.equals(p.getName())) {
          // save parameter for sample groups
          sgroupPara = p;
          break;
        }
      }
      int max = 0;
      // calc size of sample groups
      for (RawDataFile file : peakList.getRawDataFiles()) {
        String parameterValue = sgroupOf(project, file);

        Integer v = sgroupSize.get(parameterValue);
        int val = v == null ? 0 : v;
        sgroupSize.put(parameterValue, val + 1);
        if (val + 1 > max)
          max = val + 1;
      }

      setGroupFilterEnabled(!sgroupSize.isEmpty());
    }
  }

  /**
   * 
   * @param file
   * @return sample group value of raw file
   */
  private String sgroupOf(MZmineProject project, RawDataFile file) {
    return String.valueOf(project.getParameterValue(sgroupPara, file));
  }

  class MutableInt {
    int total;
    int value = 1; // note that we start at 1 since we're counting

    MutableInt(int total) {
      this.total = total;
    }

    public void increment() {
      ++value;
    }

    public int get() {
      return value;
    }
  }

  /**
   * Size map
   * 
   * @return
   */
  public HashMap<String, Integer> getGroupSizeMap() {
    return sgroupSize;
  }

  /**
   * Group parameter
   * 
   * @return
   */
  public UserParameter<?, ?> getGroupParam() {
    return sgroupPara;
  }
}
