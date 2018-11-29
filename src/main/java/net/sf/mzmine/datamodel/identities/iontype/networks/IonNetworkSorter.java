package net.sf.mzmine.datamodel.identities.iontype.networks;

import java.util.Comparator;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * Sort ion identity networks
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class IonNetworkSorter implements Comparator<AnnotationNetwork> {
  private SortingProperty property;
  private SortingDirection direction;

  public IonNetworkSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  @Override
  public int compare(AnnotationNetwork a, AnnotationNetwork b) {
    Double va = getValue(a);
    Double vb = getValue(b);

    if (direction == SortingDirection.Ascending)
      return va.compareTo(vb);
    else
      return vb.compareTo(va);
  }

  private Double getValue(AnnotationNetwork net) {
    switch (property) {
      case Height:
        return net.getHeightSum();
      case MZ:
        return net.getNeutralMass();
      case RT:
        return net.getAvgRT();
      case ID:
        return (double) net.getID();
    }

    // We should never get here, so throw exception
    throw (new IllegalStateException());
  }

}
