package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.Comparator;

public class PKLRowGroupList extends ArrayList<RowGroup> {

  /**
   * Sort by average retention time
   */
  public void sortByRT() {
    this.sort(new Comparator<RowGroup>() {
      @Override
      public int compare(RowGroup a, RowGroup b) {
        return Double.compare(a.getCenterRT(), b.getCenterRT());
      }
    });
  }

}
