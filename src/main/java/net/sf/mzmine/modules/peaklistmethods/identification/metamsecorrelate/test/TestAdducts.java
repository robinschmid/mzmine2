package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;

public class TestAdducts { 

	public static final ESIAdductType H = new ESIAdductType("H", 1.007276, 1, 1);
	private static final ESIAdductType NA = new ESIAdductType("Na", 22.989218, 1, 1);
	private static final ESIAdductType NH4 = new ESIAdductType("NH4", 18.033823, 1, 1);
	private static final ESIAdductType K = new ESIAdductType("K", 38.963158, 1, 1);
	private static final ESIAdductType FE = new ESIAdductType("Fe", 55.933840, 2, 1);
	private static final ESIAdductType CA = new ESIAdductType("Ca", 39.961493820, 2, 1);
	// maybe i dont have to add these
	private static final ESIAdductType NA2 = new ESIAdductType("[M-H+Na2]+", 44.971160, 1, 1);
	private static final ESIAdductType K2 = new ESIAdductType("[M-H+K2]+", 76.919040, 1, 1);

	// NEGATIVE
	public static final ESIAdductType H_NEG = new ESIAdductType("H", -1.007276, -1, 1);
	private static final ESIAdductType CL = new ESIAdductType("Cl", 34.969401, -1, 1);
	private static final ESIAdductType BR = new ESIAdductType("Br", 78.918886, -1, 1);
	private static final ESIAdductType FA = new ESIAdductType("FA", 44.99820285, -1, 1);
	
	// modifications 
	private static final ESIAdductType MEOH = new ESIAdductType("MeOH", 32.026215, 0, 1);
	private static final ESIAdductType HFA = new ESIAdductType("HFA", 46.005479, 0, 1);
	private static final ESIAdductType HAc = new ESIAdductType("HAc", 60.021129, 0, 1);
	private static final ESIAdductType ACN = new ESIAdductType("ACN", 41.026549, 0, 1);
	private static final ESIAdductType H2O = new ESIAdductType("H2O", -18.010565, 0, 1);
	private static final ESIAdductType NH3 = new ESIAdductType("NH3", -17.026549, 0, 1);
	private static final ESIAdductType CO = new ESIAdductType("CO", -27.994915, 0, 1);
	private static final ESIAdductType CO2 = new ESIAdductType("CO2", -43.989829, 0, 1);
	
	public static void main(String[] args) {
		ESIAdductType[] pos = {H, NA};
		ESIAdductType[] pos2 = {H, NA,FE};
		ESIAdductType[] neg = {H_NEG, CL};
		ESIAdductType[] neg2 = {H_NEG, NA};
		ESIAdductType[] mod = {H2O};
		ESIAdductType[] mod2 = {H2O, CO2};

		System.out.println("POS");
		MetaMSEcorrelateTask t = new MetaMSEcorrelateTask(true, pos, mod, 2, 2, 2, 2);
		System.out.println("POS");
		MetaMSEcorrelateTask t3 = new MetaMSEcorrelateTask(true, pos2, mod2, 1, 2, 2, 2);
		System.out.println("NEG");
		MetaMSEcorrelateTask t4 = new MetaMSEcorrelateTask(false, neg2, mod, 1, 2, 2, 2);
		System.out.println("NEG");
		MetaMSEcorrelateTask t2 = new MetaMSEcorrelateTask(false, neg, mod2, 1, 2, 2, 2);
	}

}
