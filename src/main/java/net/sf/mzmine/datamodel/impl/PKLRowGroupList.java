package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.Comparator;

public class PKLRowGroupList extends ArrayList<PKLRowGroup> {

  /**
   * Sort by average retention time
   */
  public void sortByRT() {
    this.sort(new Comparator<PKLRowGroup>() {
      @Override
      public int compare(PKLRowGroup a, PKLRowGroup b) {
        return Double.compare(a.getCenterRT(), b.getCenterRT());
      }
    });
  }

}
