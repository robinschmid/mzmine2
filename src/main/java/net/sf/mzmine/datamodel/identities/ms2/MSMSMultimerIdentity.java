package net.sf.mzmine.datamodel.identities.ms2;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.IonType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * One MSMS signal identified by several x-mers (M , 2M, 3M ...)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSMultimerIdentity extends MSMSIonIdentity {

  // the identified x-mer
  private List<MSMSMultimerIdentity> links;

  public MSMSMultimerIdentity(MZTolerance mzTolerance, DataPoint dp, IonType b) {
    super(mzTolerance, dp, b);
  }

  public List<MSMSMultimerIdentity> getLinks() {
    return links;
  }

  public void addLink(MSMSMultimerIdentity l) {
    if (links == null)
      links = new ArrayList<>();
    links.add(l);
  }

  public int getLinksCount() {
    return links == null ? 0 : links.size();
  }

  public int getMCount() {
    return getType().getMolecules();
  }

}
