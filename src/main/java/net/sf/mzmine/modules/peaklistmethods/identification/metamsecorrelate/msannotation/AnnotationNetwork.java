package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.HashMap;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * An annotation network full of ions that point to the same neutral molecule (neutral mass)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class AnnotationNetwork extends HashMap<PeakListRow, ESIAdductIdentity> {
  private final int id;
  private Double neutralMass = null;
  // maximum deviation from neutral mass average
  private Double maxDev = null;
  private double avgRT;

  public AnnotationNetwork(int id) {
    super();
    this.id = id;
  }

  public int getID() {
    return id;
  }

  public void setNeutralMass(double neutralMass) {
    this.neutralMass = neutralMass;
  }

  public double getNeutralMass() {
    return neutralMass == null ? calcNeutralMass() : neutralMass;
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
    for (Entry<PeakListRow, ESIAdductIdentity> e : entrySet()) {
      mass += e.getValue().getA().getMass(e.getKey().getAverageMZ());
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
    for (Entry<PeakListRow, ESIAdductIdentity> e : entrySet()) {
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
    for (Entry<PeakListRow, ESIAdductIdentity> e : entrySet()) {
      double mass = e.getValue().getA().getMass(e.getKey().getAverageMZ());
      max = Math.max(Math.abs(neutralMass - mass), max);
    }
    maxDev = max;
    return maxDev;
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
}
