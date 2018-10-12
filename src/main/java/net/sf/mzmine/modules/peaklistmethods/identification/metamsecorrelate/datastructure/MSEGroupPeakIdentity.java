package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class MSEGroupPeakIdentity extends SimplePeakIdentity {
  private PKLRowGroup group;

  public MSEGroupPeakIdentity(PKLRowGroup group) {
    this.group = group;
    int i = group.getGroupID();
    setPropertyValue(PROPERTY_NAME, "G(" + (i < 100 ? (i < 10 ? "00" : "0") : "") + i + ")");
  }

  public PKLRowGroup getGroup() {
    return group;
  }

  /**
   * Finds the first group identity
   * 
   * @param r
   * @return
   */
  public static MSEGroupPeakIdentity getIdentityOf(PeakListRow r) {
    if (r == null)
      return null;

    for (PeakIdentity pi : r.getPeakIdentities()) {
      if (pi instanceof MSEGroupPeakIdentity)
        return (MSEGroupPeakIdentity) pi;
    }
    return null;
  }
}
