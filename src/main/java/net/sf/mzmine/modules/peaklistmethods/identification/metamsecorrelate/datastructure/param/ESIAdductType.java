package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param;

import java.util.Arrays;
import java.util.Vector;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;

public class ESIAdductType implements Comparable<ESIAdductType> {
	// values
	// use combinations of X adducts (2H++; -H+Na2+) and modifications
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
	private static final ESIAdductType ISOPROP = new ESIAdductType("IsoProp", 60.058064, 0, 1);
	// isotopes
	public static final ESIAdductType C13 = new ESIAdductType("(13C)", 1.003354838, 0, 1);
	
	// default values
    private static final ESIAdductType[] DEFAULT_VALUES_POSITIVE = { H, NA, K, NH4, K, CA, FE };
    private static final ESIAdductType[] DEFAULT_VALUES_NEGATIVE = { H_NEG, NA, CL, BR, FA };
    // default modifications
    private static final ESIAdductType[] DEFAULT_VALUES_MODIFICATIONS = { H2O, NH3, CO, CO2, HFA, HAc, MEOH, ACN, ISOPROP };
    // isotopes
    private static final ESIAdductType[] DEFAULT_VALUES_ISOTOPES = { C13 };
	
    
    // data 
	private String name;
	private String parsedName;
	private double massDifference;
	// charge and count of molecules
	private int charge, molecules; 
	private ESIAdductType[] modification;
	private ESIAdductType[] adducts;
	/**
	 * 
	 * @param name
	 * @param massDifference mass difference (for single charge, Molecule) for example M to M+H+ (1.0072) M to M+Na+ (22.9892)
	 * @param charge negative for negatives
	 * @param molecules count of molecules in a cluster
	 */
	/**
	 * copy of adduct
	 * @param a
	 */
	public ESIAdductType(ESIAdductType a) {
		super();
		this.modification = a.getModification();
		this.massDifference = a.massDifference;
		this.charge = a.charge;
		this.molecules = a.molecules; 
		this.name = a.name;
		this.adducts = a.getAdducts();
		this.parsedName = parseName();
	} 
	/**
	 * new raw adduct
	 * @param name
	 * @param massDifference
	 * @param charge
	 * @param molecules
	 */
	public ESIAdductType(String name, double massDifference, int charge, int molecules) {
		super();
		this.name = name;
		this.modification = null;
		this.massDifference = massDifference;
		this.charge = charge;
		this.molecules = molecules; 
		this.adducts = new ESIAdductType[1];
		this.adducts[0] = this;
		this.parsedName = parseName();
	} 
	/**
	 * fast creation of combined adducts
	 * @param adduct
	 */
	public ESIAdductType(final ESIAdductType[] adduct) {
		// all mods
		Vector<ESIAdductType> mod = new Vector<ESIAdductType>(); 
		for(ESIAdductType a : adduct)
			if(a.getModification()!=null)
				for(ESIAdductType m : a.getModification())
					mod.add(m); 
		
		if(mod.size()>0) modification = mod.toArray(new ESIAdductType[mod.size()]);
		else modification = null;
		// adducts
		this.adducts = adduct;
		double md = 0;
		int z=0;
		int mol=0;
		for(int i=0; i<adduct.length; i++) {
			ESIAdductType a = adduct[i];
			md += a.getMassDifference();
			z += a.getCharge();
			mol = a.getMolecules();
		}  
		charge = z;
		molecules = mol;
		massDifference = md;
		this.parsedName = parseName();
	}
	/**
	 * for combining two adducts
	 * @param a1
	 * @param a2
	 */
	public ESIAdductType(final ESIAdductType a1, final ESIAdductType a2) { 
		name = "";
		// add modification
		int length = 0;
		if(a1.getModification()!=null) length += a1.getModification().length;
		if(a2.getModification()!=null) length += a2.getModification().length;
		if(length!=0) {
			this.modification = new ESIAdductType[length]; 
			int c=0;
			for(c=0; a1.getModification()!=null && c<a1.getModification().length; c++)
				modification[c] = a1.getModification()[c];
			for(int i=0; a2.getModification()!=null && i<a2.getModification().length; i++)
				modification[c+i] = a2.getModification()[i];
		}
		else this.modification = null;
		// all adducts
		Vector<ESIAdductType> ad = new Vector<ESIAdductType>(); 
		for(ESIAdductType n : a1.getAdducts())
			ad.add(n);
		for(ESIAdductType n : a2.getAdducts())
			ad.add(n);
		adducts = ad.toArray(new ESIAdductType[ad.size()]);
		charge = a1.getCharge()+a2.getCharge();
		molecules = a1.getMolecules();
		massDifference = a1.getMassDifference()+a2.getMassDifference();
		this.parsedName = parseName();
	}
	/**
	 * for adding a modifcation
	 * @param a
	 * @param mod
	 * @return
	 */
	public static ESIAdductType createModified(final ESIAdductType a, final ESIAdductType mod) { 
		ESIAdductType na = new ESIAdductType(a);
		// modification are saved in adducts for combined mods
		ESIAdductType[] realMod = mod.getAdducts();
		// add modification
		int length = realMod.length;
		if(a.getModification()!=null) length += a.getModification().length;
		na.modification = new ESIAdductType[length]; 
		for(int i=0; i<realMod.length; i++) {
			na.modification[i] = realMod[i];
			na.massDifference += realMod[i].getMassDifference();
		}
		if(a.getModification()!=null)
			for(int i=0; i<na.modification.length-realMod.length; i++)
				na.modification[i+realMod.length] = a.getModification()[i]; 
		// parse name
		na.parsedName = na.parseName();
		return na;
	}

