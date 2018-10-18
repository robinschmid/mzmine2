package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSDataPointIdentity;

/**
 * One MSMS signal identified by several x-mers (M , 2M, 3M ...)
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSMultimerIdentity extends AbstractMSMSDataPointIdentity {

  // the identified x-mer
  private ESIAdductType type;
  private List<MSMSMultimerIdentity> links;

  public MSMSMultimerIdentity(DataPoint dp, ESIAdductType type) {
    super(dp);
    this.type = type;
  }

  public List<MSMSMultimerIdentity> getLinks() {
    return links;
  }

  public void addLink(MSMSMultimerIdentity l) {
    if (links == null)
      links = new ArrayList<>();
    links.add(l);
  }

  @Override
  public String getName() {
    return type.getName();
  }

  public int getLinksCount() {
    return links == null ? 0 : links.size();
  }

  public int getMCount() {
    return type.getMolecules();
  }

  public ESIAdductType getType() {
    return type;
  }
}
