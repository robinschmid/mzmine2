package net.sf.mzmine.datamodel.identities.iontype.networks;

import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;

public class IonNetRelationPeakIdentity extends SimplePeakIdentity {
  private IonNetwork net;

  public IonNetRelationPeakIdentity(IonNetwork net, String name) {
    super(name);
    this.net = net;
  }

  public IonNetwork getNetwork() {
    return net;
  }

}
