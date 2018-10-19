package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public abstract class AbstractMSMSDataPointIdentity extends AbstractMSMSIdentity {

  private DataPoint dp;

  public AbstractMSMSDataPointIdentity(MZTolerance mzTolerance, DataPoint dp) {
    super(mzTolerance);
    this.dp = dp;
  }

  public AbstractMSMSDataPointIdentity(DataPoint dp) {
    this(null, dp);
  }


  public DataPoint getDp() {
    return dp;
  }

  public double getMZ() {
    return dp.getMZ();
  }

}
