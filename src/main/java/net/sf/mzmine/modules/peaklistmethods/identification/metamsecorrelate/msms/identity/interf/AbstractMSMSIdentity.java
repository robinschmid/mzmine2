package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.interf;

import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public abstract class AbstractMSMSIdentity {

  // the mz tolerance that was used to find identity
  protected MZTolerance mzTolerance;


  public AbstractMSMSIdentity(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }

  public abstract String getName();

  /**
   * the mz tolerance that was used to find identity
   * 
   * @return
   */
  public MZTolerance getMzTolerance() {
    return mzTolerance;
  }

  /**
   * the mz tolerance that was used to find identity
   * 
   * @param mzTolerance
   */
  public void setMzTolerance(MZTolerance mzTolerance) {
    this.mzTolerance = mzTolerance;
  }
}
