package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.AdductType;

public class TestAdducts {

  public static final AdductType H = new AdductType("H", 1.007276, 1, 1);
  private static final AdductType NA = new AdductType("Na", 22.989218, 1, 1);
  private static final AdductType NH4 = new AdductType("NH4", 18.033823, 1, 1);
  private static final AdductType K = new AdductType("K", 38.963158, 1, 1);
  private static final AdductType FE = new AdductType("Fe", 55.933840, 2, 1);
  private static final AdductType CA = new AdductType("Ca", 39.961493820, 2, 1);
  // maybe i dont have to add these
  private static final AdductType NA2 = new AdductType("[M-H+Na2]+", 44.971160, 1, 1);
  private static final AdductType K2 = new AdductType("[M-H+K2]+", 76.919040, 1, 1);

  // NEGATIVE
  public static final AdductType H_NEG = new AdductType("H", -1.007276, -1, 1);
  private static final AdductType CL = new AdductType("Cl", 34.969401, -1, 1);
  private static final AdductType BR = new AdductType("Br", 78.918886, -1, 1);
  private static final AdductType FA = new AdductType("FA", 44.99820285, -1, 1);

  // modifications
  private static final AdductType MEOH = new AdductType("MeOH", 32.026215, 0, 1);
  private static final AdductType HFA = new AdductType("HFA", 46.005479, 0, 1);
  private static final AdductType HAc = new AdductType("HAc", 60.021129, 0, 1);
  private static final AdductType ACN = new AdductType("ACN", 41.026549, 0, 1);
  private static final AdductType H2O = new AdductType("H2O", -18.010565, 0, 1);
  private static final AdductType NH3 = new AdductType("NH3", -17.026549, 0, 1);
  private static final AdductType CO = new AdductType("CO", -27.994915, 0, 1);
  private static final AdductType CO2 = new AdductType("CO2", -43.989829, 0, 1);

  public static void main(String[] args) {
    AdductType[] pos = {H, NA};
    AdductType[] pos2 = {H, NA, FE};
    AdductType[] neg = {H_NEG, CL};
    AdductType[] neg2 = {H_NEG, NA};
    AdductType[] mod = {H2O};
    AdductType[] mod2 = {H2O, CO2};

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
