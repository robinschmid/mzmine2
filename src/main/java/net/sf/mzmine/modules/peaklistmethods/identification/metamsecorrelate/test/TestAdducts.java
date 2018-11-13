package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.AdductType;

public class TestAdducts {

  public static final AdductType H = new AdductType("H", 1.007276, 1);
  private static final AdductType NA = new AdductType("Na", 22.989218, 1);

  public static void main(String[] args) {
    AdductType[] pos = {H, NA};

    // System.out.println("POS");
    // MetaMSEcorrelateTask t = new MetaMSEcorrelateTask(true, pos, mod, 2, 2, 2, 2);
    // System.out.println("POS");
    // MetaMSEcorrelateTask t3 = new MetaMSEcorrelateTask(true, pos2, mod2, 1, 2, 2, 2);
    // System.out.println("NEG");
    // MetaMSEcorrelateTask t4 = new MetaMSEcorrelateTask(false, neg2, mod, 1, 2, 2, 2);
    // System.out.println("NEG");
    // MetaMSEcorrelateTask t2 = new MetaMSEcorrelateTask(false, neg, mod2, 1, 2, 2, 2);
  }

}
