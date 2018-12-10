package net.sf.mzmine.datamodel.identities.iontype.networks;

import java.util.Arrays;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;

public abstract class IonNetworkRelationInterf {

  public abstract String getName(IonNetwork net);

  public abstract String getDescription();

  public abstract IonNetwork[] getAllNetworks();

  public boolean isLowestIDNetwork(IonNetwork net) {
    return Arrays.stream(getAllNetworks()).noneMatch(n -> n.getID() < net.getID());
  }
}
