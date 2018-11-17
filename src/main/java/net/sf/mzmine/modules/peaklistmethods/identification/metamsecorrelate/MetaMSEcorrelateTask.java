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
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.regression.SimpleRegression;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.MSAnnotationNetworkLogic;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.datamodel.impl.RowGroupList;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation.FeatureShapeCorrelationParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation.InterSampleHeightCorrParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RFullCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter.OverlapResult;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary.CheckMode;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.MSAnnMSMSCheckParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.MSAnnMSMSCheckTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.similarity.MS2SimilarityTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.maths.similarity.Similarity;

public class MetaMSEcorrelateTask extends AbstractTask {

  // Logger.
  private static final Logger LOG = Logger.getLogger(MetaMSEcorrelateTask.class.getName());

  public enum Stage {
    CORRELATION_ANNOTATION(0.5), GROUPING(0.6), MS2_SIMILARITY(0.8), ANNOTATION(0.95), REFINEMENT(
        1d);
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

  protected ParameterSet parameters;
  protected MZmineProject project;
  // GENERAL
  protected PeakList peakList;
  protected RTTolerance rtTolerance;
  protected boolean autoSuffix;
  protected String suffix;

  // ADDUCTS
  protected MSAnnotationLibrary library;
  protected boolean searchAdducts;
  // annotate only the ones in corr groups
  protected boolean annotateOnlyCorrelated;
  protected CheckMode adductCheckMode;
  // MSMS refinement
  protected boolean doMSMSchecks;
  protected MSAnnMSMSCheckParameters msmsChecks;

  // GROUP and MIN SAMPLES FILTER
  protected boolean useGroups;
  protected String groupingParameter;
  /**
   * Minimum percentage of samples (in group if useGroup) that have to contain a feature
   */
  protected MinimumFeatureFilter minFFilter;
  // min adduct height and feature height for minFFilter
  protected double minHeight;

  // FEATURE SHAPE CORRELATION
  // correlation r to identify negative correlation
  protected SimilarityMeasure shapeSimMeasure;
  protected boolean useTotalShapeCorrFilter;
  protected double minTotalShapeCorrR;
  protected double minShapeCorrR;
  protected double noiseLevelCorr;
  protected int minCorrelatedDataPoints;
  protected int minCorrDPOnFeatureEdge;

  // MAX INTENSITY PROFILE CORRELATION ACROSS SAMPLES
  protected SimilarityMeasure heightSimMeasure;
  protected boolean useHeightCorrFilter;
  protected double minHeightCorr;
  protected int minDPHeightCorr;

  // perform MS2Similarity check
  protected boolean checkMS2Similarity;

  // stage of processing
  private Stage stage;

  // output
  protected PeakList groupedPKL;
  protected boolean performAnnotationRefinement;
  protected AnnotationRefinementParameters refineParam;


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

    // height and noise
    noiseLevelCorr = parameters.getParameter(MetaMSEcorrelateParameters.NOISE_LEVEL).getValue();
    minHeight = parameters.getParameter(MetaMSEcorrelateParameters.MIN_HEIGHT).getValue();

    // by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
    MinimumFeaturesFilterParameters minS = (MinimumFeaturesFilterParameters) parameterSet
        .getParameter(MetaMSEcorrelateParameters.MIN_SAMPLES_FILTER).getEmbeddedParameters();
    minFFilter = minS.createFilterWithGroups(project, peakList.getRawDataFiles(), groupingParameter,
        minHeight);

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
    minCorrelatedDataPoints =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();
    minCorrDPOnFeatureEdge =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_DP_FEATURE_EDGE).getValue();