	public ESIAdductType[] getModification() {
		return modification;
	}
	/**
	 * 
	 * @return array of names
	 */
	public String[] getNames() {
		String[] names = new String[adducts.length];
		for(int i=0; i<names.length; i++) 
			names[i] = adducts[i].getRawName();
		return names;
	}
	public String[] getModNames() {
		String[] names = new String[modification.length];
		for(int i=0; i<names.length; i++) 
			names[i] = modification[i].getRawName();
		return names;
	}
	
	public String getRawName() {
		return name;
	}
	/**
	 * 
	 * @return parsed name (f.e. -2H+Na)
	 */
	public String getName() {
		return parsedName;
	}
	public String parseName() { 

		String s = null;
		int counter = 0;
		String add, counterS;
		
		String mod = "";
		if(modification!=null) {
			Arrays.sort(modification);

			for(int i=0; i<modification.length; i++) {
				String cs = modification[i].getRawName();
				if(s==null) {
					s = cs;
					counter = 1;
				}
				else if(s==cs) counter++;
				else {
					add = (modification[i-1].getMassDifference()<0? "-" : "+");
					counterS = counter>1? String.valueOf(counter) : "";
					mod += add + counterS + s;
					s = cs;
					counter = 1;
				}
			} 
			add = (modification[modification.length-1].getMassDifference()<0? "-" : "+");
			counterS = counter>1? String.valueOf(counter) : "";
			mod += add + counterS + s;
		}

		s = null;
		Arrays.sort(adducts);
		String nname = "";
		for(int i=0; i<adducts.length; i++) {
			String cs = adducts[i].getRawName();
			if(s==null) {
				s = cs;
				counter = 1;
			}
			else if(s==cs) counter++;
			else {
				add = (adducts[i-1].getMassDifference()<0? "-" : "+");
				counterS = counter>1? String.valueOf(counter) : "";
				nname += add + counterS + s;
				s = cs;
				counter = 1;
			}
		} 
		add = (adducts[adducts.length-1].getMassDifference()<0? "-" : "+");
		counterS = counter>1? String.valueOf(counter) : "";
		nname += add + counterS + s;
		
		return mod+nname;
	}
	public double getMassDifference() {
		return massDifference;
	}
	public int getCharge() {
		return charge;
	}
	public int getMolecules() {
		return molecules;
	} 
	
	/**
	 * either adds a new identity or adds row2 as an identifier to an existing identity
	 * @param row1 row to add the identity to
	 * @param row2 identified by this row
	 */
	public void addAdductIdentityToRow(PeakListRow row1, PeakListRow row2) {
		boolean added = false;
		for(PeakIdentity id : row1.getPeakIdentities()) {
			if(ESIAdductIdentity.class.isInstance(id)) {
				ESIAdductIdentity a = (ESIAdductIdentity) id;
				// equals? add row2 to partners
				if(a.equalsAdduct(this)) {
					a.addPartnerRow(row2);
					added = true;
					break;
				}
			}
		}
		if(!added)
			row1.addPeakIdentity(new ESIAdductIdentity(row2, this), false);
	}

