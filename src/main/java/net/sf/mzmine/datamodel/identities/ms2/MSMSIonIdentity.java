package net.sf.mzmine.datamodel.identities.ms2;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
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
