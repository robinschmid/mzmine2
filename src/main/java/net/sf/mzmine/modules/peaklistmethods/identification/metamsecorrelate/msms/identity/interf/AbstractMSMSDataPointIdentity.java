package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf;

import net.sf.mzmine.datamodel.DataPoint;

public abstract class AbstractMSMSDataPointIdentity implements AbstractMSMSIdentity {

  private DataPoint dp;

  public AbstractMSMSDataPointIdentity(DataPoint dp) {
    super();
    this.dp = dp;
  }

  public DataPoint getDp() {
    return dp;
  }

}
