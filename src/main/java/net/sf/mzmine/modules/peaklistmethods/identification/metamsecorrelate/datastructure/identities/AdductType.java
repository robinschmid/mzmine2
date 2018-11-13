package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.mzmine.main.MZmineCore;

public class AdductType extends NeutralMolecule implements Comparable<AdductType> {

  // use combinations of X adducts (2H++; -H+Na2+) and modifications
  public static final AdductType M_MINUS =
      new AdductType(IonModificationType.ADDUCT, "e", +0.00054858, -1);
  public static final AdductType H_NEG =
      new AdductType(IonModificationType.ADDUCT, "H", "H", -1.007276, -1);
  public static final AdductType M_PLUS =
      new AdductType(IonModificationType.ADDUCT, "e", -0.00054858, 1);
  public static final AdductType H =
      new AdductType(IonModificationType.ADDUCT, "H", "H", 1.007276, 1);
  //
  private static final AdductType NA =
      new AdductType(IonModificationType.ADDUCT, "Na", "Na", 22.989218, 1);
  private static final AdductType NH4 =
      new AdductType(IonModificationType.ADDUCT, "NH4", "NH4", 18.033823, 1);
  private static final AdductType K =
      new AdductType(IonModificationType.ADDUCT, "K", "K", 38.963158, 1);
  private static final AdductType FE =
      new AdductType(IonModificationType.ADDUCT, "Fe", "Fe", 55.933840, 2);
  private static final AdductType CA =
      new AdductType(IonModificationType.ADDUCT, "Ca", "Ca", 39.961493820, 2);
  private static final AdductType MG =
      new AdductType(IonModificationType.ADDUCT, "Mg", "Mg", 47.96953482, 2);
  // combined
  private static final AdductType H2plus = new CombinedAdductType(new AdductType[] {H, H});
  private static final AdductType NA_H = new CombinedAdductType(new AdductType[] {NA, H});
  private static final AdductType K_H = new CombinedAdductType(new AdductType[] {K, H});
  private static final AdductType NH4_H = new CombinedAdductType(new AdductType[] {NH4, H});
  private static final AdductType Hneg_NA2 =
      new CombinedAdductType(new AdductType[] {NA, NA, H_NEG});
  private static final AdductType Hneg_CA = new CombinedAdductType(new AdductType[] {CA, H_NEG});
  private static final AdductType Hneg_FE = new CombinedAdductType(new AdductType[] {FE, H_NEG});
  private static final AdductType Hneg_MG = new CombinedAdductType(new AdductType[] {MG, H_NEG});

  // NEGATIVE
  private static final AdductType CL =
      new AdductType(IonModificationType.ADDUCT, "Cl", "Cl", 34.969401, -1);
  private static final AdductType BR =
      new AdductType(IonModificationType.ADDUCT, "Br", "Br", 78.918886, -1);
  private static final AdductType FA =
      new AdductType(IonModificationType.ADDUCT, "FA", "HCO2", 44.99820285, -1);
  // combined
  // +Na -2H+]-
  private static final AdductType NA_2H =
      new CombinedAdductType(new AdductType[] {NA, H_NEG, H_NEG});

  // modifications
  private static final AdductType H2 =
      new AdductType(IonModificationType.NEUTRAL_LOSS, "H2", "H2", -2.015650, 0);
  private static final AdductType C2H4 =
      new AdductType(IonModificationType.NEUTRAL_LOSS, "C2H4", "C2H4", -28.031301, 0);
  private static final AdductType H2O =
      new AdductType(IonModificationType.NEUTRAL_LOSS, "H2O", "H2O", -18.010565, 0);
  private static final AdductType H2O_2 = new CombinedAdductType(new AdductType[] {H2O, H2O});

