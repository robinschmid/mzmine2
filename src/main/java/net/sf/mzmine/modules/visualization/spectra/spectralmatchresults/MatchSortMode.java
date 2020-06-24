package net.sf.mzmine.modules.visualization.spectra.spectralmatchresults;

/**
 * Sor
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public enum MatchSortMode {
  MATCH_SCORE, EXPLAINED_LIBRARY_INTENSITY, COMBINED;
  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
