package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.AdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSDataPointIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSIonIdentity extends AbstractMSMSDataPointIdentity {

  protected AdductType type;

  public MSMSIonIdentity(MZTolerance mzTolerance, DataPoint dp, AdductType type) {
    super(mzTolerance, dp);
    this.type = type;
  }

  @Override
  public String getName() {
    return type.toString(false);
  }

  public AdductType getType() {
    return type;
  }
}