    // total corr
    useTotalShapeCorrFilter =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR).getValue();
    minTotalShapeCorrR = corrp.getParameter(FeatureShapeCorrelationParameters.MIN_TOTAL_CORR)
        .getEmbeddedParameter().getValue();
    // ADDUCTS
    searchAdducts = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_LIBRARY).getValue();
    MSAnnotationParameters annParam = parameterSet
        .getParameter(MetaMSEcorrelateParameters.ADDUCT_LIBRARY).getEmbeddedParameters();
    library = new MSAnnotationLibrary(annParam);

    adductCheckMode = annParam.getParameter(MSAnnotationParameters.CHECK_MODE).getValue();

    annotateOnlyCorrelated =
        parameterSet.getParameter(MetaMSEcorrelateParameters.ANNOTATE_ONLY_GROUPED).getValue();


    // MSMS refinement
    doMSMSchecks = annParam.getParameter(MSAnnotationParameters.MSMS_CHECK).getValue();
    msmsChecks = annParam.getParameter(MSAnnotationParameters.MSMS_CHECK).getEmbeddedParameters();


    performAnnotationRefinement =
        annParam.getParameter(MSAnnotationParameters.ANNOTATION_REFINEMENTS).getValue();
    refineParam = annParam.getParameter(MSAnnotationParameters.ANNOTATION_REFINEMENTS)
        .getEmbeddedParameters();

    // END OF ADDUCTS AND REFINEMENT

    // intensity correlation across samples
    useHeightCorrFilter =
        parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION).getValue();
    minHeightCorr = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_CORRELATION)
        .getValue();
    minDPHeightCorr = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MIN_DP).getValue();

    heightSimMeasure = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleHeightCorrParameters.MEASURE).getValue();


    // suffix
    autoSuffix = !parameters.getParameter(MetaMSEcorrelateParameters.SUFFIX).getValue();

    if (autoSuffix)
      suffix = MessageFormat.format("corr {2} r>={0} dp>={1}, {3}", minShapeCorrR,
          minCorrelatedDataPoints, shapeSimMeasure, searchAdducts ? "MS annot" : "");
    else
      suffix = parameters.getParameter(MetaMSEcorrelateParameters.SUFFIX).getEmbeddedParameter()
          .getValue();
  }



  public MetaMSEcorrelateTask() {}



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
      groupedPKL = copyPeakList(peakList, suffix);

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
      RowGroupList groups = corrMap.createCorrGroups(groupedPKL, minShapeCorrR, stageProgress);

      if (isCanceled())
        return;
      // refinement:
      // filter by avg correlation in group
      // delete single connections between sub networks
      if (groups != null) {
        // set groups to pkl
        groups.stream().map(g -> (CorrelationRowGroup) g)
            .forEach(g -> g.recalcGroupCorrelation(corrMap));
        groupedPKL.setGroups(groups);
        groups.setGroupsToAllRows();

        // do MSMS comparison of group
        double maxDiff = msmsChecks.getParameter(MSAnnMSMSCheckParameters.MZ_TOLERANCE).getValue()
            .getMzTolerance();
        maxDiff = Math.min(maxDiff, 0.0015);
        setStage(Stage.MS2_SIMILARITY);

        if (checkMS2Similarity)
          MS2SimilarityTask.checkGroupList(this, stageProgress, groups,
              msmsChecks.getParameter(MSAnnMSMSCheckParameters.MASS_LIST).getValue(), maxDiff, 3, 3,
              25);


        // annotation at groups stage
        if (searchAdducts && annotateOnlyCorrelated) {
          LOG.info("Corr: Annotation of groups only");
          setStage(Stage.ANNOTATION);
          AtomicInteger compared = new AtomicInteger(0);
          AtomicInteger annotPairs = new AtomicInteger(0);
          // for all groups
          groups.parallelStream().forEach(g -> {
            if (!this.isCanceled()) {
              annotateGroup(g, compared, annotPairs);
              stageProgress.addAndGet(1d / groups.size());
            }
          });

          if (isCanceled())
            return;

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
          if (isCanceled())
            return;

          // refinement
          if (performAnnotationRefinement) {
            LOG.info("Corr: Refine annotations");
            AnnotationRefinementTask ref =
                new AnnotationRefinementTask(project, refineParam, groupedPKL);
            ref.refine();
          }
          if (isCanceled())
            return;

          // recalc annotation networks
          MSAnnotationNetworkLogic.recalcAllAnnotationNetworks(nets, true);

          // show all annotations with the highest count of links
          LOG.info("Corr: show most likely annotations");
          MSAnnotationNetworkLogic.sortIonIdentities(groupedPKL, true);
        }

        if (isCanceled())
          return;
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

  private PeakList copyPeakList(PeakList peakList, String suffix) {
    SimplePeakList pkl = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());
    for (PeakListRow row : peakList.getRows()) {
      pkl.addRow(copyPeakRow(row));
    }
    return pkl;
  }

  /**
   * Create a copy of a peak list row.
   *
   * @param row the row to copy.
   * @return the newly created copy.
   */
  private static PeakListRow copyPeakRow(final PeakListRow row) {
    // Copy the peak list row.
    final PeakListRow newRow = new SimplePeakListRow(row.getID());
    PeakUtils.copyPeakListRowProperties(row, newRow);

    // Copy the peaks.
    for (final Feature peak : row.getPeaks()) {
      final Feature newPeak = new SimpleFeature(peak);
      PeakUtils.copyPeakProperties(peak, newPeak);
      newRow.addPeak(peak.getDataFile(), newPeak);
    }

    return newRow;
  }

  /**
   * Annotates all rows in a group
   * 
   * @param g
   * @param compared
   * @param annotPairs
   */
  private void annotateGroup(RowGroup g, AtomicInteger compared, AtomicInteger annotPairs) {
    for (int i = 0; i < g.size() - 1; i++) {
      // check against existing networks
      for (int k = i + 1; k < g.size(); k++) {
        compared.incrementAndGet();
        // check for adducts in library
        List<IonIdentity[]> id =
            library.findAdducts(peakList, g.get(i), g.get(k), adductCheckMode, minHeight);
        if (!id.isEmpty())
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
  private void doR2RComparison(PeakList peakList, R2RCorrMap map) throws Exception {
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
                R2RFullCorrelationData corr = corrR2R(raw, row, row2, minCorrelatedDataPoints,
                    minCorrDPOnFeatureEdge, minDPHeightCorr, minHeight, noiseLevelCorr,
                    useHeightCorrFilter, heightSimMeasure, minHeightCorr);
                if (corr != null) {
                  // deletes correlations if criteria is not met
                  corr.validate(minTotalShapeCorrR, useTotalShapeCorrFilter,
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
                  List<IonIdentity[]> id =
                      library.findAdducts(peakList, row, row2, adductCheckMode, minHeight);
                  if (!id.isEmpty())
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
   * @param useHeightCorrFilter
   * @return R2R correlation or null if invalid/no correlation
   */
  public static R2RFullCorrelationData corrR2R(RawDataFile[] raw, PeakListRow testRow,
      PeakListRow row, int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge,
      int minDPFHeightCorr, double minHeight, double noiseLevelShapeCorr,
      boolean useHeightCorrFilter, SimilarityMeasure heightSimilarity, double minHeightCorr)
      throws Exception {
    CorrelationData heightCorr =
        corrR2RFeatureHeight(raw, testRow, row, minHeight, noiseLevelShapeCorr, minDPFHeightCorr);

    // significance is alpha. 0 is perfect
    double maxSlopeSignificance = 0.3;
    double minFoldChange = 10;
    // stop if slope is negative or 0
    if (useHeightCorrFilter && heightCorr != null && filterNegativeRegression(heightCorr,
        minFoldChange, maxSlopeSignificance, minDPFHeightCorr, minHeightCorr, heightSimilarity))
      return null;
    else {
      Map<RawDataFile, CorrelationData> fCorr = corrR2RFeatureShapes(raw, testRow, row,
          minCorrelatedDataPoints, minCorrDPOnFeatureEdge, noiseLevelShapeCorr);

      if (fCorr != null && fCorr.isEmpty())
        fCorr = null;

      R2RFullCorrelationData rCorr = new R2RFullCorrelationData(testRow, row, heightCorr, fCorr);
      if (rCorr.isValid())
        return rCorr;
      else
        return null;
    }
  }

  /**
   * correlates the height profile of one row to another NO escape routine
   * 
   * @param raw
   * @param row
   * @param g
   * @return Correlation data of i profile of max i (or null if no correlation)
   */
  public static CorrelationData corrR2RFeatureHeight(final RawDataFile raw[], PeakListRow row,
      PeakListRow g, double minHeight, double noiseLevel, int minDPFHeightCorr) {
    // minimum of two
    minDPFHeightCorr = Math.min(minDPFHeightCorr, 2);

    List<double[]> data = new ArrayList<>();
    // calc ratio
    double ratio = 0;
    SimpleRegression reg = new SimpleRegression();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // I profile correlation
        double a = f1.getHeight();
        double b = f2.getHeight();
        if (a >= minHeight && b >= minHeight) {
          data.add(new double[] {a, b});
          ratio += a / b;
          reg.addData(a, b);
        }
      }
    }

    ratio = ratio / data.size();
    if (ratio != 0) {
      // estimate missing values as noise level if > minHeight
      for (int r = 0; r < raw.length; r++) {
        Feature f1 = row.getPeak(raw[r]);
        Feature f2 = g.getPeak(raw[r]);

        boolean amissing = (f1 == null || f1.getHeight() < minHeight);
        boolean bmissing = (f2 == null || f2.getHeight() < minHeight);
        // xor
        if (amissing ^ bmissing) {
          double a = amissing ? f2.getHeight() * ratio : f1.getHeight();
          double b = bmissing ? f1.getHeight() / ratio : f2.getHeight();

          // only if both are >= min height
          if (a >= minHeight && b >= minHeight) {
            if (amissing)
              a = Math.max(noiseLevel, f1 == null ? 0 : f1.getHeight());
            if (bmissing)
              b = Math.max(noiseLevel, f2 == null ? 0 : f2.getHeight());
            data.add(new double[] {a, b});
            reg.addData(a, b);
          }
        }
      }
    }

    // TODO weighting of intensity corr
    if (data.size() < 2)
      return null;
    else
      return CorrelationData.create(data);
  }

  /**
   * Only true if this should be filtered out. Need to have a minimum fold change to be significant.
   * 
   * @param data
   * @param reg
   * @param minFoldChange
   * @param maxSlopeSignificance
   * @return
   */
  private static boolean filterNegativeRegression(CorrelationData corr, double minFoldChange,
      double maxSlopeSignificance, int minDP, double minSimilarity,
      SimilarityMeasure heightSimilarity) {
    if (corr == null || (corr.getDPCount() < 3 || corr.getDPCount() < minDP))
      return false;

    double maxFC = Math.max(Similarity.maxFoldChange(corr.getData(), 0),
        Similarity.maxFoldChange(corr.getData(), 1));
    // do not use as filter if
    if (maxFC < minFoldChange)
      return false;

    double significantSlope = 0;
    try {
      significantSlope = corr.getReg().getSignificance();
    } catch (MathException e) {
      LOG.log(Level.SEVERE, "slope significance cannot be calculated", e);
    }
    // if slope is negative
    // slope significance is low (alpha is high)
    // similarity is low
    return (corr.getReg().getSlope() <= 0
        || (!Double.isNaN(significantSlope) && significantSlope > maxSlopeSignificance)
        || corr.getSimilarity(heightSimilarity) < minSimilarity);
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
  public static Map<RawDataFile, CorrelationData> corrR2RFeatureShapes(final RawDataFile raw[],
      PeakListRow row, PeakListRow g, int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge,
      double noiseLevelShapeCorr) throws Exception {
    HashMap<RawDataFile, CorrelationData> corrData = new HashMap<>();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // peak shape correlation
        CorrelationData data = corrFeatureShape(f1, f2, true, minCorrelatedDataPoints,
            minCorrDPOnFeatureEdge, noiseLevelShapeCorr);

        // if correlation is really bad return null
        if (filterNegativeRegression(data, 5, 0.2, 7, 0.5, SimilarityMeasure.PEARSON))
          return null;
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
      int minCorrelatedDataPoints, int minCorrDPOnFeatureEdge, double noiseLevelShapeCorr)
      throws Exception {
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

      // check min data points left from apex
      int left = data.size() - 1;
      if (left < minCorrDPOnFeatureEdge)
        return null;

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
      // check right and total dp
      int right = data.size() - 1 - left;
      // return pearson r
      if (data.size() >= minCorrelatedDataPoints && right >= minCorrDPOnFeatureEdge) {
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
      if (val >= noiseLevelCorr)
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
