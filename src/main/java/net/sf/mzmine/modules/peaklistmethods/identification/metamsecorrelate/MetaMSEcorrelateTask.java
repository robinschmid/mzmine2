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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation.FeatureShapeCorrelationParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation.InterSampleIntCorrParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroupList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RFullCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter.OverlapResult;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.AnnotationNetwork;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary.CheckMode;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.MSAnnMSMSCheckParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.MSAnnMSMSCheckTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class MetaMSEcorrelateTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MetaMSEcorrelateTask.class.getName());

  public enum Stage {
    CORRELATION_ANNOTATION(0.7), GROUPING(0.75), ANNOTATION(0.95), REFINEMENT(1d);
    private double finalProgress;

    Stage(double finalProgress) {
      this.finalProgress = finalProgress;
    }

    public double getFinalProgress() {
      return finalProgress;
    }
  }


  private AtomicDouble stageProgress = new AtomicDouble(0);
  private int totalRows;

  private final ParameterSet parameters;
  private final MZmineProject project;
  // GENERAL
  private final PeakList peakList;
  private final RTTolerance rtTolerance;
  private boolean autoSuffix;
  private String suffix;

  // ADDUCTS
  private MSAnnotationLibrary library;
  private final boolean searchAdducts;
  // annotate only the ones in corr groups
  private boolean annotateOnlyCorrelated;
  private CheckMode adductCheckMode;
  private double minAdductHeight;
  // MSMS refinement
  private boolean doMSMSchecks;
  private MSAnnMSMSCheckParameters msmsChecks;

  // GROUP and MIN SAMPLES FILTER
  private final boolean useGroups;
  private final String groupingParameter;
  /**
   * Minimum percentage of samples (in group if useGroup) that have to contain a feature
   */
  private final MinimumFeatureFilter minFFilter;

  // FEATURE SHAPE CORRELATION
  // correlation r to identify negative correlation
  private SimilarityMeasure shapeSimMeasure;
  private final double minShapeCorrR;
  private final double noiseLevelShapeCorr;
  private final int minCorrelatedDataPoints;

  // MAX INTENSITY PROFILE CORRELATION ACROSS SAMPLES
  private final boolean useMaxICorrFilter;
  private final double minMaxICorr;
  private final int minDPMaxICorr;


  private Stage stage;

  // output
  private MSEGroupedPeakList groupedPKL;
  private boolean performAnnotationRefinement;
  private AnnotationRefinementParameters refineParam;


  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public MetaMSEcorrelateTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakList) {
    this.project = project;
    this.peakList = peakList;
    parameters = parameterSet;

    totalRows = 0;

    // sample groups parameter
    useGroups = parameters.getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).getValue();
    groupingParameter = (String) parameters.getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER)
        .getEmbeddedParameter().getValue();
    // by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
    MinimumFeaturesFilterParameters minS = (MinimumFeaturesFilterParameters) parameterSet
        .getParameter(MetaMSEcorrelateParameters.MIN_SAMPLES_FILTER).getEmbeddedParameters();
    minFFilter =
        minS.createFilterWithGroups(project, peakList.getRawDataFiles(), groupingParameter);

    // tolerances
    rtTolerance = parameterSet.getParameter(MetaMSEcorrelateParameters.RT_TOLERANCE).getValue();

    // FEATURE SHAPE CORRELATION
    FeatureShapeCorrelationParameters corrp = (FeatureShapeCorrelationParameters) parameterSet
        .getParameter(MetaMSEcorrelateParameters.FSHAPE_CORRELATION).getEmbeddedParameters();
    // filter
    // start with high abundant features >= mainPeakIntensity
    // In this way we directly filter out groups with no abundant features
    // fill in smaller features after
    minShapeCorrR =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA).getValue();
    shapeSimMeasure = corrp.getParameter(FeatureShapeCorrelationParameters.MEASURE).getValue();
    noiseLevelShapeCorr =
        corrp.getParameter(FeatureShapeCorrelationParameters.NOISE_LEVEL_PEAK_SHAPE).getValue();
    minCorrelatedDataPoints =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();

    // ADDUCTS
    searchAdducts = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_LIBRARY).getValue();
    MSAnnotationParameters annParam = (MSAnnotationParameters) parameterSet
        .getParameter(MetaMSEcorrelateParameters.ADDUCT_LIBRARY).getEmbeddedParameters();
    library = new MSAnnotationLibrary(annParam);

    minAdductHeight = annParam.getParameter(MSAnnotationParameters.MIN_HEIGHT).getValue();
    adductCheckMode = annParam.getParameter(MSAnnotationParameters.CHECK_MODE).getValue();

    annotateOnlyCorrelated =
        parameterSet.getParameter(MetaMSEcorrelateParameters.ANNOTATE_ONLY_GROUPED).getValue();


    // MSMS refinement
    doMSMSchecks = annParam.getParameter(MSAnnotationParameters.MSMS_CHECK).getValue();
    msmsChecks = annParam.getParameter(MSAnnotationParameters.MSMS_CHECK).getEmbeddedParameters();


    performAnnotationRefinement =
        parameterSet.getParameter(MetaMSEcorrelateParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = parameterSet.getParameter(MetaMSEcorrelateParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();

    // END OF ADDUCTS AND REFINEMENT

    // intensity correlation across samples
    useMaxICorrFilter =
        parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION).getValue();
    minMaxICorr = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleIntCorrParameters.MIN_CORRELATION)
        .getValue();
    minDPMaxICorr = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleIntCorrParameters.MIN_DP).getValue();

    // suffix
    autoSuffix = parameters.getParameter(MetaMSEcorrelateParameters.SUFFIX).getValue();

    if (autoSuffix)
      suffix = MessageFormat.format("corr {2} r>={0} dp>={1}, {3}", minShapeCorrR,
          minCorrelatedDataPoints, shapeSimMeasure, searchAdducts ? "MS annot" : "");
    else
      suffix = parameters.getParameter(MetaMSEcorrelateParameters.SUFFIX).getEmbeddedParameter()
          .getValue();
  }

  @Override
  public double getFinishedPercentage() {
    if (stage == null)
      return 0;
    else {
      double prevProgress =
          stage.ordinal() == 0 ? 0 : Stage.values()[stage.ordinal() - 1].getFinalProgress();
      return prevProgress + (stage.getFinalProgress() - prevProgress) * stageProgress.get();
    }
  }

  @Override
  public String getTaskDescription() {
    return "Identification of groups in " + peakList.getName() + " scan events (lists)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    LOG.info("Starting MSE correlation search in " + peakList.getName() + " peaklists");
    try {
      if (isCanceled())
        return;

      // create new PKL for grouping
      groupedPKL = new MSEGroupedPeakList(peakList.getRawDataFiles(), peakList);
      // find groups and size
      if (useGroups) {
        groupedPKL.setSampleGroupsParameter(minFFilter.getGroupParam());
        groupedPKL.setSampleGroups(minFFilter.getGroupSizeMap());
      }

      // MAIN STEP
      // create correlation map
      setStage(Stage.CORRELATION_ANNOTATION);
      R2RCorrMap corrMap = new R2RCorrMap(rtTolerance, minFFilter);

      // do R2R comparison correlation
      // might also do annotation if selected
      doR2RComparison(groupedPKL, corrMap);
      if (isCanceled())
        return;

      LOG.info("Corr: Starting to group by correlation");
      setStage(Stage.GROUPING);
      PKLRowGroupList groups = corrMap.createCorrGroups(groupedPKL, minShapeCorrR, stageProgress);

      // refine groups
      refineGroups(groups);

      if (isCanceled())
        return;
      // refinement:
      // filter by avg correlation in group
      // delete single connections between sub networks
      if (groups != null) {
        // set groups to pkl
        groupedPKL.setCorrelationMap(corrMap);
        groupedPKL.setGroups(groups);


        // annotation at groups stage
        if (searchAdducts && annotateOnlyCorrelated) {
          LOG.info("Corr: Annotation of groups only");
          setStage(Stage.ANNOTATION);
          AtomicInteger compared = new AtomicInteger(0);
          AtomicInteger annotPairs = new AtomicInteger(0);
          // for all groups
          groups.parallelStream().forEach(g -> {
            annotateGroup(g, compared, annotPairs);
            stageProgress.addAndGet(1d / groups.size());
          });

          LOG.info("Corr: A total of " + compared.get() + " row2row adduct comparisons with "
              + annotPairs.get() + " annotation pairs");
        }

        // refinement and network creation
        if (searchAdducts) {
          setStage(Stage.REFINEMENT);
          // create networks
          LOG.info("Corr: create annotation network numbers");
          List<AnnotationNetwork> nets = MSAnnotationNetworkLogic
              .createAnnotationNetworks(groupedPKL, library.getMzTolerance(), true);

          // refinement of adducts
          // do MSMS check for multimers
          if (doMSMSchecks) {
            LOG.info("Corr: MSMS annotation refinement");
            MSAnnMSMSCheckTask task = new MSAnnMSMSCheckTask(project, msmsChecks, groupedPKL);
            task.doCheck();
          }

          // refinement
          if (performAnnotationRefinement) {
            LOG.info("Corr: Refine annotations");
            AnnotationRefinementTask ref =
                new AnnotationRefinementTask(project, refineParam, groupedPKL);
            ref.refine();
          }

          // recalc annotation networks
          MSAnnotationNetworkLogic.recalcAllAnnotationNetworks(nets, true);

          // show all annotations with the highest count of links
          LOG.info("Corr: show most likely annotations");
          MSAnnotationNetworkLogic.showMostlikelyAnnotations(groupedPKL, true);
        }

        // add to project
        project.addPeakList(groupedPKL);

        // do deisotoping
        deisotopeGroups();
        // do adduct search
        // searchAdducts();
        // Add task description to peakList.
        groupedPKL.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
            "Correlation grouping and identification of adducts", parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
          desktop.getMainWindow().repaint();

        // Done.
        setStatus(TaskStatus.FINISHED);
        LOG.info("Finished correlation grouping and adducts search in " + peakList);
      }
    } catch (

    Exception t) {
      LOG.log(Level.SEVERE, "Correlation and adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  /**
   * Recalc
   * 
   * @param groups
   */
  private void refineGroups(PKLRowGroupList groups) {
    // TODO refinemet
    // refineGroup(groups, 0);
  }


  private void refineGroup(PKLRowGroupList groups, int i) {
    if (i < groups.size()) {
      PKLRowGroup g = groups.get(i);
      // find row with highest number of negative markers and lowest number of partners
      List<OverlapResult>[] negative = findNegativeMarkers(g);

      int maxRow = -1;
      for (int r = 0; r < g.size(); r++) {
        if (!negative[r].isEmpty() && negative[r].size() > maxRow)
          maxRow = r;
      }

      if (maxRow != -1) {
        // remove
      }
    }
  }

  private List<OverlapResult>[] findNegativeMarkers(PKLRowGroup g) {
    RawDataFile[] raw = groupedPKL.getRawDataFiles();
    List<OverlapResult>[] negative = new ArrayList[g.size()];
    for (int r = 0; r < g.size(); r++) {
      negative[r] = new ArrayList<OverlapResult>();
      for (int p = 0; p < g.size(); p++) {
        if (r != p) {
          // add only negative
          OverlapResult overlap =
              minFFilter.filterMinFeaturesOverlap(raw, g.get(r), g.get(p), rtTolerance);
          if (!overlap.equals(OverlapResult.TRUE))
            negative[r].add(overlap);
        }
      }
    }
    return negative;
  }

  /**
   * Annotates all rows in a group
   * 
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(PKLRowGroup g, AtomicInteger compared, AtomicInteger annotPairs) {
    for (int i = 0; i < g.size() - 1; i++) {
      // check against existing networks


      for (int k = i + 1; k < g.size(); k++) {
        compared.incrementAndGet();
        // check for adducts in library
        ESIAdductType[] id =
            library.findAdducts(peakList, g.get(i), g.get(k), adductCheckMode, minAdductHeight);
        if (id != null)
          annotPairs.incrementAndGet();
      }
    }
  }

  private void setStage(Stage grouping) {
    stage = grouping;
    stageProgress.set(0d);
  }

  /**
   * Correlation and adduct network creation
   * 
   * @param peakList
   * @return
   */
  private void doR2RComparison(MSEGroupedPeakList peakList, R2RCorrMap map) throws Exception {
    LOG.info("Corr: Creating row2row correlation map");
    PeakListRow rows[] = peakList.getRows();
    totalRows = rows.length;
    final RawDataFile raw[] = peakList.getRawDataFiles();

    // sort by avgRT
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));

    // for all rows
    AtomicInteger annotPairs = new AtomicInteger(0);
    AtomicInteger compared = new AtomicInteger(0);

    IntStream.range(0, rows.length - 1).parallel().forEach(i -> {
      if (!isCanceled()) {
        try {
          PeakListRow row = rows[i];
          // has a minimum number/% of features in all samples / in at least one groups
          if (minFFilter.filterMinFeatures(raw, row)) {
            for (int x = i + 1; x < totalRows; x++) {
              if (isCanceled())
                break;

              PeakListRow row2 = rows[x];

              // has a minimum number/% of overlapping features in all samples / in at least one
              // groups
              // or check RTRange
              OverlapResult overlap =
                  minFFilter.filterMinFeaturesOverlap(raw, row, row2, rtTolerance);
              if (overlap.equals(OverlapResult.TRUE)) {
                // correlate if in rt range
                R2RFullCorrelationData corr = corrR2R(raw, row, row2, minDPMaxICorr,
                    minCorrelatedDataPoints, noiseLevelShapeCorr);
                if (corr != null) {
                  // deletes correlations if criteria is not met
                  corr.validate(minMaxICorr,
                      shapeSimMeasure.equals(SimilarityMeasure.PEARSON) ? minShapeCorrR : 0,
                      shapeSimMeasure.equals(SimilarityMeasure.COSINE_SIM) ? minShapeCorrR : 0);
                  // check for correlation in min samples
                  if (corr.hasFeatureShapeCorrelation())
                    checkMinFCorrelation(minFFilter, corr);
                  // still valid?
                  if (corr.isValid()) {
                    map.add(row, row2, corr);
                  }
                }

                // search directly? or search later in corr group?
                if (searchAdducts && !annotateOnlyCorrelated) {
                  compared.incrementAndGet();
                  // check for adducts in library
                  ESIAdductType[] id =
                      library.findAdducts(peakList, row, row2, adductCheckMode, minAdductHeight);
                  if (id != null)
                    annotPairs.incrementAndGet();
                }
              }
            }
          }
          stageProgress.addAndGet(1d / totalRows);
        } catch (Exception e) {
          LOG.log(Level.SEVERE, "Error in parallel R2Rcomparison", e);
          throw new MSDKRuntimeException(e);
        }
      }
    });

    // number of f2f correlations
    int nR2Rcorr = 0;
    int nF2F = 0;
    for (R2RCorrelationData r2r : map.values()) {
      if (r2r instanceof R2RFullCorrelationData) {
        R2RFullCorrelationData data = (R2RFullCorrelationData) r2r;
        if (data.hasFeatureShapeCorrelation()) {
          nR2Rcorr++;
          nF2F += data.getCorrPeakShape().size();
        }
      }
    }

    LOG.info(MessageFormat.format(
        "Corr: Correlations done with {0} R2R correlations and {1} F2F correlations", nR2Rcorr,
        nF2F));
  }

  /**
   * Final check if there are enough F2FCorrelations in samples and groups
   * 
   * @param minFFilter
   * @param corr
   */
  private void checkMinFCorrelation(MinimumFeatureFilter minFFilter, R2RFullCorrelationData corr) {
    List<RawDataFile> raw = new ArrayList<>();
    for (Entry<RawDataFile, CorrelationData> e : corr.getCorrPeakShape().entrySet())
      if (e.getValue() != null && e.getValue().isValid())
        raw.add(e.getKey());
    boolean hasCorrInSamples = minFFilter.filterMinFeatures(peakList.getRawDataFiles(), raw);
    if (!hasCorrInSamples) {
      // delete corr peak shape
      corr.setCorrPeakShape(null);
    }
  }

  /**
   * direct exclusion for high level filtering check rt of all peaks of all raw files
   * 
   * @param row
   * @param row2
   * @param minHeight minimum feature height to check for RT
   * @return true only if there was at least one RawDataFile with features in both rows with
   *         height>minHeight and within rtTolerance
   */
  public boolean checkRTRange(RawDataFile[] raw, PeakListRow row, PeakListRow row2,
      double minHeight, RTTolerance rtTolerance) {
    for (int r = 0; r < raw.length; r++) {
      Feature f = row.getPeak(raw[r]);
      Feature f2 = row2.getPeak(raw[r]);
      if (f != null && f2 != null && f.getHeight() >= minHeight && f2.getHeight() >= minHeight
          && rtTolerance.checkWithinTolerance(f.getRT(), f2.getRT())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Correlate all f2f and create row 2 row correlation data
   * 
   * @param raw
   * @param testRow
   * @param row
   * @return R2R correlation or null if invalid/no correlation
   */
  public static R2RFullCorrelationData corrR2R(RawDataFile[] raw, PeakListRow testRow,
      PeakListRow row, int minDPMaxICorr, int minCorrelatedDataPoints, double noiseLevelShapeCorr)
      throws Exception {
    CorrelationData iProfileR = corrRowToRowIProfile(raw, testRow, row, minDPMaxICorr);
    Map<RawDataFile, CorrelationData> fCorr =
        corrRowToRowFeatureShape(raw, testRow, row, minCorrelatedDataPoints, noiseLevelShapeCorr);

    if (fCorr.isEmpty())
      fCorr = null;

    R2RFullCorrelationData rCorr = new R2RFullCorrelationData(testRow, row, iProfileR, fCorr);
    if (rCorr.isValid())
      return rCorr;
    else
      return null;
  }

  /**
   * correlates the height profile of one row to another NO escape routine
   * 
   * @param raw
   * @param row
   * @param g
   * @return Correlation data of i profile of max i (or null if no correlation)
   */
  public static CorrelationData corrRowToRowIProfile(final RawDataFile raw[], PeakListRow row,
      PeakListRow g, int minDPMaxICorr) {
    List<double[]> data = new ArrayList<>();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // I profile correlation
        // TODO: low value imputation?
        double I1 = f1.getHeight();
        double I2 = f2.getHeight();
        data.add(new double[] {I1, I2});
      }
    }
    // TODO weighting of intensity corr
    if (data.size() < minDPMaxICorr)
      return null;
    else
      return CorrelationData.create(data);
  }

  /**
   * Correlation of feature to feature shapes in all RawDataFiles
   * 
   * @param raw
   * @param row
   * @param g
   * @return Map of feature shape correlation data (can be empty NON null)
   * @throws Exception
   */
  public static Map<RawDataFile, CorrelationData> corrRowToRowFeatureShape(final RawDataFile raw[],
      PeakListRow row, PeakListRow g, int minCorrelatedDataPoints, double noiseLevelShapeCorr)
      throws Exception {
    HashMap<RawDataFile, CorrelationData> corrData = new HashMap<>();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // peak shape correlation
        CorrelationData data =
            corrFeatureShape(f1, f2, true, minCorrelatedDataPoints, noiseLevelShapeCorr);
        // enough data points
        if (data != null && data.getDPCount() >= minCorrelatedDataPoints)
          corrData.put(raw[r], data);
      }
    }
    return corrData;
  }

  /**
   * feature shape correlation
   * 
   * @param f1
   * @param f2
   * @return feature shape correlation or null if not possible not enough data points for a
   *         correlation
   * @throws Exception
   */
  public static CorrelationData corrFeatureShape(Feature f1, Feature f2, boolean sameRawFile,
      int minCorrelatedDataPoints, double noiseLevelShapeCorr) throws Exception {
    // Range<Double> rt1 = f1.getRawDataPointsRTRange();
    // Range<Double> rt2 = f2.getRawDataPointsRTRange();
    if (sameRawFile) {
      // scan numbers (not necessary 1,2,3...)
      int[] sn1 = f1.getScanNumbers();
      int[] sn2 = f2.getScanNumbers();

      if (sn1.length < minCorrelatedDataPoints || sn2.length < minCorrelatedDataPoints)
        return null;

      // find max of sn1
      int maxI = 0;
      double max = 0;
      for (int i = 0; i < sn1.length; i++) {
        double val = f1.getDataPoint(sn1[i]).getIntensity();
        if (val > max) {
          maxI = i;
          max = val;
        }
      }

      int offset2 = 0;
      // find corresponding scan in sn2
      for (int i = 0; i < sn2.length; i++) {
        if (sn1[maxI] == sn2[i]) {
          offset2 = i;
          break;
        }
      }

      // save max and min of intensity of val1(x)
      List<double[]> data = new ArrayList<double[]>();

      // add all data points <=max
      int i1 = maxI;
      int i2 = offset2;
      while (i1 >= 0 && i2 >= 0) {
        int s1 = sn1[i1];
        int s2 = sn2[i2];
        // add point, if not break
        if (s1 == s2) {
          if (!addDataPointToCorr(data, f1.getDataPoint(s1), f2.getDataPoint(s2),
              noiseLevelShapeCorr))
            break;
        }
        // end of peak found
        else
          break;
        i1--;
        i2--;
      }

      // add all dp>max
      i1 = maxI + 1;
      i2 = offset2 + 1;
      while (i1 < sn1.length && i2 < sn2.length) {
        int s1 = sn1[i1];
        int s2 = sn2[i2];
        if (s1 == s2) {
          if (!addDataPointToCorr(data, f1.getDataPoint(s1), f2.getDataPoint(s2),
              noiseLevelShapeCorr))
            break;
        }
        // end of peak found
        else
          break;
        i1++;
        i2++;
      }
      // return pearson r
      if (data.size() >= minCorrelatedDataPoints) {
        return CorrelationData.create(data);
      }
    } else {
      // TODO if different raw file search for same rt
      // impute rt/I values if between 2 data points
    }
    return null;
  }

  private static boolean addDataPointToCorr(List<double[]> data, DataPoint a, DataPoint b,
      double noiseLevel) {
    // add all data points over a given threshold
    // TODO raw data (not smoothed)
    if (a != null && b != null) {
      // raw data
      double val1 = a.getIntensity();
      double val2 = b.getIntensity();

      if (val1 >= noiseLevel && val2 >= noiseLevel) {
        data.add(new double[] {val1, val2});
        return true;
      } else
        return false;
    } else
      return false;
  }

  /**
   * counts all data points >= noiseLevel
   * 
   * @param f2
   * @return
   */
  private double countDPHigherThanNoise(Feature f) {
    int c = 0;
    for (int i = 0; i < f.getScanNumbers().length; i++) {
      double val = f.getDataPoint(f.getScanNumbers()[i]).getIntensity();
      if (val >= noiseLevelShapeCorr)
        c++;
    }
    return c;
  }

  /**
   * 1. Check for specific isotopes: 2. 13C 1. Check group for isotopes 2. Check raw data for
   * isotopes
   */
  private void deisotopeGroups() {
    // TODO Auto-generated method stub

  }

  /**
   * Combine groups across different scan events
   */
  private void combineGroups() {
    // TODO Auto-generated method stub

  }


  /**
   * find in source fragments based on intensity profile in MS1 and MSE scans
   * 
   */
  private void findInSourceFragments() {
    // TODO Auto-generated method stub

  }

}
