package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;

/**
 * Correlation of row 2 row
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrMap extends TreeMap<String, RowCorrelationData> {
  private static final Logger LOG = Logger.getLogger(R2RCorrMap.class.getName());
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public R2RCorrMap() {
    super(new Comparator<String>() {
      @Override
      public int compare(String s1, String s2) {
        String[] id1 = s1.split(",");
        String[] id2 = s2.split(",");
        int a1 = Integer.valueOf(id1[0]);
        int a2 = Integer.valueOf(id2[0]);
        int b1 = Integer.valueOf(id1[1]);
        int b2 = Integer.valueOf(id2[1]);

        int compareA = Integer.compare(a1, a2);
        return compareA != 0 ? compareA : Integer.compare(b1, b2);
      }
    });
  }

  /**
   * Redirects to Map.put
   * 
   * @param row
   * @param row2
   * @param corr
   */
  public void add(PeakListRow row, PeakListRow row2, RowCorrelationData corr) {
    this.put(toKey(row, row2), corr);
  }

  public RowCorrelationData get(PeakListRow row, PeakListRow row2) {
    return get(toKey(row, row2));
  }

  /**
   * Create list of AnnotationNetworks and set net ID
   * 
   * @param rows
   * @return
   */
  public PKLRowGroupList createCorrGroups(PeakList pkl) {
    LOG.info("Corr: Creating correlation groups");

    try {
      PKLRowGroupList groups = new PKLRowGroupList();
      HashMap<Integer, PKLRowGroup> used = new HashMap<>();

      RawDataFile[] raw = pkl.getRawDataFiles();
      // add all connections
      Iterator<Entry<String, RowCorrelationData>> entries = this.entrySet().iterator();
      while (entries.hasNext()) {
        Entry<String, RowCorrelationData> e = entries.next();
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
          group = new PKLRowGroup(raw, pkl.findRowByID(ids[0]), groups.size());
          group.add(pkl.findRowByID(ids[1]));
          groups.add(group);
          // mark as used
          used.put(ids[0], group);
          used.put(ids[1], group);
        } else if (group2 == null) {
          group.add(pkl.findRowByID(ids[1]));
          used.put(ids[1], group);
        } else {
          // group is null
          group2.add(pkl.findRowByID(ids[0]));
          used.put(ids[0], group2);
        }
      }
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
