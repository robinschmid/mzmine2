package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

public enum EdgeAtt {

  TYPE, LABEL, GNPS_SCORE, DIFF_SCORE, SIM_SCORE, DIFF_N, SIM_N, SCORE;
  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
