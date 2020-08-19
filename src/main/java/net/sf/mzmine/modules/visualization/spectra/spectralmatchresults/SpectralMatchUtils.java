package net.sf.mzmine.modules.visualization.spectra.spectralmatchresults;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarity;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class SpectralMatchUtils {

  public static void sort(List<SpectralDBPeakIdentity> visibleMatches, MatchSortMode sorting,
      double factorScore) {
    switch (sorting) {
      case COMBINED:
        visibleMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double
            .compare(calcCombinedScore(b, factorScore), calcCombinedScore(a, factorScore)));
        break;
      case EXPLAINED_LIBRARY_INTENSITY:
        visibleMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double.compare(
            b.getSimilarity().getExplainedLibraryIntensityRatio(),
            a.getSimilarity().getExplainedLibraryIntensityRatio()));
        break;
      case MATCH_SCORE:
      default:
        visibleMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double
            .compare(b.getSimilarity().getScore(), a.getSimilarity().getScore()));
        break;
    }
  }

  public static double calcCombinedScore(SpectralDBPeakIdentity id, double factorScore) {
    return (id.getSimilarity().getScore() * factorScore
        + id.getSimilarity().getExplainedLibraryIntensityRatio()) / (factorScore + 1d);
  }

  /**
   * Collapse a list of matches to remove duplicates
   * 
   * @param totalMatches
   * @param compareFields
   * @param compareDataPoints
   * @param factorScore
   * @return
   */
  public static List<SpectralDBPeakIdentity> collapseList(List<SpectralDBPeakIdentity> totalMatches,
      DBEntryField[] compareFields, boolean compareDataPoints, double factorScore) {

    List<SpectralDBPeakIdentity> collapsedMatches = new ArrayList<>();

    Map<String, SpectralDBPeakIdentity> best = new HashMap<>();
    Map<String, SpectralDBPeakIdentity> explained = new HashMap<>();
    Map<String, SpectralDBPeakIdentity> combined = new HashMap<>();
    for (SpectralDBPeakIdentity match : totalMatches) {
      String key = generateKey(match, compareFields, compareDataPoints);
      compareAndAdd(match, key, best, MatchCompareMode.BEST, factorScore);
      compareAndAdd(match, key, explained, MatchCompareMode.EXPLAINED, factorScore);
      compareAndAdd(match, key, combined, MatchCompareMode.COMBINED, factorScore);
    }
    for (SpectralDBPeakIdentity e : best.values()) {
      collapsedMatches.add(e);
    }
    for (SpectralDBPeakIdentity e : explained.values()) {
      if (!collapsedMatches.contains(e))
        collapsedMatches.add(e);
    }
    for (SpectralDBPeakIdentity e : combined.values()) {
      if (!collapsedMatches.contains(e))
        collapsedMatches.add(e);
    }
    return collapsedMatches;
  }

  private static void compareAndAdd(SpectralDBPeakIdentity match, String key,
      Map<String, SpectralDBPeakIdentity> map, MatchCompareMode compare, double factorScore) {
    SpectralDBPeakIdentity other = map.get(key);
    if (other == null) {
      map.put(key, match);
      return;
    }
    SpectralSimilarity sim = other.getSimilarity();
    switch (compare) {
      case BEST:
        if (sim.getScore() < match.getSimilarity().getScore())
          map.put(key, match);
        break;
      case COMBINED:
        if (SpectralMatchUtils.calcCombinedScore(other, factorScore) < SpectralMatchUtils
            .calcCombinedScore(match, factorScore))
          map.put(key, match);
        break;
      case EXPLAINED:
        if (sim.getExplainedLibraryIntensityRatio() < match.getSimilarity()
            .getExplainedLibraryIntensityRatio())
          map.put(key, match);
        break;
    }
  }

  /**
   * Generate key to compare library entries
   * 
   * @param match
   * @param compareFields
   * @param compareDataPoints
   * @return
   */
  public static String generateKey(SpectralDBPeakIdentity match, DBEntryField[] compareFields,
      boolean compareDataPoints) {
    SpectralDBEntry e = match.getEntry();
    StringBuilder key = new StringBuilder();
    for (DBEntryField f : compareFields) {
      key.append(e.getField(f).orElse("NO").toString());
    }
    if (compareDataPoints) {
      key.append("_DP");
      key.append(e.getDataPoints().length);
    }

    return key.toString();
  }

}
