package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.AdductType;

public class TestESIadd {

  public static void main(String[] args) {


    final AdductType H = new AdductType("H", 1.007276, 1, 1);
    final AdductType NA = new AdductType("Na", 22.989218, 1, 1);
    final AdductType FE = new AdductType("Fe", 55.933840, 2, 1);
    // maybe i dont have to add these

    // NEGATIVE
    final AdductType H_NEG = new AdductType("H", -1.007276, -1, 1);
    final AdductType CL = new AdductType("Cl", 34.969401, -1, 1);

    final AdductType H2O = new AdductType("H2O", -18.010565, 0, 1);
    final AdductType CO2 = new AdductType("CO2", -43.989829, 0, 1);

    AdductType[] pos = {H};
    AdductType[] pos2 = {H, NA, FE};
    AdductType[] neg = {H_NEG, CL};
    AdductType[] neg2 = {H_NEG, NA};
    AdductType[] mod = {};
    AdductType[] mod2 = {H2O, CO2};

    // System.out.println("POS");
    // MetaMSEcorrelateTask t = new MetaMSEcorrelateTask(true, pos, mod2, 1, 1, 2, 2);
    // System.out.println("POS");
    // MetaMSEcorrelateTask t3 = new MetaMSEcorrelateTask(true, pos2, mod2, 1, 2, 2, 2);
    // System.out.println("NEG");
    // MetaMSEcorrelateTask t4 = new MetaMSEcorrelateTask(false, neg2, mod, 1, 2, 2, 2);
    // System.out.println("NEG");
    // MetaMSEcorrelateTask t2 = new MetaMSEcorrelateTask(false, neg, mod, 1, 2, 2, 0);
  }

}
