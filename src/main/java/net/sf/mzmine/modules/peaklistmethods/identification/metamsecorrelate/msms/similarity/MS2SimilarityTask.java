/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.similarity;

import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import com.google.common.util.concurrent.AtomicDouble;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.RowGroupList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MS2SimilarityProviderGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RMap;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.util.maths.similarity.Similarity;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.scans.ScanMZDiffConverter;

public class MS2SimilarityTask extends AbstractTask {
  // Logger.
  private static final Logger LOG = Logger.getLogger(MS2SimilarityTask.class.getName());

  public static Function<List<DataPoint[]>, Integer> DIFF_OVERLAP =
      list -> ScanMZDiffConverter.getOverlapOfAlignedDiff(list, 0, 1);
  public static Function<List<DataPoint[]>, Integer> SIZE_OVERLAP = List::size;

  private AtomicDouble stageProgress;

  private final MZmineProject project;
  private String massList;
  private PeakListRow[] rows;
  private double maxMassDiff;
  private int minMatch;
  private int minDP;
  private int maxDPForDiff = 25;

  private R2RMap<R2RMS2Similarity> map;

  private RowGroupList groups;

  private MS2SimilarityProviderGroup group;


  /**
   * Create the task. to run on list of groups
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public MS2SimilarityTask(final MZmineProject project, final ParameterSet parameterSet,
      RowGroupList groups) {
    this.project = project;
    // performed on groups
    this.groups = groups;

    stageProgress = new AtomicDouble(0);
  }

  /**
   * Create the task on set of rows
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public MS2SimilarityTask(final MZmineProject project, final ParameterSet parameterSet,
      PeakListRow[] rows) {
    this.project = project;
    this.rows = rows;

    stageProgress = new AtomicDouble(0);
  }

  /**
   * Create the task on single group (the result is automatically set to the group
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public MS2SimilarityTask(final MZmineProject project, final ParameterSet parameterSet,
      MS2SimilarityProviderGroup group) {
    this.project = project;
    this.group = group;

    stageProgress = new AtomicDouble(0);
  }

  @Override
  public double getFinishedPercentage() {
    return stageProgress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Check similarity of MSMS scans (mass lists)";
  }

  @Override
  public void run() {
    doCheck();
  }

  public void doCheck() {
    if (group != null)
      map = checkGroup(group, massList, maxMassDiff, minMatch, minDP, maxDPForDiff);
    else if (rows != null)
      map = checkRows(rows, massList, maxMassDiff, minMatch, minDP, maxDPForDiff);
    else if (groups != null)
      checkGroupList(this, stageProgress, groups, massList, maxMassDiff, minMatch, minDP,
          maxDPForDiff);
  }

  /**
   * Resulting map
   * 
   * @return
   */
  public R2RMap<R2RMS2Similarity> getMap() {
    return map;
  }


  public static void checkGroupList(AbstractTask task, AtomicDouble stageProgress,
      RowGroupList groups, String massList, double maxMassDiff, int minMatch, int minDP,
      int maxDPForDiff) {
    LOG.info("Calc MS/MS similarity of groups");
    final int size = groups.size();
    groups.parallelStream().forEach(g -> {
      if (!task.isCanceled()) {
        if (g instanceof MS2SimilarityProviderGroup)
          checkGroup((MS2SimilarityProviderGroup) g, massList, maxMassDiff, minMatch, minDP,
              maxDPForDiff);
        stageProgress.addAndGet(1d / size);
      }
    });
  }

  /**
   * Checks for MS2 similarity of all rows in a group. the resulting map is set to the groups3
   * 
   * 
   * @param g
   * @param massList
   * @param maxMassDiff
   * @param minMatch
   * @param minDP
   * @param maxDPForDiff
   * @return
   */
  public static R2RMap<R2RMS2Similarity> checkGroup(MS2SimilarityProviderGroup g, String massList,
      double maxMassDiff, int minMatch, int minDP, int maxDPForDiff) {
    R2RMap<R2RMS2Similarity> map = checkRows(g.toArray(new PeakListRow[g.size()]), massList,
        maxMassDiff, minMatch, minDP, maxDPForDiff);

    g.setMS2SimilarityMap(map);
    return map;
  }

  public static R2RMap<R2RMS2Similarity> checkRows(PeakListRow[] rows, String massList,
      double maxMassDiff, int minMatch, int minDP, int maxDPForDiff) {
    R2RMap<R2RMS2Similarity> map = new R2RMap<>();
    for (int i = 0; i < rows.length - 1; i++) {
      for (int j = 1; j < rows.length; j++) {
        R2RMS2Similarity r2r =
            checkR2R(rows[i], rows[j], massList, maxMassDiff, minDP, minMatch, maxDPForDiff);
        if (r2r != null)
          map.add(rows[i], rows[j], r2r);
      }
    }
    return map;
  }

  public static R2RMS2Similarity checkR2R(PeakListRow a, PeakListRow b, String massList,
      double maxMassDiff, int minDP, int minMatch, int maxDPForDiff) {
    R2RMS2Similarity r2r = new R2RMS2Similarity(a, b);

    MZTolerance mzTol = new MZTolerance(maxMassDiff, 0);
    for (Feature fa : a.getPeaks()) {
      DataPoint[] dpa = getMassList(fa, massList);
      if (dpa != null && dpa.length >= minDP) {
        // create mass diff array
        DataPoint[] massDiffA = ScanMZDiffConverter.getAllMZDiff(dpa, maxMassDiff, maxDPForDiff);
        for (Feature fb : b.getPeaks()) {
          DataPoint[] dpb = getMassList(fb, massList);
          if (dpb != null && dpb.length >= minDP) {
            // align and check spectra
            MS2Similarity spectralSim = createMS2Sim(mzTol, dpa, dpb, minMatch, SIZE_OVERLAP);

            // alignment and sim of neutral losses
            MS2Similarity massDiffSim = createMS2Sim(mzTol, massDiffA,
                ScanMZDiffConverter.getAllMZDiff(dpb, maxMassDiff, maxDPForDiff), minMatch,
                DIFF_OVERLAP);

            if (massDiffSim != null)
              r2r.addMassDiffSim(massDiffSim);
            if (spectralSim != null)
              r2r.addSpectralSim(spectralSim);
          }
        }
      }
    }
    return r2r.size() > 0 ? r2r : null;
  }

  private static MS2Similarity createMS2Sim(MZTolerance mzTol, DataPoint[] a, DataPoint[] b,
      double minMatch, Function<List<DataPoint[]>, Integer> overlapFunction) {
    // align
    List<DataPoint[]> aligned = ScanAlignment.align(mzTol, b, a);
    aligned = ScanAlignment.removeUnaligned(aligned);
    // overlapping mass diff
    int overlap = overlapFunction.apply(aligned);

    if (overlap >= minMatch) {
      // cosine
      double[][] diffArray = ScanAlignment.toIntensityArray(aligned);
      double diffCosine = Similarity.COSINE.calc(diffArray);
      return new MS2Similarity(diffCosine, overlap);
    }
    return null;
  }

  private static DataPoint[] getMassList(Feature fa, String massList) {
    if (fa.getMostIntenseFragmentScanNumber() <= 0)
      return null;
    return fa.getDataFile().getScan(fa.getMostIntenseFragmentScanNumber()).getMassList(massList)
        .getDataPoints();
  }

}