  private static final AdductType NH3 =
      new AdductType(IonModificationType.NEUTRAL_LOSS, "NH3", "NH3", -17.026549, 0);
  private static final AdductType CO =
      new AdductType(IonModificationType.NEUTRAL_LOSS, "CO", "CO", -27.994915, 0);
  private static final AdductType CO2 =
      new AdductType(IonModificationType.NEUTRAL_LOSS, "CO2", "CO2", -43.989829, 0);
  // cluster
  private static final AdductType MEOH =
      new AdductType(IonModificationType.CLUSTER, "MeOH", "CH3OH", 32.026215, 0);
  private static final AdductType HFA =
      new AdductType(IonModificationType.CLUSTER, "HFA", "CHOOH", 46.005479, 0);
  private static final AdductType HAc =
      new AdductType(IonModificationType.CLUSTER, "HAc", "CH3COOH", 60.021129, 0);
  private static final AdductType ACN =
      new AdductType(IonModificationType.CLUSTER, "ACN", "CH3CN", 41.026549, 0);
  private static final AdductType O =
      new AdductType(IonModificationType.CLUSTER, "O", "O", 15.99491462, 0);
  private static final AdductType ISOPROP =
      new AdductType(IonModificationType.CLUSTER, "IsoProp", "C3H8O", 60.058064, 0);
  // isotopes
  public static final AdductType C13 =
      new AdductType(IonModificationType.ISOTOPE, "(13C)", 1.003354838, 0);

  // default values
  private static final AdductType[] DEFAULT_VALUES_POSITIVE = {H_NEG, M_PLUS, H, NA, K, NH4, H2plus,
      CA, FE, MG, NA_H, NH4_H, K_H, Hneg_NA2, Hneg_CA, Hneg_FE, Hneg_MG};
  private static final AdductType[] DEFAULT_VALUES_NEGATIVE =
      {M_MINUS, H_NEG, NA_2H, NA, CL, BR, FA};
  // default modifications
  private static final AdductType[] DEFAULT_VALUES_MODIFICATIONS =
      {H2O, H2O_2, NH3, O, CO, CO2, C2H4, HFA, HAc, MEOH, ACN, ISOPROP};
  // isotopes
  private static final AdductType[] DEFAULT_VALUES_ISOTOPES = {C13};

  // charge
  protected IonModificationType type;
  protected String parsedName = "";
  protected int charge;


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
  public AdductType(AdductType a) {
    this(a.type, a.getName(), a.getMolFormula(), a.getMass(), a.getCharge());
  }

  /**
   * new raw adduct
   * 
   * @param name
   * @param massDifference
   * @param charge
   * @param molecules
   */
  public AdductType(IonModificationType type, String name, String molFormula, double massDifference,
      int charge) {
    super();
    this.name = name;
    this.mass = massDifference;
    this.charge = charge;
    this.molFormula = molFormula;
    parsedName = parseName();
    this.type = type;
  }

  /**
   * new raw adduct
   * 
   * @param name
   * @param massDifference
   * @param charge
   */
  public AdductType(IonModificationType type, String name, double massDifference, int charge) {
    this(type, name, "", massDifference, charge);
  }

  public AdductType() {
    this(IonModificationType.UNKNOWN, "", 0, 0);
  }

  public IonModificationType getType() {
    return type;
  }

  /**
   * 
   * @return array of names
   */
  public String[] getRawNames() {
    return new String[] {getName()};
  }

  /**
   * 
   * @return parsed name (f.e. -2H+Na)
   */
  public String getParsedName() {
    return parsedName;
  }


  public int getCharge() {
    return charge;
  }

