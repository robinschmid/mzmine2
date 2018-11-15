package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.similarity.R2RMS2Similarity;

public interface MS2SimilarityProviderGroup {

  /**
   * A map for row-2-row MS2 similarity
   * 
   * @return
   */
  public R2RMap<R2RMS2Similarity> getMS2SimilarityMap();
}
