package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.mzmine.main.MZmineCore;

public class IonType implements Comparable<IonType> {

  private AdductType adduct;
  private NeutralModification[] mod;
  private int molecules = 1;
  private String parsedName;
  private double massDifference;

  /**
   * 
   * @param name
   * @param massDifference mass difference (for single charge, Molecule) for example M to M+H+
   *        (1.0072) M to M+Na+ (22.9892)
   * @param charge negative for negatives
   * @param molecules count of molecules in a cluster
   */
  /**
   * copy of adduct
   * 
   * @param a
   */
  public IonType(IonType a) {
    super();
    if (a.getModification() != null)
      this.modification = a.getModification().clone();
    this.massDifference = a.massDifference;
    this.charge = a.charge;
    this.molecules = a.molecules;
    this.name = a.name;
    this.adducts = a.getAdducts().clone();
    this.parsedName = parseName();
  }

  /**
   * new raw adduct
   * 
   * @param name
   * @param massDifference
   * @param charge
   * @param molecules
   */
  public IonType(String name, double massDifference, int charge, int molecules) {
    super();
    this.name = name;
    this.modification = null;
    this.massDifference = massDifference;
    this.charge = charge;
    this.molecules = molecules;
    this.adducts = new IonType[1];
    this.adducts[0] = this;
    this.parsedName = parseName();
  }

  /**
   * new raw adduct
   * 
   * @param name
   * @param massDifference
   * @param charge
   */
  public IonType(String name, double massDifference, int charge) {
    this(name, massDifference, charge, 1);
  }

