package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual;

public enum NodeAtt {

  TYPE, RT, MZ, ID, INTENSITY, FORMULA, NEUTRAL_MASS, CHARGE, ION_TYPE, MS2_VERIFICATION, LABEL, NET_ID, GROUP_ID;
  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
