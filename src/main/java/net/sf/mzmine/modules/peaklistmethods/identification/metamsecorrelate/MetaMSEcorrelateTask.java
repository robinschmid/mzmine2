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
import java.util.logging.Level;
import java.util.logging.Logger;
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
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroupList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.AnnotationNetwork;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationLibrary;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationParameters;
import net.sf.mzmine.modules.visualization.metamsecorrelate.corrnetwork.visual.CorrNetworkFrame;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
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
    CORRELATION_ANNOTATION, GROUPING, REFINEMENT;
  }

  private int finishedRows;
  private int totalRows;

  private final ParameterSet parameters;
  private final MZmineProject project;
  // GENERAL
  private final PeakList peakList;
  private final RTTolerance rtTolerance;

  // ADDUCTS
  private MSAnnotationLibrary library;
  private final boolean useAdductBonusR, searchAdducts;
  private final double adductBonusR;

  // GROUP and MIN SAMPLES FILTER
  private boolean useGroups;
  private final String groupingParameter;
  /**
   * Minimum percentage of samples (in group if useGroup) that have to contain a feature
   */
  private double percContainedInSamples;
  // sample group size
  private UserParameter<?, ?> sgroupPara;
  private HashMap<Object, Integer> sgroupSize;
  private boolean hasToFilterMinFInSampleSets;

  // FEATURE SHAPE CORRELATION
  // pearson correlation r to identify negative correlation
  private final double mainPeakIntensity;
  private final double minShapeCorrR;
  private static double noiseLevelShapeCorr = 1E4;
  private static int minCorrelatedDataPoints = 3;

  // MAX INTENSITY PROFILE CORRELATION ACROSS SAMPLES
  private double minMaxICorr;
  private int minDPMaxICorr;


  private Stage stage;

  // output
  private MSEGroupedPeakList groupedPKL;


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

    finishedRows = 0;
    totalRows = 0;

    // sample groups parameter
    useGroups = parameters.getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).getValue();
    groupingParameter = (String) parameters.getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER)
        .getEmbeddedParameter().getValue();
    // by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
    percContainedInSamples =
        parameterSet.getParameter(MetaMSEcorrelateParameters.MIN_SAMPLES).getValue();

    // tolerances
    rtTolerance = parameterSet.getParameter(MetaMSEcorrelateParameters.RT_TOLERANCE).getValue();

    // FEATURE SHAPE CORRELATION
    FeatureShapeCorrelationParameters corrp = (FeatureShapeCorrelationParameters) parameterSet
        .getParameter(MetaMSEcorrelateParameters.FSHAPE_CORRELATION).getEmbeddedParameters();
    // filter
    // start with high abundant features >= mainPeakIntensity
    // In this way we directly filter out groups with no abundant features
    // fill in smaller features after
    mainPeakIntensity =
        corrp.getParameter(FeatureShapeCorrelationParameters.MAIN_PEAK_HEIGHT).getValue();
    minShapeCorrR =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_R_SHAPE_INTRA).getValue();
    noiseLevelShapeCorr =
        corrp.getParameter(FeatureShapeCorrelationParameters.NOISE_LEVEL_PEAK_SHAPE).getValue();
    minCorrelatedDataPoints =
        corrp.getParameter(FeatureShapeCorrelationParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();

    // ADDUCTS
    useAdductBonusR =
        parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_BONUSR).getValue();
    adductBonusR = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_BONUSR)
        .getEmbeddedParameter().getValue();

    searchAdducts = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_LIBRARY).getValue();
    MSAnnotationParameters annParam = (MSAnnotationParameters) parameterSet
        .getParameter(MetaMSEcorrelateParameters.ADDUCT_LIBRARY).getEmbeddedParameters();
    library = new MSAnnotationLibrary(annParam);

    // intensity correlation across samples
    minMaxICorr = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleIntCorrParameters.MIN_CORRELATION)
        .getValue();
    minDPMaxICorr = parameterSet.getParameter(MetaMSEcorrelateParameters.IMAX_CORRELATION)
        .getEmbeddedParameters().getParameter(InterSampleIntCorrParameters.MIN_DP).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0
        : (1.d / Stage.values().length) * (stage.ordinal() + (finishedRows / (double) totalRows));
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
      setSampleGroups(groupedPKL);
      groupedPKL.setSampleGroupsParameter(sgroupPara);
      groupedPKL.setSampleGroups(sgroupSize);

      // create correlation map
      stage = Stage.CORRELATION_ANNOTATION;
      finishedRows = 0;
      R2RCorrMap corrMap = new R2RCorrMap();
      List<AnnotationNetwork> annNet = doR2RComparison(groupedPKL, corrMap);
      if (isCanceled())
        return;

      // show corr network
      CorrNetworkFrame f = new CorrNetworkFrame(groupedPKL, corrMap, 0.95);
      f.setVisible(true);

      LOG.info("Corr: Starting to group by correlation");
      stage = Stage.GROUPING;
      finishedRows = 0;
      PKLRowGroupList groups = corrMap.createCorrGroups(groupedPKL);

      if (isCanceled())
        return;
      // refinement:
      // filter by avg correlation in group
      // delete single connections between sub networks
      if (groups != null) {
        // set groups to pkl
        groupedPKL.setCorrelationMap(corrMap);
        groupedPKL.setGroups(groups);

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
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Correlation and adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      throw new MSDKRuntimeException(t);
    }
  }

  /**
   * Correlation and adduct network creation
   * 
   * @param peakList
   * @return
   */
  private List<AnnotationNetwork> doR2RComparison(MSEGroupedPeakList peakList, R2RCorrMap map)
      throws Exception {
    LOG.info("Corr: Creating row2row correlation map");

    PeakListRow rows[] = peakList.getRows();
    totalRows = rows.length;
    final RawDataFile raw[] = peakList.getRawDataFiles();

    // sort by avgRT
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));

    // for all rows
    int annotPairs = 0;
    int compared = 0;
    for (int i = 0; i < totalRows - 1; i++) {
      PeakListRow row = rows[i];

      // is present in min % of samples
      if (filterMinFeaturesInSampleSet(raw, row)) {
        for (int x = i + 1; x < totalRows; x++) {
          if (isCanceled())
            return null;

          PeakListRow row2 = rows[x];
          // is present in min % of samples
          if (filterMinFeaturesInSampleSet(raw, row2)) {

            // correlate if in rt range
            if (rtTolerance.checkWithinTolerance(row.getAverageRT(), row2.getAverageRT())) {
              R2RCorrelationData corr =
                  corrR2R(raw, row, row2, minDPMaxICorr, minCorrelatedDataPoints);
              if (corr != null) {
                // deletes correlations if criteria is not met
                corr.validate(minMaxICorr, minShapeCorrR);
                // still valid?
                if (corr.isValid())
                  map.add(row, row2, corr);
              }

              if (searchAdducts) {
                // check for adducts in library
                ESIAdductType[] id = library.findAdducts(peakList, row, row2);
                compared++;
                if (id != null)
                  annotPairs++;
              }
            }
          }
        }
      }
      finishedRows = i;
    }

    // number of f2f correlations
    int nF2F = 0;
    for (R2RCorrelationData r2r : map.values()) {
      if (r2r.hasFeatureShapeCorrelation())
        nF2F += r2r.getCorrPeakShape().size();
    }

    LOG.info(MessageFormat.format(
        "Corr: Correlations done with {0} R2R correlations and {1} F2F correlations", map.size(),
        nF2F));

    if (searchAdducts) {
      LOG.info("Corr: A total of " + compared + " row2row comparisons with " + annotPairs
          + " annotation pairs");

      // show all annotations with the highest count of links
      LOG.info("Corr: show most likely annotations");
      MSAnnotationNetworkLogic.showMostlikelyAnnotations(groupedPKL);

      //
      LOG.info("Corr: create annotation network numbers");
      return MSAnnotationNetworkLogic.createAnnotationNetworks(groupedPKL, true);
    }
    return null;
  }

  /**
   * Correlate all f2f and create row 2 row correlation data
   * 
   * @param raw
   * @param testRow
   * @param row
   * @return R2R correlation or null if invalid/no correlation
   */
  public static R2RCorrelationData corrR2R(RawDataFile[] raw, PeakListRow testRow, PeakListRow row,
      int minDPMaxICorr, int minCorrelatedDataPoints) throws Exception {
    CorrelationData iProfileR = corrRowToRowIProfile(raw, testRow, row, minDPMaxICorr);
    Map<RawDataFile, CorrelationData> fCorr =
        corrRowToRowFeatureShape(raw, testRow, row, minCorrelatedDataPoints);

    if (fCorr.isEmpty())
      fCorr = null;

    R2RCorrelationData rCorr = new R2RCorrelationData(testRow, row, iProfileR, fCorr);
    if (rCorr.isValid())
      return rCorr;
    else
      return null;
  }


  /**
   * gets called to initialise variables for next peaklist
   * 
   * @param peakList
   */
  private void setSampleGroups(PeakList peakList) {
    if (groupingParameter == null || groupingParameter.length() == 0) {
      this.sgroupSize = null;
      hasToFilterMinFInSampleSets = false;
      return;
    } else {
      HashMap<Object, Integer> sgroupSize = new HashMap<Object, Integer>();
      UserParameter<?, ?> params[] = project.getParameters();
      for (UserParameter<?, ?> p : params) {
        if (groupingParameter.equals(p.getName())) {
          // save parameter for sample groups
          sgroupPara = p;
          break;
        }
      }
      int max = 0;
      // calc size of sample groups
      for (RawDataFile file : peakList.getRawDataFiles()) {
        String parameterValue = sgroupOf(file);

        Integer v = sgroupSize.get(parameterValue);
        int val = v == null ? 0 : v;
        sgroupSize.put(parameterValue, val + 1);
        if (val + 1 > max)
          max = val + 1;
      }
      this.sgroupSize = sgroupSize;
      // has to filter minimum samples with a feature in a set?
      // only if sample set has to contain more than one sample with a feature
      hasToFilterMinFInSampleSets = ((int) (max * percContainedInSamples)) > 1;
    }
  }

  /**
   * 
   * @param file
   * @return sample group value of raw file
   */
  private String sgroupOf(RawDataFile file) {
    return String.valueOf(project.getParameterValue(sgroupPara, file));
  }

  /**
   * only keep rows which contain features in at least X % samples in a set called before starting
   * row processing
   * 
   * @param raw
   * @param row
   * @return
   */
  private boolean filterMinFeaturesInSampleSet(final RawDataFile raw[], PeakListRow row) {
    // short cut if minimum is only one sample in a set
    if (!hasToFilterMinFInSampleSets || sgroupSize == null)
      return true;
    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<Object, Integer> counter = new HashMap<Object, Integer>();
    for (RawDataFile file : raw) {
      if (row.hasPeak(file)) {
        String sgroup = sgroupOf(file);
        if (counter.containsKey(sgroup)) {
          counter.put(sgroup, counter.get(sgroup) + 1);
        } else {
          counter.put(sgroup, 1);
        }
      }
    }
    // only go on if feature was present in X % of the samples
    for (Object sg : counter.keySet()) {
      if (counter.get(sg) < sgroupSize.get(sg) * percContainedInSamples)
        return false;
    }
    return true;
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
      PeakListRow row, PeakListRow g, int minCorrelatedDataPoints) throws Exception {
    HashMap<RawDataFile, CorrelationData> corrData = new HashMap<>();
    // go through all raw files
    for (int r = 0; r < raw.length; r++) {
      Feature f1 = row.getPeak(raw[r]);
      Feature f2 = g.getPeak(raw[r]);
      if (f1 != null && f2 != null) {
        // peak shape correlation
        CorrelationData data = corrFeatureShape(f1, f2, true);
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
  public static CorrelationData corrFeatureShape(Feature f1, Feature f2, boolean sameRawFile)
      throws Exception {
    // Range<Double> rt1 = f1.getRawDataPointsRTRange();
    // Range<Double> rt2 = f2.getRawDataPointsRTRange();
    if (sameRawFile) {
      // scan numbers (not necessary 1,2,3...)
      int[] sn1 = f1.getScanNumbers();
      int[] sn2 = f2.getScanNumbers();
      int offsetI1 = 0;
      int offsetI2 = 0;
      // find corresponding value
      if (sn2[0] > sn1[0]) {
        for (int i = 1; i < sn1.length; i++) {
          if (sn1[i] == sn2[0]) {
            offsetI1 = i;
            break;
          }
        }
        // peaks are not overlapping
        if (offsetI1 == 0)
          return null;
      }
      if (sn2[0] < sn1[0]) {
        for (int i = 1; i < sn2.length; i++) {
          if (sn1[0] == sn2[i]) {
            offsetI2 = i;
            break;
          }
        }
        // peaks are not overlapping
        if (offsetI2 == 0)
          return null;
      }
      // only correlate intercepting areas 0-max
      int max = 0;
      if (sn1.length - offsetI1 <= sn2.length - offsetI2)
        max = sn1.length - offsetI1;
      if (sn1.length - offsetI1 > sn2.length - offsetI2)
        max = sn2.length - offsetI2;
      if (max - offsetI1 > minCorrelatedDataPoints && max - offsetI2 > minCorrelatedDataPoints) {
        RawDataFile raw = f1.getDataFile();

        // save max and min of intensity of val1(x)
        List<double[]> data = new ArrayList<double[]>();
        // add all data points over a given threshold
        // raw data (not smoothed)
        for (int i = 0; i < max; i++) {
          if (sn1[i + offsetI1] != sn2[i + offsetI2])
            throw new Exception("Scans are not the same for peak shape corr");

          DataPoint dp = f1.getDataPoint(sn1[i + offsetI1]);
          DataPoint dp2 = f2.getDataPoint(sn2[i + offsetI2]);
          if (dp != null && dp2 != null) {
            // raw data
            double val1 = dp.getIntensity();
            double val2 = dp2.getIntensity();

            if (val1 >= noiseLevelShapeCorr && val2 >= noiseLevelShapeCorr) {
              data.add(new double[] {val1, val2});
            }
          }
        }
        // return pearson r
        if (data.size() >= minCorrelatedDataPoints) {
          return CorrelationData.create(data);
        }
      }
    } else {
      // TODO if different raw file search for same rt
      // impute rt/I values if between 2 data points
    }
    return null;
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
