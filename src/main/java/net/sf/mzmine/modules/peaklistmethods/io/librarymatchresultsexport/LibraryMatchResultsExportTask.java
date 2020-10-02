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

package net.sf.mzmine.modules.peaklistmethods.io.librarymatchresultsexport;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.LibraryScan;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.identities.iontype.CombinedIonModification;
import net.sf.mzmine.datamodel.identities.iontype.IonModification;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.MatchSortMode;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchCompareParameters;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchUtils;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarity;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class LibraryMatchResultsExportTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());


  private final static String plNamePattern = "{}";
  private final static String DEL = ",";
  private final static String NL = "\n";
  private final static String Q = "\"";

  private final PeakList[] peakLists;
  private final File fileName;
  private final String massListName;
  protected long finishedRows, totalRows;
  protected int exported = 0;

  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat retentionTimeForm = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat ppmForm = new DecimalFormat("0.00");

  private Boolean collapse;
  private DBEntryField[] collapseFields;
  private Boolean compareDataPoints;
  private MatchSortMode sorting;
  private Double factorScore;

  private List<IonType> ionAnnotations;
  private PolarityType polarity;
  private MZTolerance mzTol;

  private boolean hasHeader;


  @Override
  public double getFinishedPercentage() {
    return (totalRows == 0 ? 0.0 : (double) finishedRows / (double) totalRows);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting spectral libary matches in feature list(s) " + Arrays.toString(peakLists)
        + " to csv file(s)";
  }

  LibraryMatchResultsExportTask(ParameterSet parameters) {
    this.peakLists = parameters.getParameter(LibraryMatchResultsExportParameters.PEAK_LISTS)
        .getValue().getMatchingPeakLists();
    this.fileName =
        parameters.getParameter(LibraryMatchResultsExportParameters.FILENAME).getValue();
    this.massListName =
        parameters.getParameter(LibraryMatchResultsExportParameters.MASS_LIST).getValue();

    // sorting
    this.sorting = parameters.getParameter(LibraryMatchResultsExportParameters.sorting).getValue();
    this.factorScore =
        parameters.getParameter(LibraryMatchResultsExportParameters.weightScore).getValue();

    // annotations
    this.mzTol = parameters.getParameter(LibraryMatchResultsExportParameters.mzTol).getValue();
    this.polarity =
        parameters.getParameter(LibraryMatchResultsExportParameters.polarity).getValue();

    // collapse
    collapse = parameters.getParameter(LibraryMatchResultsExportParameters.collapse).getValue();
    ParameterSet collParam = parameters.getParameter(LibraryMatchResultsExportParameters.collapse)
        .getEmbeddedParameters();
    collapseFields = collParam.getParameter(SpectralMatchCompareParameters.fields).getValue();
    compareDataPoints =
        collParam.getParameter(SpectralMatchCompareParameters.dataPoints).getValue();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    initIonAnnotations();

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    for (PeakList l : peakLists) {
      this.totalRows += l.getNumberOfRows();
    }

    // Process feature lists
    for (PeakList pkl : peakLists) {
      // Filename
      File curFile = getRealFileName(substitute, pkl);

      // try to read first line
      hasHeader = readFirstLine(curFile);

      // Open file
      try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile, true))) {
        if (!hasHeader) {
          exportHeader(bw);
          hasHeader = true;
        }
        //
        exportPeakList(pkl, bw);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.SEVERE, "Could not open file " + curFile, e);
        return;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private boolean readFirstLine(File file) {
    try (final BufferedReader brTest = new BufferedReader(new FileReader(file))) {
      if (brTest.readLine().length() > 8)
        return true;
      else
        return false;
    } catch (IOException e) {
      return false;
    }
  }

  private void initIonAnnotations() {
    ionAnnotations = new ArrayList<>();
    for (int i = 1; i < 3; i++) {
      if (!polarity.equals(PolarityType.NEGATIVE)) {
        ionAnnotations.add(new IonType(i, IonModification.M_PLUS, null));
        ionAnnotations.add(new IonType(i, IonModification.H, null));
        ionAnnotations.add(new IonType(i, IonModification.NA, null));
        ionAnnotations.add(new IonType(i, IonModification.Hneg_NA2, null));
        ionAnnotations.add(new IonType(i, IonModification.K, null));
        ionAnnotations.add(new IonType(i, IonModification.H, IonModification.H2O));
        ionAnnotations.add(new IonType(i, IonModification.H, IonModification.C2H4));

        ionAnnotations.add(new IonType(i, IonModification.M_PLUS, IonModification.CL_H_EXCHANGE));
        ionAnnotations.add(new IonType(i, IonModification.M_PLUS, new CombinedIonModification(
            IonModification.CL_H_EXCHANGE, IonModification.CL_H_EXCHANGE)));
        ionAnnotations.add(new IonType(i, IonModification.H, IonModification.CL_H_EXCHANGE));
        ionAnnotations.add(new IonType(i, IonModification.H, new CombinedIonModification(
            IonModification.CL_H_EXCHANGE, IonModification.CL_H_EXCHANGE)));
      }
      // neg
      if (!polarity.equals(PolarityType.POSITIVE)) {
        ionAnnotations.add(new IonType(i, IonModification.M_MINUS, null));
        ionAnnotations.add(new IonType(i, IonModification.CL, null));
        ionAnnotations.add(new IonType(i, IonModification.H_NEG, null));
        ionAnnotations.add(new IonType(i, IonModification.NA_2H, null));
        ionAnnotations.add(new IonType(i, IonModification.H_NEG, IonModification.H2O));
      }
    }
  }

  /**
   * Substitute file name
   * 
   * @param substitute
   * @param peakList
   * @return
   */
  private File getRealFileName(boolean substitute, PeakList peakList) {
    if (substitute) {
      // Cleanup from illegal filename characters
      String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
      // Substitute
      String newFilename = fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
      return FileAndPathUtil.getRealFilePath(new File(newFilename), "csv");
    } else
      return FileAndPathUtil.getRealFilePath(fileName, "csv");
  }

  private void exportPeakList(PeakList pkl, BufferedWriter writer) throws IOException {
    List<SpectralDBPeakIdentity> matches = getMatches(pkl);
    // collapse multiples
    if (collapse) {
      matches =
          SpectralMatchUtils.collapseList(matches, collapseFields, compareDataPoints, factorScore);
    }
    // sort
    SpectralMatchUtils.sort(matches, sorting, factorScore);

    // export to csv
    for (SpectralDBPeakIdentity match : matches) {
      exportMatch(match, writer);
    }

    finishedRows++;
  }

  /**
   * 
   * @param match
   * @param writer
   * @throws IOException
   */
  private void exportHeader(BufferedWriter writer) throws IOException {
    StringBuilder s = new StringBuilder();
    // second line header
    s.append(DEL);
    s.append(DEL);
    s.append(DEL);

    // match scores
    s.append(DEL);
    s.append(DEL);
    s.append(DEL);
    s.append(DEL);
    s.append(DEL);
    s.append(DEL);
    s.append(DEL);

    s.append(DEL);
    s.append(DEL);
    s.append(DEL);

    for (DBEntryField db : DBEntryField.values())
      s.append(DEL);

    s.append(DEL);

    for (IonType ion : ionAnnotations) {
      s.append(DEL + "Intensity");
      s.append(DEL + "ppm error");
    }

    s.append(DEL);

    s.append(NL);
    // end of first line


    // Sample specific
    s.append("Data File");
    s.append(DEL + "MS Level");
    s.append(DEL + "Spectrum ID");
    s.append(DEL + "Spectrum Name");
    s.append(DEL + "Spectrum Description");

    // match scores
    s.append(DEL + "Matched Signals");
    s.append(DEL + "Combined Score (" + factorScore + "x Score+Explained Lib Intensity)");
    s.append(DEL + "Score");
    s.append(DEL + "Explained Lib Intensity");
    s.append(DEL + "Explained Lib Signals");
    s.append(DEL + "Total Lib Signals");

    s.append(DEL + "Explained Query Intensity");
    s.append(DEL + "Explained Query Signals");
    s.append(DEL + "Total Query Signals");

    // library entry metadata
    for (DBEntryField db : DBEntryField.values()) {
      s.append(DEL + db.toString());
    }

    // name again
    s.append(DEL + "Compound Name");

    // annotated signals matched (ion identities)
    for (IonType ion : ionAnnotations) {
      s.append(DEL + ion.toString(false));
      s.append(DEL + ion.toString(false));
    }

    // name again
    s.append(DEL + "Compound Name");

    // finish line
    s.append(NL);
    writer.append(s.toString());
  }

  /**
   * append line to csv for match
   * 
   * @param match
   * @param writer
   * @throws IOException
   */
  private void exportMatch(SpectralDBPeakIdentity match, BufferedWriter writer) throws IOException {
    SpectralSimilarity sim = match.getSimilarity();
    StringBuilder s = new StringBuilder();
    // Sample specific
    s.append(Q + match.getQueryScan().getDataFile().getName() + Q);
    s.append(DEL + match.getQueryScan().getMSLevel());
    if (match.getQueryScan() instanceof LibraryScan) {
      LibraryScan library = (LibraryScan) match.getQueryScan();

      // library entry metadata
      DBEntryField[] libraryFields =
          new DBEntryField[] {DBEntryField.ENTRY_ID, DBEntryField.NAME, DBEntryField.COMMENT};
      for (DBEntryField db : libraryFields) {
        String entry = match.getEntry().getField(db).orElse("").toString();
        // escape quatation marks
        // entry.replace(Q, Q + Q + Q);
        entry.replace(Q, "");
        if (entry.contains(",")) {
          // escape comma containing strings
          entry = Q + entry + Q;
        }
        s.append(DEL + entry);
      }
    } else {
      s.append(DEL);
      s.append(DEL);
      s.append(DEL);
    }

    // match scores
    s.append(DEL + sim.getOverlap());
    s.append(DEL + SpectralMatchUtils.calcCombinedScore(match, factorScore));
    s.append(DEL + sim.getScore());
    s.append(DEL + sim.getExplainedLibraryIntensityRatio());
    s.append(DEL + sim.getExplainedLibrarySignals());
    s.append(DEL + sim.getTotalLibrarySignals());

    s.append(DEL + sim.getExplainedQueryIntensityRatio());
    s.append(DEL + sim.getExplainedQuerySignals());
    s.append(DEL + sim.getTotalQuerySignals());

    // library entry metadata
    for (DBEntryField db : DBEntryField.values()) {
      s.append(DEL + formatField(match, db));
    }

    // name again
    s.append(DEL + formatField(match, DBEntryField.NAME));

    DataPoint[] data = match.getQueryDataPoints();
    // annotated signals matched (ion identities)
    for (IonType ion : ionAnnotations) {
      // find highest signal for ion
      DataPoint dp = findIon(match, data, ion);

      if (dp != null) {
        Double ionMZ = (Double) match.getEntry().getField(DBEntryField.EXACT_MASS).orElse(null);
        ionMZ = ion.getMZ(ionMZ);
        String intensity = intensityForm.format(dp.getIntensity());
        String ppm = ppmForm.format((dp.getMZ() - ionMZ) / ionMZ * Math.pow(10, 6));
        s.append(DEL + intensity);
        s.append(DEL + ppm);
      } else {
        s.append(DEL + "");
        s.append(DEL + "");
      }
    }

    // name again
    s.append(DEL + formatField(match, DBEntryField.NAME));

    s.append(NL);
    writer.append(s.toString());
  }

  private String formatField(SpectralDBPeakIdentity match, DBEntryField db) {
    String entry = match.getEntry().getField(db).orElse("").toString();
    // escape quatation marks
    // entry.replace(Q, Q + Q + Q);
    entry.replace(Q, "");
    if (entry.contains(",")) {
      // escape comma containing strings
      entry = Q + entry + Q;
    }
    return entry;
  }

  /**
   * Most abundant signal in range
   * 
   * @param match
   * @param data
   * @param ion
   * @return
   */
  private DataPoint findIon(SpectralDBPeakIdentity match, DataPoint[] data, IonType ion) {
    DataPoint best = null;
    Double ionMZ = (Double) match.getEntry().getField(DBEntryField.EXACT_MASS).orElse(null);
    if (ionMZ != null) {
      ionMZ = ion.getMZ(ionMZ);
      for (DataPoint d : data) {
        if (mzTol.checkWithinTolerance(d.getMZ(), ionMZ)
            && (best == null || d.getIntensity() > best.getIntensity())) {
          best = d;
        }
      }
    }
    return best;
  }

  private List<SpectralDBPeakIdentity> getMatches(PeakList pkl) {
    return Arrays.stream(pkl.getRows()).flatMap(row -> Arrays.stream(row.getPeakIdentities()))
        .filter(pi -> pi instanceof SpectralDBPeakIdentity).map(pi -> ((SpectralDBPeakIdentity) pi))
        .collect(Collectors.toList());
  }

}
