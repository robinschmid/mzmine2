package net.sf.mzmine.datamodel.identities.ms2;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSDataPointIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSMSDataPointIdentity extends AbstractMSMSDataPointIdentity {

  private String name;
  private DataPoint dp;

  public MSMSDataPointIdentity(MZTolerance mzTolerance, DataPoint dp, String name) {
    super(mzTolerance, dp);
    setName(name);
  }

  @Override
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

}
