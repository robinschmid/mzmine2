package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation;

import java.util.ArrayList;
import net.sf.mzmine.datamodel.PeakListRow;

public class AnnotationNetwork extends ArrayList<PeakListRow> {
  private final int id;

  public AnnotationNetwork(int id) {
    super();
    this.id = id;
  }

  public int getID() {
    return id;
  }
}
