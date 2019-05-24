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

import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.CorrelationRowGroup;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2GroupCorrelationData;
import net.sf.mzmine.modules.tools.msmsspectramerge.MergeMode;
import net.sf.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import net.sf.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import net.sf.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import org.apache.commons.math3.special.Erf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SiriusExportTask extends AbstractTask {
    private Logger logger = Logger.getLogger(this.getClass().getName());

    // based on netID
    public static String COMPOUND_ID = "COMPOUND_ID=";
    // based on feature shape correlation (metaMSEcorr)
    public static String CORR_GROUPID = "CORR_GROUPID=";

    // neutral mass
    public static String COMPOUND_MASS = "COMPOUND_MASS=";
    // ION
    public static String ION = "ION=";

    // next id for renumbering
    private int nextID = 1;
    private boolean renumberID;


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

    private final MsMsSpectraMergeModule mergeMethod;
    private final MsMsSpectraMergeParameters mergeParameters;

    /**
     * Experimental: Export correlated MS1 only once per MS annotation network and link to all MS2 Use
     * NetID
     */
    private boolean exportCorrMSOnce;

    // to exclude duplicates in correlated spectrum
    private MZTolerance mzTol;


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
        this.mzTol = parameters.getParameter(SiriusExportParameters.MZ_TOL).getValue();

        // this.fractionalMZ =
        // parameters.getParameter(SiriusExportParameters.FRACTIONAL_MZ)
        // .getValue();

        this.massListName = parameters.getParameter(SiriusExportParameters.MASS_LIST).getValue();
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
        renumberID = parameters.getParameter(SiriusExportParameters.RENUMBER_ID).getValue();

        OptionalModuleParameter<MsMsSpectraMergeParameters> parameter = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER);
        mergeParameters = parameter.getValue().booleanValue() ? parameter.getEmbeddedParameters() : null;
        mergeMethod = mergeParameters == null ? null : new MsMsSpectraMergeModule(mergeParameters);
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
            try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile, false))) {
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
                System.arraycopy(mergedSpectrum, behindParent, subspec, 0, subspec.length);
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

    public void runSingleRows(PeakList peakList, PeakListRow[] rows) {
        this.progress = 0d;
        setStatus(TaskStatus.PROCESSING);
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(fileName, true))) {
            for (PeakListRow row : rows)
                exportPeakListRow(row, bw, getFragmentScans(row.getRawDataFiles()));
        } catch (IOException e) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage("Could not open file " + fileName + " for writing.");
        }
        if (getStatus() == TaskStatus.PROCESSING)
            setStatus(TaskStatus.FINISHED);
    }

    /**
     * @param peakList
     * @param writer
     * @return number of exported rows
     * @throws IOException
     */
    private int exportPeakList(PeakList peakList, BufferedWriter writer) throws IOException {
        // Raw data file, scan numbers
        final HashMap<String, int[]> fragmentScans = getFragmentScans(peakList.getRawDataFiles());

        int exported = 0;
        for (PeakListRow row : peakList.getRows()) {
            boolean fitCharge = !excludeMultiCharge || row.getRowCharge() <= 1;
            IonIdentity adduct = row.getBestIonIdentity();
            boolean fitAnnotation = !needAnnotation || adduct != null;
            boolean fitMol =
                    !excludeMultimers || adduct == null || adduct.getIonType().getMolecules() <= 1;
            boolean fitFragments =
                    !excludeInsourceFrag || adduct == null || !adduct.getIonType().hasMods();
            if (fitAnnotation && fitCharge && fitMol && fitFragments) {
                if (exportPeakListRow(row, writer, fragmentScans))
                    exported++;
            }
            progress++;
        }

        logger.info(
                MessageFormat.format("exported {0} rows for peaklist {1}", exported, peakList.getName()));
        return exported;
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

    private boolean exportPeakListRow(PeakListRow row, BufferedWriter writer,
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
        // MS annotation and feature correlation group
        // can be null (both)
        // run MS annotations module or better metaMSEcorrelate
        String msAnnotationsFlags = createMSAnnotationFlags(row, mzForm);

        MergeMode mergeMode = mergeParameters == null ? null : mergeParameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
        if ((mergeMode != MergeMode.ACROSS_SAMPLES)) {
            for (Feature f : row.getPeaks()) {
                if (f.getFeatureStatus() == Feature.FeatureStatus.DETECTED && f.getMostIntenseFragmentScanNumber() >= 0) {
                    // write correlation spectrum
                    writeHeader(writer, row, f.getDataFile(), polarity, MsType.CORRELATED, -1, msAnnotationsFlags);
                    writeCorrelationSpectrum(writer, row);
                    if (mergeMode == MergeMode.CONSECUTIVE_SCANS) {
                        // merge MS/MS
                        List<MergedSpectrum> spectra = new MsMsSpectraMergeModule(mergeParameters).mergeConsecutiveScans(f, massListName);
                        for (MergedSpectrum spectrum : spectra) {
                            writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, spectrum.filterByRelativeNumberOfScans(mergeParameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue()), msAnnotationsFlags);
                            writeSpectrum(writer, spectrum.data);
                        }
                    } else if (mergeMode == MergeMode.SAME_SAMPLE) {
                        MergedSpectrum spectrum = mergeMethod.mergeFromSameSample(f, massListName).filterByRelativeNumberOfScans(mergeParameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue());
                        if (spectrum.data.length > 0) {
                            writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, spectrum, msAnnotationsFlags);
                            writeSpectrum(writer, spectrum.data);
                        }
                    } else {
                        final Scan s = f.getDataFile().getScan(f.getMostIntenseFragmentScanNumber());
                        final DataPoint[] dps = s.getMassList(massListName).getDataPoints();
                        if (dps.length > 0) {
                            writeHeader(writer, row, f.getDataFile(), polarity, MsType.MSMS, f.getMostIntenseFragmentScanNumber(), msAnnotationsFlags);
                            writeSpectrum(writer, dps);
                        }
                    }
                }
            }
        } else {
            // write correlation spectrum
            writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.CORRELATED, -1, msAnnotationsFlags);
            writeCorrelationSpectrum(writer, row);
            // merge everything into one
            MergedSpectrum spectrum = mergeMethod.mergeAcrossSamples(row, massListName).filterByRelativeNumberOfScans(mergeParameters.getParameter(MsMsSpectraMergeParameters.PEAK_COUNT_PARAMETER).getValue());
            if (spectrum.data.length > 0) {
                writeHeader(writer, row, row.getBestPeak().getDataFile(), polarity, MsType.MSMS, spectrum, msAnnotationsFlags);
                writeSpectrum(writer, spectrum.data);
            }
        }

        return true;
    }

    /**
     * Creates header for groupID, compoundGroupID compoundMass and ion annotation
     *
     * @param row
     * @return
     */
    public static String createMSAnnotationFlags(PeakListRow row, NumberFormat mzForm) {
        // MS annotation and feature correlation group
        // can be null (both)
        // run MS annotations module or better metaMSEcorrelate
        RowGroup group = row.getGroup();
        IonIdentity adduct = row.getBestIonIdentity();
        IonNetwork net = adduct != null ? adduct.getNetwork() : null;

        // find ion species by annotation (can be null)
        String corrGroupID = group != null ? "" + group.getGroupID() : "";

        String ion = "";
        String compoundGroupID = "";
        String compoundMass = "";
        if (adduct != null) {
            ion = adduct.getAdduct();
        }
        if (net != null) {
            compoundGroupID = net.getID() + "";
            compoundMass = mzForm.format(net.calcNeutralMass());
        }

        StringBuilder b = new StringBuilder();
        if (!corrGroupID.isEmpty())
            b.append(CORR_GROUPID + corrGroupID + "\n");
        if (!compoundGroupID.isEmpty())
            b.append(COMPOUND_ID + compoundGroupID + "\n");
        if (!compoundMass.isEmpty())
            b.append(COMPOUND_MASS + compoundMass + "\n");
        if (!ion.isEmpty())
            b.append(ION + ion + "\n");
        return b.toString();
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
                             MsType msType, Integer scanNumber, String msAnnotationsFlags) throws IOException {
        writeHeader(writer, row, raw, polarity, msType, scanNumber, null, msAnnotationsFlags);
    }


    private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
                             MsType msType, MergedSpectrum mergedSpectrum, String msAnnotationsFlags) throws IOException {
        writeHeader(writer, row, raw, polarity, msType, row.getID(), Arrays.stream(mergedSpectrum.origins).map(RawDataFile::getName).collect(Collectors.toList()), msAnnotationsFlags);
        // add additional fields
        writer.write("MERGED_SCANS=");
        writer.write(String.valueOf(mergedSpectrum.scanIds[0]));
        for (int k=1; k < mergedSpectrum.scanIds.length; ++k) {
            writer.write(',');
            writer.write(String.valueOf(mergedSpectrum.scanIds[k]));
        }
        writer.newLine();
        writer.write("MERGED_STATS=");
        writer.write(mergedSpectrum.getMergeStatsDescription());
        writer.newLine();
    }


    private void writeHeader(BufferedWriter writer, PeakListRow row, RawDataFile raw, char polarity,
                             MsType msType, Integer scanNumber, List<String> sources, String msAnnotationsFlags)
            throws IOException {
        final Feature feature = row.getPeak(raw);
        writer.write("BEGIN IONS");
        writer.newLine();
        writer.write("FEATURE_ID=");
        int id = renumberID ? nextID : row.getID();
        writer.write(String.valueOf(id));
        writer.newLine();
        writer.write("PEPMASS=");
        writer.write(mzForm.format(row.getBestPeak().getMZ()));
        writer.newLine();

        if (msAnnotationsFlags != null && !msAnnotationsFlags.isEmpty()) {
            writer.write(msAnnotationsFlags);
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
    private void writeCorrelationSpectrum(final BufferedWriter writer, PeakListRow mainRow)
            throws IOException {
        /*
         * Grouped by metaMSEcorrelate Annotations by MS annotations in module
         */
        // get all rows in corr group
        final RowGroup g = mainRow.getGroup();
        Map<PeakListRow, IonIdentity> rows = new HashMap<PeakListRow, IonIdentity>();

        // add all from network
        IonIdentity id = mainRow.getBestIonIdentity();
        IonNetwork network = id == null ? null : id.getNetwork();
        if (network != null) {
            for (Entry<PeakListRow, IonIdentity> e : network.entrySet()) {
                // filter duplicates
                boolean isDuplicate = false;
                for (Entry<PeakListRow, IonIdentity> other : rows.entrySet()) {
                    PeakListRow or = other.getKey();
                    PeakListRow nr = e.getKey();
                    if (mzTol.checkWithinTolerance(or.getAverageMZ(), nr.getAverageMZ())) {
                        isDuplicate = true;
                        // same mz? export only the one row with the highest intensity
                        if (or.getBestPeak().getHeight() < nr.getBestPeak().getHeight()) {
                            rows.remove(or);
                            rows.put(nr, e.getValue());
                            break;
                        }
                    }
                }
                // add new row
                if (!isDuplicate)
                    rows.put(e.getKey(), e.getValue());
            }
        }
        if (rows.isEmpty()) {
            rows.put(mainRow, id);
        }

        // add all group rows that are correlated to main
        // might be too many
        // if (g != null) {
        // for (int i = 0; i < g.size(); ++i) {
        // PeakListRow row = g.get(i);
        // if (g.isCorrelated(mainRow, row) && !rows.containsKey(row)) {
        // rows.put(row, null);
        // }
        // }
        // }

        // export all rows
        rows.entrySet().stream()
                .sorted((a, b) -> Double.compare(a.getKey().getAverageMZ(), b.getKey().getAverageMZ()))
                .forEach(e -> {
                    PeakListRow r = e.getKey();
                    R2GroupCorrelationData corrb = null;
                    if (g instanceof CorrelationRowGroup)
                        corrb = ((CorrelationRowGroup) g).getCorr(r);
                    try {
                        exportCorrelatedRow(writer, r, corrb, e.getValue());
                    } catch (IOException e1) {
                        logger.log(Level.SEVERE, e1.getMessage(), e1);
                    }
                });
        writer.write("END IONS");
        writer.newLine();
        writer.newLine();
    }

    protected int np1 = 0, np2 = 0, np3 = 0, np4 = 0, npw = 0, npe=0;

    protected void exportCorrelatedRow(BufferedWriter writer, PeakListRow row,
                                       R2GroupCorrelationData corr, IonIdentity id) throws IOException {
        double r = corr == null ? 0 : corr.getAvgPeakShapeR();

        final DataPoint[] isotopes = IsotopeUtils.extractIsotopes(row);

        ++np1;
        if (isotopes.length >= 2) {
            ++np2;
        }
        if (isotopes.length >= 3) {
            ++np3;
        }
        if (isotopes.length >= 4) {
            ++np4;
        }
        if (row.getBestIsotopePattern()!=null && row.getBestIsotopePattern().getDataPoints().length >= isotopes.length) {
            ++npe;
        }
        if (row.getBestIsotopePattern()!=null && row.getBestIsotopePattern().getDataPoints().length > isotopes.length) {
            ++npw;
            System.out.println(IsotopeUtils.extractIsotopes(row));
        }
        System.out.printf(Locale.US, "total = %d, +2 = %d, +3 = %d, +4 = %d, better = %d, worse = %d, equal = %d\n",
                np1, np2, np3, np4, np1-npe, npw, npe);

        if (id == null)
            id = row.getBestIonIdentity();
        IonNetwork network = id == null ? null : id.getNetwork();

        for (int k=0; k < isotopes.length; ++k) {
            writer.write(String.valueOf(isotopes[k].getMZ()));
            writer.write('\t');
            writer.write(String.valueOf(isotopes[k].getIntensity()));
            if (id != null && k==0) {
                writer.write('\t');
                writer.write(id.getAdduct().toString());
                if (corr!=null) {
                    writer.write('\t');
                    writer.write(String.valueOf(corr.getAvgPeakShapeR()));
                }
            }
            writer.newLine();
        }
    }

    /**
     * Write detected isotope pattern
     *
     * @param writer
     * @param row
     * @throws IOException
     */
    private void writeCorrelationIsotopes(BufferedWriter writer, PeakListRow row) throws IOException {
        /*
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
                    writer.write(" " + dp.getIntensity());

                    // isotope
                    int charge = row.getRowCharge() == 0 ? 1 : row.getRowCharge();
                    writer.write(" 1"); // skip correlation
                    writer.write(" +" + Math.round((float) (dp.getMZ() - mz0) * charge));
                    writer.newLine();
                }
            }
        }
        */
        /*{
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("isoPeaks.txt", true))) {
                final Feature feature = row.getBestPeak();
                final DataPoint[] isotopePattern = IsotopeUtils.extractIsotopes(feature);
                bw.write("---- NOW -----\n");
                for (DataPoint p : isotopePattern) {
                    bw.write(p.getMZ() + "\t" + p.getIntensity());
                    bw.newLine();
                }
                bw.write("---- BEFORE -----\n");
                if (feature.getIsotopePattern()!=null && feature.getIsotopePattern().getDataPoints().length>=1) {
                    IsotopePattern iso = feature.getIsotopePattern();
                    double base = iso.getDataPoints()[0].getIntensity();
                    for (DataPoint p : iso.getDataPoints()) {
                        bw.write(p.getMZ() + "\t" + (100d*p.getIntensity() / base));
                        bw.newLine();
                    }
                }

            }
        }
        */
        final Feature feature = row.getBestPeak();
        final DataPoint[] isotopePattern = IsotopeUtils.extractIsotopes(feature);
        for (DataPoint dp : isotopePattern) {
            writer.write(dp.getMZ() + "\t" + dp.getIntensity());
            writer.newLine();
        }
    }

    private void writeSpectrum(BufferedWriter writer, DataPoint[] dps) throws IOException {
        for (DataPoint dp : dps) {
            writer.write(mzForm.format(dp.getMZ()));
            writer.write(" " + dp.getIntensity());
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
