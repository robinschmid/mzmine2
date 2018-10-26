package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import com.google.common.util.concurrent.AtomicDouble;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;

/**
 * Correlation of row 2 row
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrMap extends ConcurrentHashMap<String, R2RCorrelationData> {
  private static final Logger LOG = Logger.getLogger(R2RCorrMap.class.getName());
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private RTTolerance rtTolerance;
  private MinimumFeatureFilter minFFilter;

  public R2RCorrMap(RTTolerance rtTolerance, MinimumFeatureFilter minFFilter) {
    //
    this.rtTolerance = rtTolerance;
    this.minFFilter = minFFilter;
  }

  public RTTolerance getRtTolerance() {
    return rtTolerance;
  }

  public MinimumFeatureFilter getMinFeatureFilter() {
    return minFFilter;
  }

  /**
   * Redirects to Map.put
   * 
   * @param row
   * @param row2
   * @param corr
   */
  public void add(PeakListRow row, PeakListRow row2, R2RCorrelationData corr) {
    this.put(toKey(row, row2), corr);
  }

  public R2RCorrelationData get(PeakListRow row, PeakListRow row2) {
    return get(toKey(row, row2));
  }

  public Stream<R2RFullCorrelationData> streamCorrData() {
    return values().stream().filter(r2r -> r2r instanceof R2RFullCorrelationData)
        .map(r2r -> (R2RFullCorrelationData) r2r);
  }

  public Stream<java.util.Map.Entry<String, R2RCorrelationData>> streamCorrDataEntries() {
    return entrySet().stream().filter(e -> e.getValue() instanceof R2RFullCorrelationData);
  }


  /**
   * Create list of correlated rows
   * 
   * 
   * @param rows
   * @param stageProgress can be null. points to the progress
   * 
   * @return
   */
  public PKLRowGroupList createCorrGroupsNew(PeakList pkl, double minShapeR,
      AtomicDouble stageProgress) {
    LOG.info("Corr: Creating correlation groups");

    try {
      PKLRowGroupList groups = new PKLRowGroupList();
      HashMap<Integer, PKLRowGroup> used = new HashMap<>();
      HashMap<Integer, Integer> usedi = new HashMap<>();
      int current = 0;

      RawDataFile[] raw = pkl.getRawDataFiles();

       this.entrySet().stream().sorted(new Comparator<Entry<String, R2RCorrelationData>>() {
         @Override
        public int compare(Entry<String, R2RCorrelationData> ea,
            Entry<String, R2RCorrelationData> eb) {
           R2RCorrelationData a = ea.getValue();
           R2RCorrelationData b = eb.getValue();
           if(!(a instanceof R2RFullCorrelationData || b instanceof R2RFullCorrelationData))
             return 0;
           else if(a instanceof R2RFullCorrelationData && b instanceof R2RFullCorrelationData) {
             if(!a.hasFeatureShapeCorrelation() && !b.hasFeatureShapeCorrelation())
               return 0;
             else if(a.hasFeatureShapeCorrelation() || a.getAvgPeakShapeR()>b.getAvgPeakShapeR())
               return 1;
       }
       else if(a instanceof R2RFullCorrelationData)
         return 1;
       else if(b instanceof R2RFullCorrelationData)
         return -1;
       throw new Exception("Type not handled for correlation data sorting");
       }
       })
       
       
      // add all connections
      Iterator<Entry<String, R2RCorrelationData>> entries = this.entrySet().iterator();

      int c = 0;
      while (entries.hasNext()) {
        Entry<String, R2RCorrelationData> e = entries.next();

        R2RCorrelationData r2r = e.getValue();
        if (r2r instanceof R2RFullCorrelationData) {
          R2RFullCorrelationData data = (R2RFullCorrelationData) r2r;
          if (data.hasFeatureShapeCorrelation() && data.getAvgPeakShapeR() >= minShapeR) {

            int[] ids = toKeyIDs(e.getKey());
            // already added?
            PKLRowGroup group = used.get(ids[0]);
            PKLRowGroup group2 = used.get(ids[1]);
            // merge groups if both present
            if (group != null && group2 != null && group.getGroupID() != group2.getGroupID()) {
              // copy all to group1 and remove g2
              for (int g2 = 0; g2 < group2.size(); g2++) {
                PeakListRow r = group2.get(g2);
                group.add(r);
                used.put(r.getID(), group);
              }
              groups.remove(group2);
            } else if (group == null && group2 == null) {
              // create new group with both rows
              group = new PKLRowGroup(raw, groups.size());
              group.add(pkl.findRowByID(ids[0]));
              group.add(pkl.findRowByID(ids[1]));
              groups.add(group);
              // mark as used
              used.put(ids[0], group);
              used.put(ids[1], group);
            } else if (group2 == null) {
              group.add(pkl.findRowByID(ids[1]));
              used.put(ids[1], group);
            } else if (group == null) {
              group2.add(pkl.findRowByID(ids[0]));
              used.put(ids[0], group2);
            }
          }
        }
        // report back progress
        c++;
        if (stageProgress != null)
          stageProgress.addAndGet(1d / this.size());
      }
      // sort by retention time
      groups.sortByRT();

      // reset index
      for (int i = 0; i < groups.size(); i++)
        groups.get(i).setGroupID(i);

      LOG.info("Corr: DONE: Creating correlation groups");
      return groups;
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error while creating groups", e);
      return null;
    }
  }

  /**
   * Create list of correlated rows TODO delete
   * 
   * @param rows
   * @param stageProgress can be null. points to the progress
   * 
   * @return
   */
  public PKLRowGroupList createCorrGroups(PeakList pkl, double minShapeR,
      AtomicDouble stageProgress) {
    LOG.info("Corr: Creating correlation groups");

    try {
      PKLRowGroupList groups = new PKLRowGroupList();
      HashMap<Integer, PKLRowGroup> used = new HashMap<>();
      HashMap<Integer, Integer> usedi = new HashMap<>();
      int current = 0;

      RawDataFile[] raw = pkl.getRawDataFiles();
      // add all connections
      Iterator<Entry<String, R2RCorrelationData>> entries = this.entrySet().iterator();
      int c = 0;
      while (entries.hasNext()) {
        Entry<String, R2RCorrelationData> e = entries.next();

        R2RCorrelationData r2r = e.getValue();
        if (r2r instanceof R2RFullCorrelationData) {
          R2RFullCorrelationData data = (R2RFullCorrelationData) r2r;
          if (data.hasFeatureShapeCorrelation() && data.getAvgPeakShapeR() >= minShapeR) {

            int[] ids = toKeyIDs(e.getKey());
            // already added?
            PKLRowGroup group = used.get(ids[0]);
            PKLRowGroup group2 = used.get(ids[1]);
            // merge groups if both present
            if (group != null && group2 != null && group.getGroupID() != group2.getGroupID()) {
              // copy all to group1 and remove g2
              for (int g2 = 0; g2 < group2.size(); g2++) {
                PeakListRow r = group2.get(g2);
                group.add(r);
                used.put(r.getID(), group);
              }
              groups.remove(group2);
            } else if (group == null && group2 == null) {
              // create new group with both rows
              group = new PKLRowGroup(raw, groups.size());
              group.add(pkl.findRowByID(ids[0]));
              group.add(pkl.findRowByID(ids[1]));
              groups.add(group);
              // mark as used
              used.put(ids[0], group);
              used.put(ids[1], group);
            } else if (group2 == null) {
              group.add(pkl.findRowByID(ids[1]));
              used.put(ids[1], group);
            } else if (group == null) {
              group2.add(pkl.findRowByID(ids[0]));
              used.put(ids[0], group2);
            }
          }
        }
        // report back progress
        c++;
        if (stageProgress != null)
          stageProgress.addAndGet(1d / this.size());
      }
      // sort by retention time
      groups.sortByRT();

      // reset index
      for (int i = 0; i < groups.size(); i++)
        groups.get(i).setGroupID(i);

      LOG.info("Corr: DONE: Creating correlation groups");
      return groups;
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error while creating groups", e);
      return null;
    }
  }

  /**
   * Key as lowID,highID
   * 
   * @param row
   * @param row2
   * @return
   */
  public static String toKey(PeakListRow row, PeakListRow row2) {
    int id = row.getID();
    int id2 = row2.getID();
    return Math.min(id, id2) + "," + Math.max(id, id2);
  }

  /**
   * The two row IDs the first is always the lower one
   * 
   * @param key
   * @return
   */
  public static int[] toKeyIDs(String key) {
    return Arrays.stream(key.split(",")).mapToInt(Integer::parseInt).toArray();
  }
}