  /**
   * fast creation of combined adducts
   * 
   * @param adduct
   */
  public IonType(final IonType[] adduct) {
    // all mods
    List<IonType> mod = new ArrayList<>();
    for (IonType a : adduct)
      if (a.getModification() != null)
        for (IonType m : a.getModification())
          mod.add(m);

    if (mod.size() > 0)
      modification = mod.toArray(new IonType[mod.size()]);
    else
      modification = null;
    // adducts
    this.adducts = adduct;
    double md = 0;
    int z = 0;
    int mol = 0;
    for (int i = 0; i < adduct.length; i++) {
      IonType a = adduct[i];
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
   * 
   * @param a1
   * @param a2
   */
  public IonType(final IonType a1, final IonType a2) {
    name = "";
    // add modification
    int length = 0;
    if (a1.getModification() != null)
      length += a1.getModification().length;
    if (a2.getModification() != null)
      length += a2.getModification().length;
    if (length != 0) {
      this.modification = new IonType[length];
      int c = 0;
      for (c = 0; a1.getModification() != null && c < a1.getModification().length; c++)
        modification[c] = a1.getModification()[c];
      for (int i = 0; a2.getModification() != null && i < a2.getModification().length; i++)
        modification[c + i] = a2.getModification()[i];
    } else
      this.modification = null;
    // all adducts
    List<IonType> ad = new ArrayList<IonType>();
    for (IonType n : a1.getAdducts())
      ad.add(n);
    for (IonType n : a2.getAdducts())
      ad.add(n);
    adducts = ad.toArray(new IonType[ad.size()]);
    charge = a1.getCharge() + a2.getCharge();
    molecules = a1.getMolecules();
    massDifference = a1.getMassDifference() + a2.getMassDifference();
    this.parsedName = parseName();
  }

  /**
   * for adding a modifcation
   * 
   * @param a
   * @param mod
   * @return
   */
  public static IonType createModified(final IonType a, final IonType mod) {
    IonType na = new IonType(a);
    // modification are saved in adducts for combined mods
    IonType[] realMod = mod.getAdducts();
    // add modification
    int length = realMod.length;
    if (a.getModification() != null)
      length += a.getModification().length;
    na.modification = new IonType[length];
    for (int i = 0; i < realMod.length; i++) {
      na.modification[i] = realMod[i];
      na.massDifference += realMod[i].getMassDifference();
    }
    if (a.getModification() != null)
      for (int i = 0; i < na.modification.length - realMod.length; i++)
        na.modification[i + realMod.length] = a.getModification()[i];
    // parse name
    na.parsedName = na.parseName();
    return na;
  }

  public IonType[] getModification() {
    return modification;
  }

  /**
   * 
   * @return array of names
   */
  public String[] getNames() {
    String[] names = new String[adducts.length];
    for (int i = 0; i < names.length; i++)
      names[i] = adducts[i].getRawName();
    return names;
  }

  public String[] getModNames() {
    String[] names = new String[modification.length];
    for (int i = 0; i < names.length; i++)
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
    if (modification != null) {
      Arrays.sort(modification);

      for (int i = 0; i < modification.length; i++) {
        String cs = modification[i].getRawName();
        if (s == null) {
          s = cs;
          counter = 1;
        } else if (s == cs)
          counter++;
        else {
          add = (modification[i - 1].getMassDifference() < 0 ? "-" : "+");
          counterS = counter > 1 ? String.valueOf(counter) : "";
          mod += add + counterS + s;
          s = cs;
          counter = 1;
        }
      }
      add = (modification[modification.length - 1].getMassDifference() < 0 ? "-" : "+");
      counterS = counter > 1 ? String.valueOf(counter) : "";
      mod += add + counterS + s;
    }

    s = null;
    String nname = "";
    if (adducts != null) {
      Arrays.sort(adducts);
      for (int i = 0; i < adducts.length; i++) {
        String cs = adducts[i].getRawName();
        if (s == null) {
          s = cs;
          counter = 1;
        } else if (s == cs)
          counter++;
        else {
          add = (adducts[i - 1].getMassDifference() < 0 ? "-" : "+");
          counterS = counter > 1 ? String.valueOf(counter) : "";
          nname += add + counterS + s;
          s = cs;
          counter = 1;
        }
      }
      add = (adducts[adducts.length - 1].getMassDifference() < 0 ? "-" : "+");
      counterS = counter > 1 ? String.valueOf(counter) : "";
      nname += add + counterS + s;
    }

    return mod + nname;
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
   * checks all sub/raw ESIAdductTypes
   * 
   * @param a
   * @return
   */
  public boolean nameEquals(IonType a) {
    boolean state = ((name != null && a.name != null && name.length() > 0 && a.name.length() > 0
        && name.equals(a.name)));
    if (state)
      return true;
    // check all sub adducts
    if (adducts.length != a.adducts.length)
      return false;

    // is already sorted?
    Arrays.sort(adducts);
    Arrays.sort(a.adducts);
    for (int i = 0; i < adducts.length; i++)
      if (!adducts[i].getRawName().equals(a.adducts[i].getRawName())
          || !adducts[i].sameMathDifference(a.adducts[i]))
        return false;
    return true;
  }

  /**
   * checks if all modification are equal
   * 
   * @param a
   * @return
   */
  public boolean modsEqual(IonType a) {
    if (!hasMods() && !a.hasMods())
      return true;
    // check all sub adducts
    if (modification.length != a.modification.length)
      return false;
    // is already sorted?
    Arrays.sort(modification);
    Arrays.sort(a.modification);
    for (int i = 0; i < modification.length; i++)
      if (!modification[i].getRawName().equals(a.modification[i].getRawName())
          || !modification[i].sameMathDifference(a.modification[i]))
        return false;
    return true;
  }


  @Override
  public String toString() {
    return toString(true);
  }

  public String toString(boolean showMass) {
    int absCharge = Math.abs(charge);
    String z = absCharge > 1 ? absCharge + "" : "";
    z += (charge < 0 ? "-" : "+");
    if (charge == 0)
      z = "";
    // molecules
    String mol = molecules > 1 ? String.valueOf(molecules) : "";
    if (showMass)
      return MessageFormat.format("[{0}M{1}]{2} ({3})", mol, parsedName, z,
          mzForm.format(getMassDifference()));
    else
      return MessageFormat.format("[{0}M{1}]{2}", mol, parsedName, z);
  }

  public String getMassDiffString() {
    return MZmineCore.getConfiguration().getMZFormat().format(massDifference) + " m/z";
  }

  /**
   * Checks mass diff, charge and mol equality
   * 
   * @param adduct
   * @return
   */
  public boolean sameMathDifference(IonType adduct) {
    return sameMassDifference(adduct) && charge == adduct.charge && molecules == adduct.molecules;
  }

  /**
   * Checks mass diff
   * 
   * @param adduct
   * @return
   */
  public boolean sameMassDifference(IonType adduct) {
    return Double.compare(massDifference, adduct.massDifference) == 0;
  }

  public int getAbsCharge() {
    return Math.abs(charge);
  }

  public IonType[] getAdducts() {
    return adducts;
  }

  public void setAdducts(IonType[] adducts) {
    this.adducts = adducts;
  }

  public void setMolecules(int i) {
    molecules = i;
  }

  /**
   * Is modified
   * 
   * @return
   */
  public boolean hasMods() {
    return modification != null && modification.length > 0;
  }

  /**
   * sorting
   */
  @Override
  public int compareTo(IonType a) {
    int i = this.getRawName().compareTo(a.getRawName());
    if (i == 0) {
      double md1 = getMassDifference();
      double md2 = a.getMassDifference();
      i = Double.compare(md1, md2);
    }
    return i;
  }

  /**
   * is a modification of parameter adduct? only if all adducts are the same, mass difference must
   * be different ONLY if this is a mod of parameter adduct
   * 
   * @param adduct
   * @return
   */
  public boolean isModificationOf(IonType adduct) {
    return !sameMassDifference(adduct) && molecules == adduct.molecules && charge == adduct.charge
        && nameEquals(adduct)
        && (this.hasMods()
            && (!adduct.hasMods() || (adduct.modification.length < this.modification.length
                && adduct.modification[0].equals(this.modification[0]))));
  }

  /**
   * subtracts the mods of the parameter adduct from this adduct
   * 
   * @param adduct
   * @return
   */
  public IonType subtractMods(IonType adduct) {
    // return an identity with only the modifications
    if (!adduct.hasMods())
      return new IonType(modification);
    else if (hasMods()) {
      List<IonType> mods = new ArrayList<>();
      for (int i = 0; i < modification.length; i++)
        if (adduct.modification.length <= i || !modification[i].equals(adduct.modification[i]))
          mods.add(modification[i]);
      return new IonType(mods.toArray(new IonType[mods.size()]));
    } else
      return null;
  }


  /**
   * 
   * @return modifications only or null
   */
  public IonType getModifiedOnly() {
    IonType[] all = getModification();
    if (all == null)
      return null;

    return new IonType(all);
  }

  /**
   * 
   * @return count of modification
   */
  public int getModCount() {
    return modification == null ? 0 : modification.length;
  }

  /**
   * ((mz * charge) - deltaMass) / numberOfMolecules
   * 
   * @param mz
   * @return
   */
  public double getMass(double mz) {
    return ((mz * this.getAbsCharge()) - this.getMassDifference()) / this.getMolecules();
  }


  /**
   * neutral mass of M to mz of yM+X]charge
   * 
   * (mass*mol + deltaMass) /charge
   * 
   * @param mz
   * @return
   */
  public double getMZ(double neutralmass) {
    return (neutralmass * getMolecules() + getMassDifference()) / getAbsCharge();
  }


  /**
   * Get the default adducts.
   *
   * @return the list of default adducts.
   */
  public static IonType[] getDefaultValuesPos() {
    return Arrays.copyOf(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_POSITIVE.length);
  }

  public static IonType[] getDefaultValuesNeg() {
    return Arrays.copyOf(DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_NEGATIVE.length);
  }

  public static IonType[] getDefaultModifications() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_MODIFICATIONS.length);
  }

  public static IonType[] getDefaultIsotopes() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_ISOTOPES.length);
  }

  @Override
  public boolean equals(final Object obj) {
    final boolean eq;
    if (obj instanceof IonType) {
      final IonType adduct = (IonType) obj;

      eq = adduct == this
          || (sameMathDifference(adduct) && nameEquals(adduct) && modsEqual(adduct));
    } else {
      eq = false;
    }
    return eq;
  }

  /**
   * is a fragment
   * 
   * @return
   */
  public boolean isFragment() {
    return getModification() != null || getCharge() == 0;
  }

  public IonType createModified(IonType mod) {
    return IonType.createModified(this, mod);
  }

  /**
   * 
   * @param b
   * @return true if no modification is a duplicate
   */
  public boolean uniqueModificationsTo(IonType b) {
    return (this.getModCount() == 0 && b.getModCount() == 0) || (this.getModCount() == 0)
        || b.getModCount() == 0 || Arrays.stream(getModification()).noneMatch(
            moda -> Arrays.stream(b.getModification()).anyMatch(modb -> moda.equals(modb)));
  }

  /**
   * 
   * @param b
   * @return true if no adduct is a duplicate
   */
  public boolean uniqueAdductsTo(IonType b) {
    return (this.getAdducts().length == 0 && b.getAdducts().length == 0)
        || (this.getAdducts().length == 0) || b.getAdducts().length == 0
        || Arrays.stream(getAdducts())
            .noneMatch(adda -> Arrays.stream(b.getAdducts()).anyMatch(addb -> adda.equals(addb)));
  }
}
