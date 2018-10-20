/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.math3.special.Erf;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2GroupCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class SiriusExportTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  // based on netID
  public static String COMPOUND_ID = "COMPOUND_ID=";
  // based on feature shape correlation (metaMSEcorr)
  public static String CORR_GROUPID = "CORR_GROUPID=";

  // neutral mass
  public static String COMPOUND_MASS = "COMPOUND_MASS=";


  private final static String plNamePattern = "{}";
  protected static final Comparator<DataPoint> CompareDataPointsByMz = new Comparator<DataPoint>() {
    @Override
    public int compare(DataPoint o1, DataPoint o2) {
      return Double.compare(o1.getMZ(), o2.getMZ());
    }
  };
  private final PeakList[] peakLists;
  private final File fileName;
  // private final boolean fractionalMZ;
  private final String massListName;
  protected double progress, totalProgress;
  protected final SiriusExportParameters.MERGE_MODE mergeMsMs;

  // by robin
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  // seconds
  private NumberFormat rtsForm = new DecimalFormat("0.###");
  // correlation
  private NumberFormat corrForm = new DecimalFormat("0.0000");

  private boolean excludeInsourceFrag;
  private boolean needAnnotation;
  private boolean excludeMultimers;
  private boolean excludeMultiCharge;

  /**
   * Experimental: Export correlated MS1 only once per MS annotation network and link to all MS2 Use
   * NetID
   */
  private boolean exportCorrMSOnce;


  @Override
  public double getFinishedPercentage() {
    return (totalProgress == 0 ? 0 : progress / totalProgress);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting peak list(s) " + Arrays.toString(peakLists) + " to MGF file(s)";
  }

  SiriusExportTask(ParameterSet parameters) {
    this.peakLists = parameters.getParameter(SiriusExportParameters.PEAK_LISTS).getValue()
        .getMatchingPeakLists();

    this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();

    // this.fractionalMZ =
    // parameters.getParameter(SiriusExportParameters.FRACTIONAL_MZ)
    // .getValue();

    this.massListName = parameters.getParameter(SiriusExportParameters.MASS_LIST).getValue();
    this.mergeMsMs = parameters.getParameter(SiriusExportParameters.MERGE).getValue();
    // new parameters related to MS annotate and metaMSEcorrelate
    excludeInsourceFrag =
        parameters.getParameter(SiriusExportParameters.EXCLUDE_INSOURCE_FRAGMENTS).getValue();
    excludeMultiCharge =
        parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTICHARGE).getValue();
    excludeMultimers = parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTIMERS).getValue();
    needAnnotation = parameters.getParameter(SiriusExportParameters.NEED_ANNOTATION).getValue();
    // experimental
    exportCorrMSOnce =
        parameters.getParameter(SiriusExportParameters.EXPORT_CORRMS1_ONLY_ONCE).getValue();
  }

  @Override
  public void run() {
    this.progress = 0d;
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    int counter = 0;
    for (PeakList l : peakLists)
      counter += l.getNumberOfRows();
    this.totalProgress = counter;

    // Process peak lists
    for (PeakList peakList : peakLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Open file
      try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile, true))) {
        exportPeakList(peakList, bw);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
      }

      // If peak list substitution pattern wasn't found,
      // treat one peak list only
      if (!substitute)
        break;
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private static DataPoint[] merge(double parentPeak, List<DataPoint[]> scans) {
    final DataPointSorter sorter =
        new DataPointSorter(SortingProperty.Intensity, SortingDirection.Descending);
    double maxTIC = 0d;
    int best = 0;
    for (int i = 0; i < scans.size(); ++i) {
      final DataPoint[] scan = scans.get(i);
      Arrays.sort(scan, sorter);
      double tic = 0d;
      for (int j = 0; j < Math.min(40, scan.length); ++j) {
        tic += scan[j].getIntensity();
      }
      if (tic > maxTIC) {
        maxTIC = tic;
        best = i;
      }
      DataPoint[] m = scans.get(0);
      scans.set(0, scans.get(best));
      scans.set(best, m);
    }
    final DataPoint[] mergedSpectrum = scans.get(0).clone();
    Arrays.sort(mergedSpectrum, CompareDataPointsByMz);
    for (int i = 1; i < scans.size(); ++i) {
      merge(mergedSpectrum, scans.get(i));
    }
    // remove noise
    if (mergedSpectrum.length > 60) {
      double lowestIntensity = Double.POSITIVE_INFINITY,
          secondLowestIntensity = Double.POSITIVE_INFINITY;
      for (int i = 0; i < mergedSpectrum.length; ++i) {
        final double z = mergedSpectrum[i].getIntensity();
        if (z < secondLowestIntensity) {
          if (z < lowestIntensity) {
            secondLowestIntensity = lowestIntensity;
            lowestIntensity = z;
          } else
            secondLowestIntensity = z;
        }
      }
      double baseline = lowestIntensity + secondLowestIntensity;
      int behindParent = Arrays.binarySearch(mergedSpectrum,
          new SimpleDataPoint(parentPeak + 5, 0d), CompareDataPointsByMz);
      if (behindParent < 0) {
        behindParent = -(behindParent + 1);
      }
      final int noisePeaksBehindParentPeak = mergedSpectrum.length - behindParent;
      if (noisePeaksBehindParentPeak >= 10) {
        final DataPoint[] subspec = new DataPoint[noisePeaksBehindParentPeak];
        System.arraycopy(mergedSpectrum, noisePeaksBehindParentPeak, subspec, 0, subspec.length);
        Arrays.sort(subspec, sorter);
        int q75 = (int) (subspec.length * 0.75);
        baseline = Math.max(subspec[q75].getIntensity(), baseline);
      }
      final List<DataPoint> keep = new ArrayList<>();
      for (int i = 0; i < mergedSpectrum.length; ++i) {
        if (mergedSpectrum[i].getIntensity() > baseline)
          keep.add(mergedSpectrum[i]);
      }
      return keep.toArray(new DataPoint[keep.size()]);
    }
    return mergedSpectrum;
  }

  private static void merge(DataPoint[] orderedByMz, DataPoint[] orderedByInt) {
    // we assume a rather large deviation as signal peaks should be contained in more than one
    // measurement
    final List<DataPoint> append = new ArrayList<>();
    final double absoluteDeviation = 0.005;
    for (int k = 0; k < orderedByInt.length; ++k) {
      final DataPoint peak = orderedByInt[k];
      final double dev = Math.max(absoluteDeviation, peak.getMZ() * 10e-6);
      final double lb = peak.getMZ() - dev, ub = peak.getMZ() + dev;
      int mz1 = Arrays.binarySearch(orderedByMz, peak, CompareDataPointsByMz);
      if (mz1 < 0) {
        mz1 = -(mz1 + 1);
      }
      int mz0 = mz1 - 1;
      while (mz1 < orderedByMz.length && orderedByMz[mz1].getMZ() <= ub)
        ++mz1;
      --mz1;
      while (mz0 >= 0 && orderedByMz[mz0].getMZ() >= lb)
        --mz0;
      ++mz0;
      if (mz0 <= mz1) {
        // merge!
        int mostIntensive = mz0;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = mz0; i <= mz1; ++i) {
          final double massDiff = orderedByMz[i].getMZ() - peak.getMZ();
          final double score =
              Erf.erfc(3 * massDiff) / (dev * Math.sqrt(2)) * orderedByMz[i].getIntensity();
          if (score > bestScore) {
            bestScore = score;
            mostIntensive = i;
          }
        }
        final double mzValue =
            peak.getIntensity() > orderedByMz[mostIntensive].getIntensity() ? peak.getMZ()
                : orderedByMz[mostIntensive].getMZ();
        orderedByMz[mostIntensive] = new SimpleDataPoint(mzValue,
            peak.getIntensity() + orderedByMz[mostIntensive].getIntensity());
      } else {
        // append
        append.add(peak);
      }
    }
    if (append.size() > 0) {
      int offset = orderedByMz.length;
      orderedByMz = Arrays.copyOf(orderedByMz, orderedByMz.length + append.size());
      for (DataPoint p : append) {
        orderedByMz[offset++] = p;
      }
      Arrays.sort(orderedByMz, CompareDataPointsByMz);
    }
  }

  public void runSingleRow(PeakList peakList, PeakListRow row) {
    this.progress = 0d;
    setStatus(TaskStatus.PROCESSING);
    try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
      exportPeakListRow(peakList, row, bw, getFragmentScans(row.getRawDataFiles()));
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + fileName + " for writing.");
    }
    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private void exportPeakList(PeakList peakList, BufferedWriter writer) throws IOException {
    // Raw data file, scan numbers
    final HashMap<String, int[]> fragmentScans = getFragmentScans(peakList.getRawDataFiles());

    int exported = 0;
    for (PeakListRow row : peakList.getRows()) {
      boolean fitCharge = !excludeMultiCharge || row.getRowCharge() <= 1;
      ESIAdductIdentity adduct = MSAnnotationNetworkLogic.getMostLikelyAnnotation(row, true);
      boolean fitAnnotation = !needAnnotation || adduct != null;
      boolean fitMol = !excludeMultimers || adduct == null || adduct.getA().getMolecules() <= 1;
      boolean fitFragments = !excludeInsourceFrag || adduct == null || !adduct.getA().isFragment();
      if (fitAnnotation && fitCharge && fitMol && fitFragments) {
        if (exportPeakListRow(peakList, row, writer, fragmentScans))
          exported++;
      }
    }

    logger.info(
        MessageFormat.format("exported {0} rows for peaklist {1}", exported, peakList.getName()));
  }

  private HashMap<String, int[]> getFragmentScans(RawDataFile[] rawDataFiles) {
    final HashMap<String, int[]> fragmentScans = new HashMap<>();
    for (RawDataFile r : rawDataFiles) {
      int[] scans = new int[0];
      for (int msLevel : r.getMSLevels()) {
        if (msLevel > 1) {
          int[] concat = r.getScanNumbers(msLevel);
          int offset = scans.length;
          scans = Arrays.copyOf(scans, scans.length + concat.length);
          System.arraycopy(concat, 0, scans, offset, concat.length);
        }
      }
      Arrays.sort(scans);
      fragmentScans.put(r.getName(), scans);
    }
    return fragmentScans;
  }

  private boolean exportPeakListRow(PeakList peakList, PeakListRow row, BufferedWriter writer,
      final HashMap<String, int[]> fragmentScans) throws IOException {
    if (isSkipRow(row))
      return false;
    // get row charge and polarity
    char polarity = 0;
    for (Feature f : row.getPeaks()) {
      char pol = f.getDataFile().getScan(f.getRepresentativeScanNumber()).getPolarity()
          .asSingleChar().charAt(0);
      if (pol != polarity && polarity != 0) {
        setErrorMessage(
            "Joined features have different polarity. This is most likely a bug. If not, please separate them as individual features and/or write a feature request on github.");
        setStatus(TaskStatus.ERROR);
        return false;
      } else {
        polarity = pol;
      }
    }
    // find ion species by annotation (can be null)
    String ion = findIonAnnotation(peakList, row);
    // write correlation spectrum
    writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.CORRELATED, -1, ion);
    writeCorrelationSpectrum(writer, peakList, row);

    List<DataPoint[]> toMerge = new ArrayList<>();
    List<String> sources = new ArrayList<>();

    // for each MS/MS write corresponding MS1 and MSMS spectrum
    for (Feature f : row.getPeaks()) {
      if (mergeMsMs == SiriusExportParameters.MERGE_MODE.MERGE_CONSECUTIVE_SCANS)
        toMerge.clear();
      if (f.getFeatureStatus() == Feature.FeatureStatus.DETECTED
          && f.getMostIntenseFragmentScanNumber() >= 0) {
        final int[] scanNumbers = f.getScanNumbers().clone();
        Arrays.sort(scanNumbers);
        int[] fs = fragmentScans.get(f.getDataFile().getName());
        int startWith = scanNumbers[0];
        int j = Arrays.binarySearch(fs, startWith);
        if (j < 0)
          j = (-j - 1);
        for (int k = j; k < fs.length; ++k) {
          final Scan scan = f.getDataFile().getScan(fs[k]);
          if (scan.getMSLevel() > 1 && Math.abs(scan.getPrecursorMZ() - f.getMZ()) < 0.1) {
            // TODO check if to include MS1 precursor scan
            /*
             * if (includeMs1) { // find precursor scan int prec = Arrays.binarySearch(scanNumbers,
             * fs[k]); if (prec < 0) prec = -prec - 1; prec = Math.max(0, prec - 1); for (; prec <
             * scanNumbers.length && scanNumbers[prec] < fs[k]; ++prec) { final Scan precursorScan =
             * f.getDataFile().getScan(scanNumbers[prec]); if (precursorScan.getMSLevel() == 1) {
             * writeHeader(writer, row, f.getDataFile(), polarity, MsType.MS,
             * precursorScan.getScanNumber()); writeSpectrum(writer, massListName != null ?
             * precursorScan.getMassList(massListName).getDataPoints() :
             * precursorScan.getDataPoints()); } } }
             */ // Do not include MS1 scans (except for isotope pattern)
            if (mergeMsMs == SiriusExportParameters.MERGE_MODE.NO_MERGE) {
              writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, scan.getScanNumber(),
                  ion);
              writeSpectrum(writer,
                  massListName != null ? scan.getMassList(massListName).getDataPoints()
                      : scan.getDataPoints());
            } else {
              if (mergeMsMs == SiriusExportParameters.MERGE_MODE.MERGE_OVER_SAMPLES)
                sources.add(f.getDataFile().getName());
              toMerge.add(massListName != null ? scan.getMassList(massListName).getDataPoints()
                  : scan.getDataPoints());
            }
          }
        }
        if (mergeMsMs == SiriusExportParameters.MERGE_MODE.MERGE_CONSECUTIVE_SCANS
            && toMerge.size() > 0) {
          writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, null, ion);
          writeSpectrum(writer, merge(f.getMZ(), toMerge));
        }
      }
      ++progress;
    }
    if (mergeMsMs == SiriusExportParameters.MERGE_MODE.MERGE_OVER_SAMPLES && toMerge.size() > 0) {
      writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.MSMS, null,
          sources, ion);
      writeSpectrum(writer, merge(row.getAverageMZ(), toMerge));
    }
    // exported
    return true;
  }

  /**
   * Ion or null
   * 
   * @param peakList
   * @param row
   * @return
   */
  private String findIonAnnotation(PeakList peakList, PeakListRow row) {
    String ion = null;
    // find by MS annotation
    PKLRowGroup g = null;
    if (peakList instanceof MSEGroupedPeakList)
      g = ((MSEGroupedPeakList) peakList).getGroup(row);
    ESIAdductIdentity id = MSAnnotationNetworkLogic.getMostLikelyAnnotation(row, g);
    if (id != null)
      ion = id.getAdduct();

    return ion;
  }

  private boolean isSkipRow(PeakListRow row) {
    // skip rows which have no isotope pattern and no MS/MS spectrum
    for (Feature f : row.getPeaks()) {
      if (f.getFeatureStatus() == Feature.FeatureStatus.DETECTED) {
        if ((f.getIsotopePattern() != null && f.getIsotopePattern().getDataPoints().length > 1)
            || f.getMostIntenseFragmentScanNumber() >= 0)
          return false;
      }
    }
    return true;
  }

  private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
      MsType msType, Integer scanNumber, String ion) throws IOException {
    writeHeader(writer, row, raw, polarity, msType, scanNumber, null, ion);
  }

  private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
      MsType msType, Integer scanNumber, List<String> sources, String ion) throws IOException {
    final Feature feature = row.getPeak(raw);
    writer.write("BEGIN IONS");
    writer.newLine();
    writer.write("FEATURE_ID=");
    writer.write(String.valueOf(row.getID()));
    writer.newLine();
    writer.write("PEPMASS=");
    writer.write(mzForm.format(row.getBestPeak().getMZ()));
    writer.newLine();

    if (ion != null && !ion.isEmpty()) {
      writer.write("ION=");
      writer.write(ion);
      writer.newLine();
    }
    writer.write("CHARGE=");
    writer.write(String.valueOf(Math.abs(row.getRowCharge())));
    writer.write(polarity);
    writer.newLine();
    writer.write("RTINSECONDS=");
    writer.write(rtsForm.format(feature.getRT() * 60d));
    writer.newLine();
    switch (msType) {
      case CORRELATED:
        writer.write("SPECTYPE=CORRELATED MS");
        writer.newLine();
      case MS:
        writer.write("MSLEVEL=1");
        writer.newLine();
        break;
      case MSMS:
        writer.write("MSLEVEL=2");
        writer.newLine();
    }
    writer.write("FILENAME=");
    if (sources != null) {
      writer.write(escape(sources.get(0), ";"));
      for (int i = 1; i < sources.size(); ++i) {
        writer.write(";");
        writer.write(escape(sources.get(i), ";"));
      }
      writer.newLine();
    } else if (msType == MsType.CORRELATED) {
      RawDataFile[] raws = row.getRawDataFiles();
      writer.write(escape(raws[0].getName(), ";"));
      for (int i = 1; i < raws.length; ++i) {
        writer.write(";");
        writer.write(escape(raws[i].getName(), ";"));
      }
      writer.newLine();
    } else {
      writer.write(feature.getDataFile().getName());
      writer.newLine();
    }
    if (scanNumber != null) {
      writer.write("SCANS=");
      writer.write(String.valueOf(scanNumber));
      writer.newLine();
    }
  }

  /**
   * Write all correlated features (adducts, in-source fragments, istotopes)
   * 
   * @param writer
   * @param feature
   * @throws IOException
   */
  private void writeCorrelationSpectrum(BufferedWriter writer, PeakList pkl, PeakListRow row)
      throws IOException {
    /*
     * Grouped by metaMSEcorrelate Annotations by MS annotations in module
     */
    boolean exported = false;
    if (pkl != null && pkl instanceof MSEGroupedPeakList) {
      MSEGroupedPeakList peakList = (MSEGroupedPeakList) pkl;
      // get all rows in corr group
      PKLRowGroup g = peakList.getGroup(row);
      if (g != null && g.size() > 1) {
        writeCorrelationSpectrum(writer, g);
        exported = true;
      }
    }
    // export isotopes if failed
    if (!exported) {
      Feature feature = row.getBestPeak();
      if (feature.getIsotopePattern() != null) {
        writeSpectrum(writer, feature.getIsotopePattern().getDataPoints());
      } else {
        // write nothing
        writer.write(mzForm.format(feature.getMZ()));
        writer.write(' ');
        writer.write("100.0");
        writer.newLine();
        writer.write("END IONS");
        writer.newLine();
        writer.newLine();
      }
    }
  }

  /**
   * Export all grouped features and their isotope pattern. Export correlation and annotation
   * 
   * @param writer
   * @param g
   * @param map
   * @throws IOException
   */
  private void writeCorrelationSpectrum(BufferedWriter writer, PKLRowGroup g) throws IOException {
    for (int i = 0; i < g.size(); ++i) {
      PeakListRow row = g.get(i);
      R2GroupCorrelationData corr = g.getCorr(i);
      double r = corr.getAvgPeakShapeR();
      Feature best = row.getBestPeak();
      ESIAdductIdentity id = MSAnnotationNetworkLogic.getMostLikelyAnnotation(row, g);

      // TODO : best feature mz or avg?
      // intensity?
      writer.write(mzForm.format(row.getAverageMZ()));
      writer.write(' ');
      writer.write(intensityForm.format(best.getHeight()));
      writer.write(' ');
      writer.write(corrForm.format(r));
      if (id != null) {
        writer.write(' ');
        writer.write(id.getAdduct());
        // TODO added by Robin
        // need to check if this is conform to the standards
        writer.write(' ');
        writer.write("by=" + id.getPartnerRowsID().length);
      }
      writer.newLine();
      // export isotope pattern
      writeCorrelationIsotopes(writer, row);
    }
    writer.write("END IONS");
    writer.newLine();
    writer.newLine();
  }

  /**
   * Write detected isotope pattern
   * 
   * @param writer
   * @param row
   * @throws IOException
   */
  private void writeCorrelationIsotopes(BufferedWriter writer, PeakListRow row) throws IOException {
    IsotopePattern pattern = row.getBestIsotopePattern();
    if (pattern != null) {
      double mz0 = 0;
      for (DataPoint dp : pattern.getDataPoints()) {
        if (mz0 == 0) {
          // do not export first ( it is already)
          mz0 = dp.getMZ();
        } else {
          // TODO : best feature mz or avg?
          // intensity?
          writer.write(mzForm.format(dp.getMZ()));
          writer.write(' ');
          writer.write(intensityForm.format(dp.getIntensity()));

          // isotope
          writer.write(' ');
          writer.write(' '); // skip correlation
          writer.write(' ');
          writer.write("+" + Math.round((float) (dp.getMZ() - mz0)));
          writer.newLine();
        }
      }
    }
  }

  private void writeSpectrum(BufferedWriter writer, DataPoint[] dps) throws IOException {
    for (DataPoint dp : dps) {
      writer.write(mzForm.format(dp.getMZ()));
      writer.write(' ');
      writer.write(intensityForm.format(dp.getIntensity()));
      writer.newLine();

    }
    writer.write("END IONS");
    writer.newLine();
    writer.newLine();
  }

  private String escape(String name, String s) {
    return name.replaceAll(s, "\\" + s);
  }

  private static enum MsType {
    MS, MSMS, CORRELATED
  }



}
