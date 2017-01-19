package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.test;

import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;

public class TestESIadd {

	public static void main(String[] args) {
		

		final ESIAdductType H = new ESIAdductType("H", 1.007276, 1, 1);
		final ESIAdductType NA = new ESIAdductType("Na", 22.989218, 1, 1);
		final ESIAdductType FE = new ESIAdductType("Fe", 55.933840, 2, 1);
		// maybe i dont have to add these

		// NEGATIVE
		 final ESIAdductType H_NEG = new ESIAdductType("H", -1.007276, -1, 1);
		  final ESIAdductType CL = new ESIAdductType("Cl", 34.969401, -1, 1);
		
		  final ESIAdductType H2O = new ESIAdductType("H2O", -18.010565, 0, 1); 
		  final ESIAdductType CO2 = new ESIAdductType("CO2", -43.989829, 0, 1);
		
		ESIAdductType[] pos = {H};
		ESIAdductType[] pos2 = {H, NA,FE};
		ESIAdductType[] neg = {H_NEG, CL};
		ESIAdductType[] neg2 = {H_NEG, NA};
		ESIAdductType[] mod = {};
		ESIAdductType[] mod2 = {H2O, CO2};

		System.out.println("POS");
		MetaMSEcorrelateTask t = new MetaMSEcorrelateTask(true, pos, mod2, 1, 1, 2, 2);
		System.out.println("POS");
		MetaMSEcorrelateTask t3 = new MetaMSEcorrelateTask(true, pos2, mod2, 1, 2, 2, 2);
		System.out.println("NEG");
		MetaMSEcorrelateTask t4 = new MetaMSEcorrelateTask(false, neg2, mod, 1, 2, 2, 2);
		System.out.println("NEG");
		MetaMSEcorrelateTask t2 = new MetaMSEcorrelateTask(false, neg, mod, 1, 2, 2, 0);
	}

}
