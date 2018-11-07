package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.similarity;

public class MS2Similarity {
  private double cosine;
  private int overlap;

  public MS2Similarity(double cosine, int overlap) {
    super();
    this.cosine = cosine;
    this.overlap = overlap;
  }

  public int getOverlap() {
    return overlap;
  }

  public double getCosine() {
    return cosine;
  }

}
