package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf.AbstractMSMSDataPointIdentity;

public class MSMSDataPointIdentity extends AbstractMSMSDataPointIdentity {

  private String name;
  private DataPoint dp;

  public MSMSDataPointIdentity(DataPoint dp, String name) {
    super(dp);
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
