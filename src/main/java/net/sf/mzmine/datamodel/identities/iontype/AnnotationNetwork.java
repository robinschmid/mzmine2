package net.sf.mzmine.datamodel.identities.iontype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.MolecularFormulaIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * An annotation network full of ions that point to the same neutral molecule (neutral mass)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class AnnotationNetwork extends HashMap<PeakListRow, IonIdentity> {
  // MZtolerance on MS1 to generate this network
  private MZTolerance mzTolerance;
  // network id
  private int id;
  // neutral mass of central molecule which is described by all members of this network
  private Double neutralMass = null;
  // maximum absolute deviation from neutral mass average
  private Double maxDev = null;
  // average retention time of network
  private double avgRT;

  // possible formulas for this neutral mass
  private List<MolecularFormulaIdentity> molFormulas;

  public AnnotationNetwork(MZTolerance mzTolerance, int id) {
    super();
    this.mzTolerance = mzTolerance;
    this.id = id;
  }

  /**
   * Network ID
   * 
   * @return
   */
  public int getID() {
    return id;
  }

  public List<MolecularFormulaIdentity> getMolFormulas() {
    return molFormulas;
  }

  /**
   * The first formula should be the best
   * 
   * @param molFormulas
   */
  public void setMolFormulas(List<MolecularFormulaIdentity> molFormulas) {
    this.molFormulas = molFormulas;
  }

  public void addMolFormula(MolecularFormulaIdentity formula) {
    if (molFormulas == null)
      molFormulas = new ArrayList<>();
    this.molFormulas.add(formula);
  }

  /**
   * Best molecular formula (first in list)
   * 
   * @return
   */
  public MolecularFormulaIdentity getBestMolFormula() {
    return molFormulas == null || molFormulas.isEmpty() ? null : molFormulas.get(0);
  }

  /**
   * Neutral mass of center molecule which is described by all members of this network
   * 
   * @param neutralMass
   */
  public double getNeutralMass() {
    return neutralMass == null ? calcNeutralMass() : neutralMass;
  }

  @Override
  public IonIdentity put(PeakListRow key, IonIdentity value) {
    IonIdentity e = super.put(key, value);
    fireChanged();
    return e;
  }

  @Override
  public IonIdentity remove(Object key) {
    IonIdentity e = super.remove(key);
    fireChanged();
    return e;
  }

  @Override
  public void clear() {
    super.clear();
    fireChanged();
  }

  @Override
  public IonIdentity replace(PeakListRow key, IonIdentity value) {
    IonIdentity e = super.replace(key, value);
    fireChanged();
    return e;
  }

  public void fireChanged() {
    resetNeutralMass();
    resetMaxDev();
  }

  public void resetNeutralMass() {
    neutralMass = null;
  }

  /**
   * Maximum absolute deviation from central neutral mass
   */
  public void resetMaxDev() {
    maxDev = null;
  }

  /**
   * Calculates and sets the neutral mass average
   * 
   * @return
   */
  public double calcNeutralMass() {
    neutralMass = null;
    if (size() == 0)
      return 0;

    double mass = 0;
    for (Entry<PeakListRow, IonIdentity> e : entrySet()) {
      mass += e.getValue().getIonType().getMass(e.getKey().getAverageMZ());
    }
    neutralMass = mass / size();
    return neutralMass;
  }

  /**
   * Calculates and sets the avg RT
   * 
   * @return
   */
  public double calcAvgRT() {
    neutralMass = null;
    if (size() == 0)
      return 0;

    double rt = 0;
    for (Entry<PeakListRow, IonIdentity> e : entrySet()) {
      rt += e.getKey().getAverageRT();
    }
    avgRT = rt / size();
    return avgRT;
  }

  public double getAvgRT() {
    return avgRT;
  }

  /**
   * calculates the maximum deviation from the average mass
   * 
   * @return
   */
  public double calcMaxDev() {
    maxDev = null;
    if (size() == 0)
      return 0;

    neutralMass = getNeutralMass();
    if (neutralMass == null || neutralMass == 0)
      return 0;

    double max = 0;
    for (Entry<PeakListRow, IonIdentity> e : entrySet()) {
      double mass = getMass(e);
      max = Math.max(Math.abs(neutralMass - mass), max);
    }
    maxDev = max;
    return maxDev;
  }

  /**
   * Neutral mass of entry
   * 
   * @param e
   * @return
   */
  public double getMass(Entry<PeakListRow, IonIdentity> e) {
    return e.getValue().getIonType().getMass(e.getKey().getAverageMZ());
  }

  public double getMaxDev() {
    return maxDev == null ? calcMaxDev() : maxDev;
  }

  /**
   * All rows point to the same neutral mass
   * 
   * @param mzTol
   * @return
   */
  public boolean checkAllWithinMZTol(MZTolerance mzTol) {
    double neutralMass = getNeutralMass();
    double maxDev = getMaxDev();
    return mzTol.checkWithinTolerance(neutralMass, neutralMass + maxDev);
  }

  public int[] getAllIDs() {
    return keySet().stream().mapToInt(e -> e.getID()).toArray();
  }

  public void setNetworkToAllRows() {
    values().stream().forEach(id -> id.setNetwork(this));
  }

  /**
   * Checks the calculated neutral mass of the ion annotation against the avg neutral mass
   * 
   * @param row
   * @param pid
   * @return
   */
  public boolean checkForAnnotation(PeakListRow row, IonType pid) {
    return mzTolerance.checkWithinTolerance(calcNeutralMass(), pid.getMass(row.getAverageMZ()));
  }

  /**
   * Checks for links and adds those as partner rows
   * 
   * @param row
   * @param pid
   */
  public void addAllLinksTo(PeakListRow row, IonIdentity pid) {
    double nmass = pid.getIonType().getMass(row.getAverageMZ());
    this.entrySet().stream().forEach(e -> {
      if (e.getKey().getID() != row.getID()) {
        double pmass = getMass(e);
        if (mzTolerance.checkWithinTolerance(pmass, nmass)) {
          // add to both
          pid.addPartnerRow(e.getKey(), e.getValue());
          e.getValue().addPartnerRow(row, pid);
        }
      }
    });
  }

  public void delete() {
    entrySet().stream().forEach(e -> {
      e.getKey().removePeakIdentity(e.getValue());
    });
    clear();
  }

  /**
   * row has smallest id?
   * 
   * @param row
   * @return
   */
  public boolean hasSmallestID(PeakListRow row) {
    if (!containsKey(row))
      return false;
    else {
      return keySet().stream().noneMatch(r -> r.getID() < row.getID());
    }
  }

  /**
   * row has smallest id?
   * 
   * @param id
   * @return
   */
  public boolean hasSmallestID(int id) {
    return keySet().stream().anyMatch(r -> r.getID() == id)
        && keySet().stream().noneMatch(r -> r.getID() < id);
  }

  /**
   * Correlation group id (if existing) is always the one of the first entry
   * 
   * @return correlation group id or -1
   */
  public int getCorrID() {
    if (isEmpty())
      return -1;
    return keySet().iterator().next().getGroupID();
  }

  /**
   * Checks if all entries are in the same correlation group
   * 
   * @return correlation group id or -1
   */
  public boolean allSameCorrGroup() {
    if (isEmpty())
      return true;
    int cid = getCorrID();
    for (PeakListRow r : keySet()) {
      if (r.getGroupID() != cid)
        return false;
    }
    return true;
  }

  public MZTolerance getMZTolerance() {
    return mzTolerance;
  }

  public void setID(int i) {
    id = i;
    setNetworkToAllRows();
  }

  public void recalcConnections() {
    // Do not need to do this?
    // for (Entry<PeakListRow, ESIAdductIdentity> a : entrySet()) {
    // ESIAdductIdentity adduct = a.getValue();
    // if (adduct.getA().getAbsCharge() > 0)
    // adduct.resetLinks();
    // }

    // add all links
    for (Entry<PeakListRow, IonIdentity> a : entrySet()) {
      IonIdentity adduct = a.getValue();
      if (adduct.getIonType().getAbsCharge() > 0)
        addAllLinksTo(a.getKey(), adduct);
    }
  }

}
