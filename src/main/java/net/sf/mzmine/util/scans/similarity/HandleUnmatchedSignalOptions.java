package net.sf.mzmine.util.scans.similarity;

public enum HandleUnmatchedSignalOptions {
  KEEP_ALL_AND_MATCH_TO_ZERO, REMOVE_ALL, KEEP_LIBRARY_SIGNALS, KEEP_EXPERIMENTAL_SIGNALS;

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }
}
