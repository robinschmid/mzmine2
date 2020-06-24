/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.scans.similarity;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.scans.ScanAlignment;

/**
 * The result of a {@link SpectralSimilarityFunction}.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SpectralSimilarity {
  // similarity score (depends on similarity function)
  private double score;
  // aligned signals in library and query spectrum
  private int overlap;
  // library intensity described by matched signals
  private double explainedLibraryIntensity = 0;
  private double totalLibraryIntensity = 0;
  private int explainedLibrarySignals = 0;
  private int totalLibrarySignals = 0;

  private double explainedQueryIntensity = 0;
  private double totalQueryIntensity = 0;
  private int explainedQuerySignals = 0;
  private int totalQuerySignals = 0;
  // similarity function name
  private String funcitonName;

  // spectral data can be nullable to save memory
  // library and query spectrum (may be filtered)
  private @Nullable DataPoint[] library;
  private @Nullable DataPoint[] query;
  // aligned data points (found in both the library[0] and the query[1] sepctrum)
  // alinged[library, query][data points]
  private @Nullable DataPoint[][] aligned;

  // options to handle unmatched signals
  private HandleUnmatchedSignalOptions handleUnmatched;

  /**
   * The result of a {@link SpectralSimilarityFunction}.
   * 
   * @param funcitonName Similarity function name
   * @param score similarity score
   * @param overlap count of aligned data points in library and query spectrum
   */
  public SpectralSimilarity(String funcitonName, double score, int overlap) {
    this.funcitonName = funcitonName;
    this.score = score;
    this.overlap = overlap;
  }

  /**
   * The result of a {@link SpectralSimilarityFunction}.
   * 
   * @param funcitonName Similarity function name
   * @param score similarity score
   * @param overlap count of aligned data points in library and query spectrum
   * @param librarySpec library spectrum (or other) which was matched to querySpec (may be filtered)
   * @param querySpec query spectrum which was matched to librarySpec (may be filtered)
   * @param alignedDP aligned data points (alignedDP.get(data point index)[library/query spectrum])
   * @param handleUnmatched
   */
  public SpectralSimilarity(String funcitonName, double score, int overlap,
      @Nullable DataPoint[] librarySpec, @Nullable DataPoint[] querySpec,
      @Nullable List<DataPoint[]> alignedDP, HandleUnmatchedSignalOptions handleUnmatched) {
    this.handleUnmatched = handleUnmatched;
    DataPointSorter sorter = new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending);
    this.funcitonName = funcitonName;
    this.score = score;
    this.overlap = overlap;
    this.library = librarySpec;
    this.query = querySpec;



    if (alignedDP != null) {
      // filter unaligned
      List<DataPoint[]> filtered = ScanAlignment.removeUnaligned(alignedDP);
      aligned = ScanAlignment.convertBackToMassLists(filtered);

      // calculate spectral overlap before removing unaligned signals from filtered library/query
      calculateSpectralOverlap();

      for (DataPoint[] dp : aligned)
        Arrays.sort(dp, sorter);

      // filter from unmatched
      switch (handleUnmatched) {
        case KEEP_ALL_AND_MATCH_TO_ZERO:
        default:
          break;
        case KEEP_EXPERIMENTAL_SIGNALS:
          library = Arrays.stream(library)
              .filter(dp -> alignedDP.stream().anyMatch(alDP -> dp.equals(alDP[0])))
              .toArray(DataPoint[]::new);
          break;
        case KEEP_LIBRARY_SIGNALS:
          query = Arrays.stream(query)
              .filter(dp -> alignedDP.stream().anyMatch(alDP -> dp.equals(alDP[1])))
              .toArray(DataPoint[]::new);
          break;
        case REMOVE_ALL:
          library = Arrays.stream(library)
              .filter(dp -> alignedDP.stream().anyMatch(alDP -> dp.equals(alDP[0])))
              .toArray(DataPoint[]::new);
          query = Arrays.stream(query)
              .filter(dp -> alignedDP.stream().anyMatch(alDP -> dp.equals(alDP[1])))
              .toArray(DataPoint[]::new);
          break;
      }
    }
    if (this.library != null)
      Arrays.sort(this.library, sorter);
    if (this.query != null)
      Arrays.sort(this.query, sorter);
  }


  private void calculateSpectralOverlap() {
    explainedLibraryIntensity =
        Arrays.stream(aligned[0]).mapToDouble(DataPoint::getIntensity).sum();
    totalLibraryIntensity = Arrays.stream(library).mapToDouble(DataPoint::getIntensity).sum();
    explainedQueryIntensity = Arrays.stream(aligned[1]).mapToDouble(DataPoint::getIntensity).sum();
    totalQueryIntensity = Arrays.stream(query).mapToDouble(DataPoint::getIntensity).sum();


    explainedLibrarySignals = aligned[0].length;
    totalLibrarySignals = library.length;


    explainedQuerySignals = aligned[1].length;
    totalQuerySignals = query.length;
  }


  public double getExplainedLibraryIntensity() {
    return explainedLibraryIntensity;
  }

  public int getExplainedLibrarySignals() {
    return explainedLibrarySignals;
  }

  public double getTotalLibraryIntensity() {
    return totalLibraryIntensity;
  }

  public int getTotalLibrarySignals() {
    return totalLibrarySignals;
  }

  public double getExplainedQueryIntensity() {
    return explainedQueryIntensity;
  }

  public double getTotalQueryIntensity() {
    return totalQueryIntensity;
  }

  public int getExplainedQuerySignals() {
    return explainedQuerySignals;
  }

  public int getTotalQuerySignals() {
    return totalQuerySignals;
  }

  public double getExplainedLibraryIntensityRatio() {
    if (totalLibraryIntensity == 0)
      return 0;
    return explainedLibraryIntensity / totalLibraryIntensity;
  }

  public double getExplainedQueryIntensityRatio() {
    if (totalQueryIntensity == 0)
      return 0;
    return explainedQueryIntensity / totalQueryIntensity;
  }

  /**
   * Number of overlapping signals in both spectra
   * 
   * @return
   */
  public int getOverlap() {
    return overlap;
  }

  /**
   * Cosine similarity
   * 
   * @return
   */
  public double getScore() {
    return score;
  }

  /**
   * SPectralSimilarityFunction name
   * 
   * @return
   */
  public String getFunctionName() {
    return funcitonName;
  }

  /**
   * Library spectrum (usually filtered)
   * 
   * @return
   */
  public DataPoint[] getLibrary() {
    return library;
  }

  /**
   * Query spectrum (usually filtered)
   * 
   * @return
   */
  public DataPoint[] getQuery() {
    return query;
  }

  /**
   * All aligned data points of library(0) and query(1) spectrum
   * 
   * @return DataPoint[library, query][datapoints]
   */
  public DataPoint[][] getAlignedDataPoints() {
    return aligned;
  }
}
