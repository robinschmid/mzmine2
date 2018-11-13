package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.IonType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSDataPointIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSIonIdentity extends AbstractMSMSDataPointIdentity {

  protected IonType type;

  public MSMSIonIdentity(MZTolerance mzTolerance, DataPoint dp, IonType b) {
    super(mzTolerance, dp);
    this.type = b;
  }

  @Override
  public String getName() {
    return type.toString(false);
  }

  public IonType getType() {
    return type;
  }
}