  /**
   * checks all sub/raw ESIAdductTypes
   * 
   * @param a
   * @return
   */
  public boolean nameEquals(AdductType a) {
    return parsedName.equals(a.parsedName);
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
    if (showMass)
      return MessageFormat.format("[M{0}]{1} ({2})", parsedName, z,
          MZmineCore.getConfiguration().getMZFormat().format(getMass()));
    else
      return MessageFormat.format("[M{0}]{1}", parsedName, z);
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
  public boolean sameMathDifference(AdductType adduct) {
    return sameMassDifference(adduct) && charge == adduct.charge;
  }

  /**
   * Checks mass diff
   * 
   * @param adduct
   * @return
   */
  public boolean sameMassDifference(AdductType adduct) {
    return Double.compare(mass, adduct.mass) == 0;
  }

  public int getAbsCharge() {
    return Math.abs(charge);
  }

  public @Nonnull AdductType[] getAdducts() {
    return new AdductType[] {this};
  }

  public int getNumberOfAdducts() {
    return 1;
  }

  /**
   * sorting
   */
  @Override
  public int compareTo(AdductType a) {
    int i = this.getName().compareTo(a.getName());
    if (i == 0) {
      i = Double.compare(getMass(), a.getMass());
      if (i == 0)
        i = Double.compare(getCharge(), a.getCharge());
    }
    return i;
  }

  /**
   * ((mz * charge) - deltaMass) / numberOfMolecules
   * 
   * @param mz
   * @return
   */
  public double getMass(double mz) {
    return ((mz * this.getAbsCharge()) - this.getMass());
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
    return (neutralmass + getMass()) / getAbsCharge();
  }


  @Override
  public int hashCode() {
    return Objects.hash(parsedName, charge, mass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!obj.getClass().equals(getClass()))
      return false;
    if (!(obj instanceof AdductType))
      return false;
    AdductType other = (AdductType) obj;
    if (charge != other.charge)
      return false;
    if (parsedName == null) {
      if (other.parsedName != null)
        return false;
    } else if (!parsedName.equals(other.parsedName))
      return false;

    if (!Objects.equals(mass, other.getMass()))
      return false;
    return true;
  }


  /**
   * 
   * @param b
   * @return true if no adduct is a duplicate
   */
  public boolean uniqueAdductsTo(AdductType adduct) {
    AdductType[] a = getAdducts();
    AdductType[] b = adduct.getAdducts();
    return Arrays.stream(a).noneMatch(adda -> Arrays.stream(b).anyMatch(addb -> adda.equals(addb)));
  }


  /**
   * Get the default adducts.
   *
   * @return the list of default adducts.
   */
  public static AdductType[] getDefaultValuesPos() {
    return Arrays.copyOf(DEFAULT_VALUES_POSITIVE, DEFAULT_VALUES_POSITIVE.length);
  }

  public static AdductType[] getDefaultValuesNeg() {
    return Arrays.copyOf(DEFAULT_VALUES_NEGATIVE, DEFAULT_VALUES_NEGATIVE.length);
  }

  public static AdductType[] getDefaultModifications() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_MODIFICATIONS.length);
  }

  public static AdductType[] getDefaultIsotopes() {
    return Arrays.copyOf(DEFAULT_VALUES_MODIFICATIONS, DEFAULT_VALUES_ISOTOPES.length);
  }

  /**
   * Undefined adduct for charge
   * 
   * @param charge
   * @return
   */
  public static AdductType getUndefinedforCharge(int charge) {
    double mass = AdductType.M_PLUS.getMass() * charge;
    return new AdductType(IonModificationType.UNDEFINED_ADDUCT, "?", mass, charge);
  }

  /**
   * All sub types of this need to be in parameter b
   * 
   * @param b
   * @return
   */
  public boolean isSubsetOf(AdductType b) {
    if (b instanceof CombinedAdductType) {
      // ion modifications all need to be in the mod array of this
      AdductType[] full = b.getAdducts();
      AdductType[] subset = this.getAdducts();
      boolean[] used = new boolean[full.length];

      for (int i = 0; i < subset.length; i++) {
        boolean found = false;
        for (int tm = 0; tm < used.length && !found; tm++) {
          if (!used[tm] && full[tm].equals(subset[i])) {
            used[tm] = true;
            found = true;
          }
        }
        if (!found)
          return false;
      }
      return true;
    } else
      return false;
  }

  /**
   * Removes all sub types of parameter from this type
   * 
   * @param type
   * @return
   */
  public @Nullable AdductType remove(AdductType type) {
    if (this.isSubsetOf(type) || this.equals(type))
      return null;
    else
      return this;
  }
}
