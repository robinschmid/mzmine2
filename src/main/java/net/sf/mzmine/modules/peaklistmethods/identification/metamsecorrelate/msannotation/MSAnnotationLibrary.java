package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.AdductType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSAnnotationLibrary {
  private static final Logger LOG = Logger.getLogger(MSAnnotationLibrary.class.getName());

  public enum CheckMode {
    AVGERAGE, ONE_FEATURE, ALL_FEATURES;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

  private MZTolerance mzTolerance;
  // adducts
  private final AdductType[] selectedAdducts, selectedMods;
  private List<AdductType> allAdducts = new ArrayList<AdductType>();
  private List<AdductType> allModification = new ArrayList<AdductType>();
  private final boolean isPositive;
  private final int maxMolecules, maxCharge;

  public MSAnnotationLibrary(MSAnnotationParameters parameterSet) {
    this(parameterSet, parameterSet.getParameter(MSAnnotationParameters.MZ_TOLERANCE).getValue(),
        parameterSet.getParameter(MSAnnotationParameters.MAX_CHARGE).getValue());
  }

  /**
   * For simple setup
   * 
   * @param parameterSet
   */
  public MSAnnotationLibrary(MSAnnotationParameters parameterSet, MZTolerance mzTolerance,
      int maxCharge) {
    this.mzTolerance = mzTolerance;
    this.maxCharge = maxCharge;
    // adducts stuff
    isPositive = parameterSet.getParameter(MSAnnotationParameters.POSITIVE_MODE).getValue()
        .equals("POSITIVE");
    maxMolecules = parameterSet.getParameter(MSAnnotationParameters.MAX_MOLECULES).getValue();

    selectedAdducts = parameterSet.getParameter(MSAnnotationParameters.ADDUCTS).getValue()[0];
    selectedMods = parameterSet.getParameter(MSAnnotationParameters.ADDUCTS).getValue()[1];

    createAllAdducts(isPositive, maxMolecules, maxCharge);
  }

  /**
   * create all possible adducts
   */
  private void createAllAdducts(boolean positive, int maxMolecules, int maxCharge) {
    // normal primary adducts
    for (AdductType a : selectedAdducts)
      if ((a.getCharge() > 0 && positive) || (a.getCharge() < 0 && !positive))
        allAdducts.add(a);

    // only keep the ones with <=maxCharge
    for (int i = 0; i < allAdducts.size(); i++) {
      if (allAdducts.get(i).getAbsCharge() > maxCharge) {
        allAdducts.remove(i);
        i--;
      }
    }

    addModification();
    // multiple molecules
    addMultipleMolecules(maxMolecules);
    // print them out
    for (AdductType a : allAdducts)
      LOG.info(a.toString());
  }

  /**
   * Does only find one
   * 
   * @param mainRow main peak.
   * @param possibleAdduct candidate adduct peak.
   */
  public AdductType[] findAdducts(final PeakList peakList, final PeakListRow row1,
      final PeakListRow row2, final CheckMode mode, final double minHeight) {
    return findAdducts(peakList, row1, row2, row1.getRowCharge(), row2.getRowCharge(), mode,
        minHeight);
  }

  /**
   * Does only find one adduct
   * 
   * @param peakList
   * @param row1
   * @param row2
   * @param z1 -1 or 0 if not set (charge state always positive)
   * @param z2 -1 or 0 if not set (charge state always positive)
   * @return
   */
  public AdductType[] findAdducts(final PeakList peakList, final PeakListRow row1,
      final PeakListRow row2, int z1, int z2, final CheckMode mode, final double minHeight) {
    z1 = Math.abs(z1);
    z2 = Math.abs(z2);
    // check all combinations of adducts
    for (AdductType adduct : allAdducts) {
      for (AdductType adduct2 : allAdducts) {
        if (adduct.equals(adduct2))
          continue;

        // do not check if MOL = MOL and MOL>1
        // only one can be modified
        // check charge state if absCharge is not -1 or 0 (no charge detected)
        if (checkMolCount(adduct, adduct) //
            && checkMaxMod(adduct, adduct2) //
            && checkChargeStates(adduct, adduct2, z1, z2) //
            && checkMultiChargeDifference(adduct, adduct2)) {
          // checks each raw file - only true if all m/z are in range
          if (checkAdduct(peakList, row1, row2, adduct, adduct2, mode, minHeight)) {
            // reduce mol if mol1==mol2
            // [2M+H] and [2M+Na] --> [M+H] [M+Na]
            if (adduct.getMolecules() == adduct2.getMolecules()) {
              adduct = new AdductType(adduct);
              adduct.setMolecules(1);
              adduct2 = new AdductType(adduct2);
              adduct2.setMolecules(1);
            }

            // is a2 a modification of a1? (same adducts - different mods
            if (adduct2.isModificationOf(adduct)) {
              AdductType mod = adduct2.subtractMods(adduct);
              ESIAdductIdentity.addAdductIdentityToRow(row1, AdductType.M_UNMODIFIED, row1, mod);
            } else if (adduct.isModificationOf(adduct2)) {
              AdductType mod = adduct.subtractMods(adduct2);
              ESIAdductIdentity.addAdductIdentityToRow(row1, mod, row2, AdductType.M_UNMODIFIED);
            } else {
              // Add adduct identity and notify GUI.
              // only if not already present
              ESIAdductIdentity.addAdductIdentityToRow(row1, adduct, row2, adduct2);
            }
            // update
            MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row1, false);
            MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row2, false);
            // there can only be one hit for a row-row comparison
            return new AdductType[] {adduct, adduct2};
          }
        }
      }
    }
    // no adduct to be found
    return null;
  }

  /**
   * [yM+X]2+ and [yM+X-H]+ are only different by -H. if any adduct part or modification equals,
   * return false. Charge is different
   * 
   * @param a
   * @param b
   * @return only true if charge is equal or no modification or adduct sub part equals
   */
  private boolean checkMultiChargeDifference(AdductType a, AdductType b) {
    return a.getCharge() == b.getCharge() || (a.uniqueModificationsTo(b) && a.uniqueAdductsTo(b));
  }

  /**
   * MOL != MOL or MOL==1
   * 
   * @param a
   * @param b
   * @return
   */
  private boolean checkMolCount(AdductType a, AdductType b) {
    return a.getMolecules() != b.getMolecules() || (a.getMolecules() == 1 && b.getMolecules() == 1);
  }

  /**
   * True if a charge state was not detected or if it fits to the adduct
   * 
   * @param adduct
   * @param adduct2
   * @param z1
   * @param z2
   * @return
   */
  private boolean checkChargeStates(AdductType adduct, AdductType adduct2, int z1, int z2) {
    return (z1 <= 0 || adduct.getAbsCharge() == z1) && (z2 <= 0 || adduct2.getAbsCharge() == z2);
  }

  /**
   * Only one adduct can have modifications
   * 
   * @param adduct
   * @param adduct2
   * @return
   */
  private boolean checkMaxMod(AdductType adduct, AdductType adduct2) {
    return !(adduct.getModCount() > 0 && adduct2.getModCount() > 0);
  }

  /**
   * Check if candidate peak is a given type of adduct of given main peak. is not checking retention
   * time (has to be checked before)
   * 
   * @param peakList
   * @param row1
   * @param row2
   * @param adduct
   * @param adduct2
   * @param minHeight exclude smaller peaks as they can have a higher mz difference
   * @return false if one peak pair with height>=minHeight is outside of mzTolerance
   */
  private boolean checkAdduct(final PeakList peakList, final PeakListRow row1,
      final PeakListRow row2, final AdductType adduct, final AdductType adduct2,
      final CheckMode mode, double minHeight) {
    // averarge mz
    if (mode.equals(CheckMode.AVGERAGE)) {
      double m1 = adduct.getMass(row1.getAverageMZ());
      double m2 = adduct2.getMass(row2.getAverageMZ());
      return mzTolerance.checkWithinTolerance(m1, m2);
    } else {
      // feature comparison
      // for each peak[rawfile] in row
      boolean hasCommonPeak = false;
      //
      for (RawDataFile raw : peakList.getRawDataFiles()) {
        Feature f1 = row1.getPeak(raw);
        Feature f2 = row2.getPeak(raw);
        // check for minimum height. Small peaks have a higher delta mz
        if (f1 != null && f2 != null && f1.getHeight() >= minHeight
            && f2.getHeight() >= minHeight) {
          hasCommonPeak = true;
          double m1 = adduct.getMass(f1.getMZ());
          double m2 = adduct2.getMass(f2.getMZ());
          boolean sameMZ = mzTolerance.checkWithinTolerance(m1, m2);

          // short cut
          switch (mode) {
            case ONE_FEATURE:
              if (sameMZ)
                return true;
              break;
            case ALL_FEATURES:
              if (!sameMZ)
                return false;
              break;
          }
        }
      }
      // directly returns false if not in range
      // so if has common peak = isAdduct
      return mode.equals(CheckMode.ALL_FEATURES) && hasCommonPeak;
    }
  }

  /**
   * adds modification to the existing adducts
   */
  private void addModification() {
    // normal mods
    for (AdductType a : selectedMods)
      allModification.add(a);

    // add new modified adducts
    int size = allAdducts.size();
    for (int i = 0; i < size; i++) {
      AdductType a = allAdducts.get(i);
      // all mods
      for (AdductType mod : allModification) {
        allAdducts.add(AdductType.createModified(a, mod));
      }
    }
  }

  private void addMultipleMolecules(int maxMolecules) {
    int size = allAdducts.size();
    for (int k = 0; k < size; k++) {
      AdductType a = allAdducts.get(k);
      for (int i = 2; i <= maxMolecules; i++) {
        allAdducts.add(new AdductType(a));
        allAdducts.get(allAdducts.size() - 1).setMolecules(i);
      }
    }
  }

  /**
   * does not check maxCharge-delete afterwards
   * 
   * @param adducts
   * @param maxCombination
   * @param maxCharge
   * @param run init with 1
   */
  private void combineAdducts(List<AdductType> targetList, AdductType[] selectedList,
      final List<AdductType> adducts, int maxCombination, int run, boolean zeroChargeAllowed) {
    List<AdductType> newAdducts = new ArrayList<AdductType>();
    for (int i = 0; i < adducts.size(); i++) {
      AdductType a1 = adducts.get(i);
      for (int k = 0; k < selectedList.length; k++) {
        AdductType a2 = selectedList[k];
        AdductType na = new AdductType(a1, a2);
        if ((zeroChargeAllowed || na.getCharge() != 0) && !isContainedIn(targetList, na)) {
          newAdducts.add(na);
          targetList.add(na);
        }
      }
    }
    // first run = combination of two
    if (run + 1 < maxCombination) {
      combineAdducts(targetList, selectedList, newAdducts, maxCombination, run + 1,
          zeroChargeAllowed);
    }
  }

  private boolean isContainedIn(List<AdductType> adducts, AdductType na) {
    for (AdductType a : adducts) {
      if (a.sameMathDifference(na))
        return true;
    }
    return false;
  }

  /**
   * add or remove hydrogen to obtain more adduct types also to all positive adducts
   * 
   * @param positive
   */
  private void addRemoveHydrogen(boolean positive) {
    AdductType H = AdductType.H;
    AdductType Hneg = AdductType.H_NEG;
    // remove/add hydrogen from double charged ones to get single charge
    // example: M+Fe]2+ will be M+Fe-H]+
    for (int i = 0; i < allAdducts.size(); i++) {
      AdductType a = allAdducts.get(i);
      for (int z = a.getAbsCharge(); z > 1; z--) {
        // positive remove H ; negative add H
        AdductType tmpA = new AdductType(a, positive ? Hneg : H);
        if (!isContainedIn(allAdducts, tmpA))
          allAdducts.add(tmpA);
        a = tmpA;
      }
    }
    // find !positve selectedAdducts and
    // add/remove as many H as possible
    for (int i = 0; i < selectedAdducts.length; i++) {
      AdductType a = selectedAdducts[i];
      // adduct has a different charge state than MS mode
      if (((a.getCharge() > 0) != positive)) {
        // add/remove H to absCharge == 1 (+- like positive)
        AdductType[] start = new AdductType[a.getAbsCharge() + 2];
        start[0] = a;
        for (int k = 1; k < start.length; k++)
          start[k] = positive ? H : Hneg;
        a = new AdductType(start);
        if (!isContainedIn(allAdducts, a))
          allAdducts.add(a);
        // loop runs:
        for (int z = 2; z <= maxCharge; z++) {
          AdductType tmpA = new AdductType(a, positive ? H : Hneg);
          if (!isContainedIn(allAdducts, tmpA))
            allAdducts.add(tmpA);
          a = tmpA;
        }
      }
    }
  }

  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  public AdductType[] getSelectedAdducts() {
    return selectedAdducts;
  }

  public AdductType[] getSelectedMods() {
    return selectedMods;
  }

  public List<AdductType> getAllAdducts() {
    return allAdducts;
  }

  public List<AdductType> getAllModification() {
    return allModification;
  }

  public boolean isPositive() {
    return isPositive;
  }

  public int getMaxMolecules() {
    return maxMolecules;
  }

  public int getMaxCharge() {
    return maxCharge;
  }

}
