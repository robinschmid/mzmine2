package net.sf.mzmine.datamodel.identities.ms2;

import net.sf.mzmine.datamodel.identities.iontype.IonModification;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSMSModificationIdentity extends AbstractMSMSIdentity {

  private final IonModification mod;

  public MSMSModificationIdentity(MZTolerance mzTolerance, IonModification mod) {
    super(mzTolerance);
    this.mod = mod;
  }


  @Override
  public String getName() {
    return mod.getName();
  }


  public IonModification getMod() {
    return mod;
  }

  public double getMass() {
    return mod.getMass();
  }


  public double getAbsMass() {
    return Math.abs(getMass());
  }

}
