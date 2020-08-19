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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.MatchSortMode;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchCompareParameters;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchUtils;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class LibraryMatchResultsExportTask extends AbstractTask {

  private boolean DEBUG_MODE;

  private final static String plNamePattern = "{}";
  private final static String DEL = ",";
  private final static String NL = "\n";

  private final PeakList[] peakLists;
  private final File fileName;
  private final String massListName;
  protected long finishedRows, totalRows;
  protected int exported = 0;

  private NumberFormat intensityForm = MZmineCore.getConfiguration().getIntensityFormat();
  private NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat retentionTimeForm = MZmineCore.getConfiguration().getRTFormat();

  private Boolean collapse;
  private DBEntryField[] collapseFields;
  private Boolean compareDataPoints;
  private MatchSortMode sorting;
  private Double factorScore;


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

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    for (PeakList l : peakLists) {
      this.totalRows += l.getNumberOfRows();
    }

    // Process feature lists
    for (PeakList pkl : peakLists) {
      // Filename
      File curFile = getRealFileName(substitute, pkl);

      // Open file
      try (final BufferedWriter bw = new BufferedWriter(new FileWriter(curFile, true))) {
        exportPeakList(pkl, bw);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
      }
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
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
   * append line to csv for match
   * 
   * @param match
   * @param writer
   * @throws IOException
   */
  private void exportMatch(SpectralDBPeakIdentity match, BufferedWriter writer) throws IOException {
    StringBuilder s = new StringBuilder();
    // Sample specific
    s.append(match.getQueryScan().getDataFile().getName() + DEL);
    s.append(match.getQueryScan().getMSLevel());

    // match scores

    // library entry metadata

    // annotated signals matched (ion identities)

    s.append(NL);
    writer.append(s.toString());
  }

  private List<SpectralDBPeakIdentity> getMatches(PeakList pkl) {
    return Arrays.stream(pkl.getRows()).flatMap(row -> Arrays.stream(row.getPeakIdentities()))
        .filter(pi -> pi instanceof SpectralDBPeakIdentity).map(pi -> ((SpectralDBPeakIdentity) pi))
        .collect(Collectors.toList());
  }

}