	/**
	 * Get the default adducts.
	 *
	 * @return the list of default adducts.
	 */
	public static ESIAdductType[] getDefaultValuesPos() {
		return Arrays.copyOf(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_POSITIVE.length);
	}
	public static ESIAdductType[] getDefaultValuesNeg() {
		return Arrays.copyOf(DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_NEGATIVE.length);
	}
	public static ESIAdductType[] getDefaultModifications() {
		return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_MODIFICATIONS.length);
	}
	public static ESIAdductType[] getDefaultIsotopes() {
		return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_ISOTOPES.length);
	}

	@Override
	public boolean equals(final Object obj) {
		final boolean eq;
		if (obj instanceof ESIAdductType) {
			final ESIAdductType adduct = (ESIAdductType) obj;

			eq = adduct == this ||
					(sameMathDifference(adduct) 
					&& nameEquals(adduct)
					&& modsEqual(adduct));
		} else {
			eq = false;
		}
		return eq;
	}

	/**
	 * checks all sub/raw ESIAdductTypes
	 * @param a
	 * @return
	 */
	public boolean nameEquals(ESIAdductType a) { 
		boolean state = ((name != null && a.name != null 
				&& name.length()>0 && a.name.length()>0 && name.equals(a.name)));
		if(state)
			return true;
		// check all sub adducts
		if(adducts.length!=a.adducts.length)
			return false;
		
		// is already sorted?
		Arrays.sort(adducts);
		Arrays.sort(a.adducts);
		for(int i=0; i<adducts.length; i++) 
			if(!adducts[i].getRawName().equals(a.adducts[i].getRawName()) 
					|| !adducts[i].sameMathDifference(a.adducts[i]))
				return false;
		return true;
	}
	
	/**
	 * checks if all modification are equal
	 * @param a
	 * @return
	 */
	public boolean modsEqual(ESIAdductType a) {  
		if(!hasMods() && !a.hasMods())
			return true;
		// check all sub adducts
		if(modification.length!=a.modification.length)
			return false;
		// is already sorted?
		Arrays.sort(modification);
		Arrays.sort(a.modification);
		for(int i=0; i<modification.length; i++) 
			if(!modification[i].getRawName().equals(a.modification[i].getRawName()) 
					|| !modification[i].sameMathDifference(a.modification[i]))
				return false;
		return true;
	}
	
	
	public String toString() {
		String z = Math.abs(charge) + (charge<0? "-" : "+");
		if(charge==0)
			z="";
		// molecules
		String mol = molecules>1? String.valueOf(molecules) : "";
		return "["+mol+"M"+parsedName+"]"+z;
	}
	
	public String getMassDiffString() {
		return MZmineCore.getConfiguration().getMZFormat()
				.format(massDifference) + " m/z";
	} 

	public boolean sameMathDifference(ESIAdductType adduct) { 
		return Math.abs(massDifference-adduct.massDifference) <0.000001 
				&& charge == adduct.charge 
				&& molecules == adduct.molecules;
	}

	public int getAbsCharge() { 
		return Math.abs(charge);
	}
	public ESIAdductType[] getAdducts() {
		return adducts;
	}
	public void setAdducts(ESIAdductType[] adducts) {
		this.adducts = adducts;
	}
	public void setMolecules(int i) {
		molecules = i;
	}
	public boolean hasMods() {
		return modification!=null && modification.length>0;
	}
	/**
	 * sorting
	 */
	@Override
	public int compareTo(ESIAdductType a) { 
		int i = this.getRawName().compareTo(a.getRawName());
		if(i==0) {
			double md1 = getMassDifference();
			double md2 = a.getMassDifference();
			i = md1==md2? 0 : md1<md2? -1 : 1;
		}
		return i;
	}
	/**
	 * is a modification of parameter adduct?
	 * only if all adducts are the same, mass difference must be different
	 * ONLY if this is a mod of parameter adduct
	 * @param adduct
	 * @return
	 */
	public boolean isModificationOf(ESIAdductType adduct) {
		return !sameMathDifference(adduct) && molecules==adduct.molecules && charge==adduct.charge
				&& nameEquals(adduct) && (this.hasMods()&& (!adduct.hasMods() 
				|| (adduct.modification.length<this.modification.length && adduct.modification[0].equals(this.modification[0]))));
	}
	/**
	 * is a modification of parameter adduct?
	 * only if all adducts are the same, mass difference must be different
	 * ALWAYS if one is a mod of the other
	 * @param adduct
	 * @return
	 */
	public boolean sameAdductsButModified(ESIAdductType adduct) {
		return !sameMathDifference(adduct) && molecules==adduct.molecules
				&& charge==adduct.charge && (adduct.hasMods() || this.hasMods()) && nameEquals(adduct);
	}
	
	/**
	 * subtracts the mods of the parameter adduct from this adduct
	 * @param adduct
	 * @return
	 */
	public ESIAdductType subtractMods(ESIAdductType adduct) {
		if(!adduct.hasMods())
			return new ESIAdductType(modification);
		else if(hasMods()){
			Vector<ESIAdductType> mods = new Vector<ESIAdductType>();
			for(int i=0; i<modification.length; i++)
				if(adduct.modification.length<=i || !modification[i].equals(adduct.modification[i]))
					mods.add(modification[i]);
			return new ESIAdductType(mods.toArray(new ESIAdductType[mods.size()]));
		}
		else return null;
	}
	/**
	 * 
	 * @return count of modification
	 */
	public int getModCount() {
		return modification==null? 0 : modification.length;
	}
	/**
	 * trys to calculate if this is a main adduct
	 * @return
	 */
	public boolean isMainAdduct(int maxAdducts) {
		// is a modification of >=2
		if(charge==0 && adducts.length>1)
			return false;
		
		return adducts.length<=maxAdducts && (!hasMods() 
				|| modification.length<=1);
	}
}
