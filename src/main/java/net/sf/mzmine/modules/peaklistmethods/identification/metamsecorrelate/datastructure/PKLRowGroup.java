package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import org.jfree.chart.ChartColor;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.similarity.R2RMS2Similarity;

public class PKLRowGroup extends RowGroup implements MS2SimilarityProviderGroup {
  // colors
  public static final Paint[] colors = ChartColor.createDefaultPaintArray();
  // correlation data of all rows to this group
  private R2GroupCorrelationData[] corr;
  // MS/MS similarity map
  private R2RMap<R2RMS2Similarity> ms2SimilarityMap;

  public PKLRowGroup(final RawDataFile[] raw, int groupID) {
    super(raw, groupID);
  }

  /**
   * Recalculates all stats for this group
   * 
   * @param corrMap
   */
  public void recalcGroupCorrelation(R2RCorrMap corrMap) {
    // init
    corr = new R2GroupCorrelationData[this.size()];

    // test all rows against all other rows
    for (int i = 0; i < this.size(); i++) {
      List<R2RFullCorrelationData> rowCorr = new ArrayList<>();
      PeakListRow testRow = this.get(i);
      for (int k = 0; k < this.size(); k++) {
        if (i != k) {
          R2RCorrelationData r2r = corrMap.get(testRow, this.get(k));
          // TODO this should always be a full - otherwise do not group!
          if (r2r instanceof R2RFullCorrelationData)
            rowCorr.add((R2RFullCorrelationData) r2r);
        }
      }
      // create group corr object
      corr[i] = new R2GroupCorrelationData(testRow, rowCorr, testRow.getBestPeak().getHeight());
    }
  }

  /**
   * correlation of each row to the group
   * 
   * @return
   */
  public R2GroupCorrelationData[] getCorr() {
    return corr;
  }

  /**
   * correlation of a row to the group
   * 
   * @return
   */
  public R2GroupCorrelationData getCorr(int row) {
    return corr[row];
  }

  public R2GroupCorrelationData getCorr(PeakListRow row) {
    if (row == null)
      return null;
    int index = indexOf(row);
    if (index != -1)
      return getCorr(index);
    return null;
  }

  @Override
  public R2RMap<R2RMS2Similarity> getMS2SimilarityMap() {
    return ms2SimilarityMap;
  }

  public void setMS2SimilarityMap(R2RMap<R2RMS2Similarity> map) {
    this.ms2SimilarityMap = map;
  }

}
