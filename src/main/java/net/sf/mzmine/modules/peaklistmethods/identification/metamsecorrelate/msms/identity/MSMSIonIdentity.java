package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSDataPointIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

/**
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class MSMSIonIdentity extends AbstractMSMSDataPointIdentity {

  protected ESIAdductType type;

  public MSMSIonIdentity(MZTolerance mzTolerance, DataPoint dp, ESIAdductType type) {
    super(mzTolerance, dp);
    this.type = type;
  }

  @Override
  public String getName() {
    return type.toString(false);
  }

  public ESIAdductType getType() {
    return type;
  }
}
