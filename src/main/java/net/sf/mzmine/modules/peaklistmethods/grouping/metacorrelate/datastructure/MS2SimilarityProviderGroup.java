package net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.R2RMS2Similarity;

public abstract class MS2SimilarityProviderGroup extends RowGroup {

  public MS2SimilarityProviderGroup(RawDataFile[] raw, int groupID) {
    super(raw, groupID);
  }

  /**
   * A map for row-2-row MS2 similarity
   * 
   * @return
   */
  public abstract R2RMap<R2RMS2Similarity> getMS2SimilarityMap();

  /**
   * Similarity map for row-2-row MS2 comparison
   * 
   * @param map
   * @return
   */
  public abstract void setMS2SimilarityMap(R2RMap<R2RMS2Similarity> map);
}
