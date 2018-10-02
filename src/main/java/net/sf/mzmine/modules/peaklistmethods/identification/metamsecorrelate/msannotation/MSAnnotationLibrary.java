package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSAnnotationLibrary {
  private static final Logger LOG = Logger.getLogger(MSAnnotationLibrary.class.getName());
  private MZTolerance mzTolerance;
  // adducts
  private final ESIAdductType[] selectedAdducts, selectedMods;
  private List<ESIAdductType> allAdducts = new ArrayList<ESIAdductType>();
  private List<ESIAdductType> allModification = new ArrayList<ESIAdductType>();
  private final boolean isPositive;
  private final int maxMolecules, maxCombinations, maxCharge, maxMods;

  public MSAnnotationLibrary(MSAnnotationParameters parameterSet) {
    mzTolerance = parameterSet.getParameter(MSAnnotationParameters.MZ_TOLERANCE).getValue();
    // adducts stuff
    isPositive = parameterSet.getParameter(MSAnnotationParameters.POSITIVE_MODE).getValue();
    maxMolecules = parameterSet.getParameter(MSAnnotationParameters.MAX_MOLECULES).getValue();
    maxCombinations = parameterSet.getParameter(MSAnnotationParameters.MAX_COMBINATION).getValue();
    maxCharge = parameterSet.getParameter(MSAnnotationParameters.MAX_CHARGE).getValue();
    maxMods = parameterSet.getParameter(MSAnnotationParameters.MAX_MODS).getValue();

    selectedAdducts = parameterSet.getParameter(MSAnnotationParameters.ADDUCTS).getValue()[0];
    selectedMods = parameterSet.getParameter(MSAnnotationParameters.ADDUCTS).getValue()[1];

    createAllAdducts(isPositive, maxMolecules, maxCombinations, maxCharge, maxMods);
  }

  /**
   * create all possible adducts
   */
  private void createAllAdducts(boolean positive, int maxMolecules, int maxCombination,
      int maxCharge, int maxMods) {
    // normal primary adducts
    for (ESIAdductType a : selectedAdducts)
      if ((a.getCharge() > 0 && positive) || (a.getCharge() < 0 && !positive))
        allAdducts.add(a);
    // combined adducts
    if (maxCombination > 1) {
      combineAdducts(allAdducts, selectedAdducts, new ArrayList<ESIAdductType>(allAdducts),
          maxCombination, 1, false);
    }

    // only keep the ones with maxCharge+1 (to abstract/add one H only)
    for (int i = 0; i < allAdducts.size(); i++) {
      if (allAdducts.get(i).getAbsCharge() > maxCharge + 1) {
        allAdducts.remove(i);
        i--;
      }
    }
    // add or remove H from multi charged (Fe2+)
    addRemoveHydrogen(positive);

    // add modification
    if (maxMods > 0)
      addModification();
    // multiple molecules
    addMultipleMolecules(maxMolecules);
    // remove all >max charge
    for (int i = 0; i < allAdducts.size(); i++) {
      if (allAdducts.get(i).getAbsCharge() > maxCharge) {
        allAdducts.remove(i);
        i--;
      }
    }
    // print them out
    for (ESIAdductType a : allAdducts)
      LOG.info(a.toString());
  }

  /**
   * Does only find one
   * 
   * @param mainRow main peak.
   * @param possibleAdduct candidate adduct peak.
   */
  public ESIAdductType[] findAdducts(final PeakList peakList, final PeakListRow row1,
      final PeakListRow row2, final int z1, final int z2) {
    // check all combinations of adducts
    for (final ESIAdductType adduct : allAdducts) {
      for (final ESIAdductType adduct2 : allAdducts) {
        // for one adduct use a maximum of 1 modification
        // second can have <=maxMods
        if (!adduct.equals(adduct2) && !(adduct.getModCount() > 1 && adduct2.getModCount() > 1)) {
          // check charge state if absCharge is not -1 or 0
          if ((z1 <= 0 || adduct.getAbsCharge() == z1)
              && (z2 <= 0 || adduct2.getAbsCharge() == z2)) {
            // checks each raw file - only true if all m/z are in range
            if (checkAdduct(peakList, row1, row2, adduct, adduct2)) {
              // is a2 a modification of a1? (same adducts - different mods
              if (adduct2.isModificationOf(adduct)) {
                adduct2.subtractMods(adduct).addAdductIdentityToRow(row2, row1);
                MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row2, false);
              } else if (adduct.isModificationOf(adduct2)) {
                adduct.subtractMods(adduct2).addAdductIdentityToRow(row1, row2);
                MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row1, false);
              } else {
                // Add adduct identity and notify GUI.
                // only if not already present
                adduct.addAdductIdentityToRow(row1, row2);
                MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row1, false);
                adduct2.addAdductIdentityToRow(row2, row1);
                MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row2, false);
              }
              // there can only be one hit for a row-row comparison
              return new ESIAdductType[] {adduct, adduct2};
            }
          }
        }
      }
    }
    // no adduct to be found
    return null;
  }

  /**
   * Check if candidate peak is a given type of adduct of given main peak. is not checking retention
   * time (has to be checked before)
   * 
   * @param mainPeak main peak.
   * @param possibleAdduct candidate adduct peak.
   * @param adduct adduct.
   * @return true if mass difference, retention time tolerance and adduct peak height conditions are
   *         met.
   */
  private boolean checkAdduct(final PeakList peakList, final PeakListRow row1,
      final PeakListRow row2, final ESIAdductType adduct, final ESIAdductType adduct2) {
    // for each peak[rawfile] in row
    boolean hasCommonPeak = false;
    //
    for (RawDataFile raw : peakList.getRawDataFiles()) {
      Feature f1 = row1.getPeak(raw);
      Feature f2 = row2.getPeak(raw);
      if (f1 != null && f2 != null) {
        hasCommonPeak = true;
        double mz1 = ((f1.getMZ() * adduct.getAbsCharge()) - adduct.getMassDifference())
            / adduct.getMolecules();
        double mz2 = ((f2.getMZ() * adduct2.getAbsCharge()) - adduct2.getMassDifference())
            / adduct2.getMolecules();
        if (!mzTolerance.checkWithinTolerance(mz1, mz2))
          return false;
      }
    }
    // directly returns false if not in range
    // so if has common peak = isAdduct
    return hasCommonPeak;
  }

  /**
   * adds modification to the existing adducts
   */
  private void addModification() {
    // normal mods
    for (ESIAdductType a : selectedMods)
      allModification.add(a);
    // combined modification
    if (maxMods > 1)
      combineAdducts(allModification, selectedMods, new ArrayList<ESIAdductType>(allModification),
          maxMods, 1, true);
    // add new modified adducts
    int size = allAdducts.size();
    for (int i = 0; i < size; i++) {
      ESIAdductType a = allAdducts.get(i);
      // all mods
      for (ESIAdductType mod : allModification) {
        allAdducts.add(ESIAdductType.createModified(a, mod));
      }
    }
  }

  private void addMultipleMolecules(int maxMolecules) {
    int size = allAdducts.size();
    for (int k = 0; k < size; k++) {
      ESIAdductType a = allAdducts.get(k);
      for (int i = 2; i <= maxMolecules; i++) {
        allAdducts.add(new ESIAdductType(a));
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
  private void combineAdducts(List<ESIAdductType> targetList, ESIAdductType[] selectedList,
      final List<ESIAdductType> adducts, int maxCombination, int run, boolean zeroChargeAllowed) {
    List<ESIAdductType> newAdducts = new ArrayList<ESIAdductType>();
    for (int i = 0; i < adducts.size(); i++) {
      ESIAdductType a1 = adducts.get(i);
      for (int k = 0; k < selectedList.length; k++) {
        ESIAdductType a2 = selectedList[k];
        ESIAdductType na = new ESIAdductType(a1, a2);
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

  private boolean isContainedIn(List<ESIAdductType> adducts, ESIAdductType na) {
    for (ESIAdductType a : adducts) {
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
    ESIAdductType H = ESIAdductType.H;
    ESIAdductType Hneg = ESIAdductType.H_NEG;
    // remove/add hydrogen from double charged ones to get single charge
    // example: M+Fe]2+ will be M+Fe-H]+
    for (int i = 0; i < allAdducts.size(); i++) {
      ESIAdductType a = allAdducts.get(i);
      for (int z = a.getAbsCharge(); z > 1; z--) {
        // positive remove H ; negative add H
        ESIAdductType tmpA = new ESIAdductType(a, positive ? Hneg : H);
        if (!isContainedIn(allAdducts, tmpA))
          allAdducts.add(tmpA);
        a = tmpA;
      }
    }
    // find !positve selectedAdducts and
    // add/remove as many H as possible
    for (int i = 0; i < selectedAdducts.length; i++) {
      ESIAdductType a = selectedAdducts[i];
      // adduct has a different charge state than MS mode
      if (((a.getCharge() > 0) != positive)) {
        // add/remove H to absCharge == 1 (+- like positive)
        ESIAdductType[] start = new ESIAdductType[a.getAbsCharge() + 2];
        start[0] = a;
        for (int k = 1; k < start.length; k++)
          start[k] = positive ? H : Hneg;
        a = new ESIAdductType(start);
        if (!isContainedIn(allAdducts, a))
          allAdducts.add(a);
        // loop runs:
        for (int z = 2; z <= maxCharge; z++) {
          ESIAdductType tmpA = new ESIAdductType(a, positive ? H : Hneg);
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

  public ESIAdductType[] getSelectedAdducts() {
    return selectedAdducts;
  }

  public ESIAdductType[] getSelectedMods() {
    return selectedMods;
  }

  public List<ESIAdductType> getAllAdducts() {
    return allAdducts;
  }

  public List<ESIAdductType> getAllModification() {
    return allModification;
  }

  public boolean isPositive() {
    return isPositive;
  }

  public int getMaxMolecules() {
    return maxMolecules;
  }

  public int getMaxCombinations() {
    return maxCombinations;
  }

  public int getMaxCharge() {
    return maxCharge;
  }

  public int getMaxMods() {
    return maxMods;
  }


}
