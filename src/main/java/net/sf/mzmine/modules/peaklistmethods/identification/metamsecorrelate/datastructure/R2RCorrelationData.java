package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter.OverlapResult;

/**
 * row to row correlation (2 rows) Intensity profile and peak shape correlation
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrelationData {

  public enum NegativeMarker {
    // intensity range is not shared between these two rows
    // at least in one raw data file: the features are out of RT range
    // the features do not overlap with X % of their intensity
    AntiOverlap, //
    MinFeaturesRequirementNotMet, //
    OutOfRTRange; // Features are out of RT range

    public static NegativeMarker fromOverlapResult(OverlapResult overlap) {
      switch (overlap) {
        case AntiOverlap:
          return NegativeMarker.AntiOverlap;
        case BelowMinSamples:
          return NegativeMarker.MinFeaturesRequirementNotMet;
        case OutOfRTRange:
          return NegativeMarker.OutOfRTRange;
      }
      return null;
    }
  }

  // correlation of a to b
  // id A < id B
  private PeakListRow a, b;

  // ANTI CORRELATION MARKERS
  // to be used to exclude rows from beeing grouped
  private List<NegativeMarker> negativMarkers;

  public R2RCorrelationData(PeakListRow a, PeakListRow b) {
    if (a.getID() < b.getID()) {
      this.a = a;
      this.b = b;
    } else {
      this.b = a;
      this.a = b;
    }
  }

  /**
   * Stream all R2RFullCorrelationData found in PKLRowGroups (is distinct)
   * 
   * @param peakList
   * @return
   */
  public static Stream<R2RFullCorrelationData> streamFrom(PeakList peakList) {
    if (peakList.getGroups() == null)
      return Stream.empty();
    return peakList.getGroups().stream().filter(g -> g instanceof PKLRowGroup)
        .map(g -> ((PKLRowGroup) g).getCorr()).flatMap(Arrays::stream) // R2GCorr
        .flatMap(r2g -> r2g.getCorr() == null ? null
            : r2g.getCorr().stream() //
                .filter(r2r -> r2r.getIDA() == r2g.getRow().getID())); // a is always the lower id
  }

  /**
   * 
   * @return List of negativ markers (non-null)
   */
  public @Nonnull List<NegativeMarker> getNegativMarkers() {
    return negativMarkers == null ? new ArrayList<>() : negativMarkers;
  }

  public int getNegativMarkerCount() {
    return negativMarkers == null ? 0 : negativMarkers.size();
  }

  /**
   * Negativ marker for this correlation (exclude from further grouping)
   * 
   * @param nm
   */
  public void addNegativMarker(NegativeMarker nm) {
    if (negativMarkers == null)
      negativMarkers = new ArrayList<>();
    negativMarkers.add(nm);
  }

  public PeakListRow getRowA() {
    return a;
  }

  public PeakListRow getRowB() {
    return b;
  }

  public int getIDA() {
    return a == null ? 0 : a.getID();
  }

  public int getIDB() {
    return b == null ? 0 : b.getID();
  }

  public boolean hasFeatureShapeCorrelation() {
    return false;
  }

  public double getAvgShapeR() {
    return 0;
  }

  public double getAvgShapeCosineSim() {
    return 0;
  }

}
