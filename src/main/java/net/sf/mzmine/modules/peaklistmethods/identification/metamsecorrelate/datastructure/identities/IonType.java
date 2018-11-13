package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.mzmine.main.MZmineCore;

public class IonType extends NeutralMolecule implements Comparable<IonType> {

  protected final @Nonnull AdductType adduct;
  protected final @Nullable NeutralModification[] mod;
  protected final int molecules;
  protected final int charge;

  public IonType(AdductType adduct, NeutralModification... mod) {
    this(1, adduct, mod);
  }

  public IonType(int molecules, AdductType adduct, NeutralModification... mod) {
    this.adduct = adduct;
    this.mod = mod;
    this.charge = adduct.charge;
    this.molecules = molecules;
    name = parseName();
    //
    mass = adduct.getMass();
    if (mod != null) {
      Arrays.sort(mod);
      mass += Arrays.stream(mod).mapToDouble(NeutralModification::getMass).sum();
    }
  }

  /**
   * for adding modifications
   * 
   * @param a
   * @param mod
   * @return
   */
  public IonType createModified(final NeutralModification... newMod) {
    List<NeutralMolecule> allMod = new ArrayList<>();
    for (NeutralModification m : newMod)
      allMod.add(m);
    if (this.mod != null)
      for (NeutralModification m : this.mod)
        allMod.add(m);

    return new IonType(this.adduct, allMod.toArray(new NeutralModification[allMod.size()]));
  }

  /**
   * All modifications
   * 
   * @return
   */
  public NeutralModification[] getModification() {
    return mod;
  }

  @Override
  public String parseName() {
    StringBuilder sb = new StringBuilder();
    // modification first
    if (mod != null)
      Arrays.stream(mod).map(NeutralModification::getParsedName).forEach(n -> sb.append(n));
    // adducts
    sb.append(adduct.getParsedName());

    return sb.toString();
  }

  public double getMassDifference() {
    return mass;
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
    return name.equals(a.name);
  }

  /**
   * checks if all modification are equal
   * 
   * @param a
   * @return
   */
  public boolean modsEqual(IonType a) {
    if (this.mod == a.mod)
      return true;
    if (this.mod == null ^ a.mod == null)
      return false;

    if (this.mod.length != a.mod.length)
      return false;

    // is already sorted
    for (int i = 0; i < mod.length; i++)
      if (!mod[i].equals(a.mod[i]))
        return false;
    return true;
  }

  /**
   * checks if at least one modification is shared
   * 
   * @param a
   * @return
   */
  public boolean hasModificationOverlap(IonType a) {
    if (this.mod == a.mod || (mod == null && a.mod == null))
      return true;

    if (this.mod == null ^ a.mod == null)
      return false;

    if (this.mod.length == 0 || a.mod.length == 0)
      return false;

    // is already sorted
    for (final NeutralModification thisMod : mod)
      if (Arrays.stream(a.mod).anyMatch(moda -> moda.equals(thisMod)))
        return true;
    return false;
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
      return MessageFormat.format("[{0}M{1}]{2} ({3})", mol, name, z,
          MZmineCore.getConfiguration().getMZFormat().format(getMassDifference()));
    else
      return MessageFormat.format("[{0}M{1}]{2}", mol, name, z);
  }

  public String getMassDiffString() {
    return MZmineCore.getConfiguration().getMZFormat().format(mass) + " m/z";
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
    return Double.compare(mass, adduct.mass) == 0;
  }

  public int getAbsCharge() {
    return Math.abs(charge);
  }

  public AdductType getAdduct() {
    return adduct;
  }

  /**
   * Is modified
   * 
   * @return
   */
  public boolean hasMods() {
    return mod != null && mod.length > 0;
  }

  /**
   * sorting
   */
  @Override
  public int compareTo(IonType a) {
    int i = this.getName().compareTo(a.getName());
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
  public boolean isModificationOf(IonType ion) {
    if (!hasMods() || !(ion.getModCount() < getModCount() && mass != ion.mass
        && adduct.equals(ion.adduct) && molecules == ion.molecules && charge == ion.charge))
      return false;
    else if (!ion.hasMods())
      return true;
    else {
      // ion modifications all need to be in the mod array of this
      boolean[] used = new boolean[this.mod.length];

      for (int i = 0; i < ion.getModCount(); i++) {
        boolean found = false;
        for (int tm = 0; tm < used.length && !found; tm++) {
          if (!used[tm] && this.mod[tm].equals(ion.mod[i])) {
            used[tm] = true;
            found = true;
          }
        }
        if (!found)
          return false;
      }
      return true;
    }
  }

  /**
   * subtracts the mods of the parameter adduct from this adduct
   * 
   * @param adduct
   * @return
   */
  public IonType subtractMods(IonType ion) {
    // return an identity with only the modifications
    if (hasMods() && ion.hasMods()) {
      List<NeutralModification> newMods = new ArrayList<>();
      for (NeutralModification m : mod)
        newMods.add(m);

      for (NeutralModification m : ion.mod)
        newMods.remove(m);
      return new IonType(this.molecules, this.adduct,
          newMods.toArray(new NeutralModification[newMods.size()]));
    } else
      return this;
  }


  /**
   * Undefined adduct with 1 molecule and all modifications
   * 
   * @return modifications only or null
   */
  public IonType getModifiedOnly() {
    return new IonType(1, AdductType.getUndefinedforCharge(this.charge), mod);
  }

  /**
   * 
   * @return count of modification
   */
  public int getModCount() {
    return mod == null ? 0 : mod.length;
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

  @Override
  public boolean equals(final Object obj) {
    if (obj == null || !obj.getClass().equals(this.getClass()) || !(obj instanceof IonType))
      return false;
    if (!super.equals(obj))
      return false;

    final IonType a = (IonType) obj;
    return (sameMathDifference(a) && adductsEqual(a) && modsEqual(a));
  }

  @Override
  public int hashCode() {
    return Objects.hash(adduct, mod == null ? "" : mod, charge, molecules, mass, name);
  }

  /**
   * 
   * @param b
   * @return true if no adduct is a duplicate
   */
  public boolean adductsEqual(IonType b) {
    return adduct.equals(b.adduct);
  }
}
