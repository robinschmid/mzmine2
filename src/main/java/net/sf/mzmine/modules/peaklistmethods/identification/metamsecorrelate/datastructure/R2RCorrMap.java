package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.HashMap;
import net.sf.mzmine.datamodel.PeakListRow;

/**
 * Correlation of row 2 row
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrMap extends HashMap<String, RowCorrelationData> {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

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
   * Key as lowID,highID
   * 
   * @param row
   * @param row2
   * @return
   */
  public String toKey(PeakListRow row, PeakListRow row2) {
    int id = row.getID();
    int id2 = row2.getID();
    return Math.min(id, id2) + "," + Math.max(id, id2);
  }
}
