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

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;
import com.itextpdf.text.DocumentException;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.librarymatchresultsexport.LibraryMatchResultsExportParameters.Formats;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.MatchSortMode;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsModule;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsParameters;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectraIdentificationResultsWindow;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchCompareParameters;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchPanel;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchUtils;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;
import net.sf.mzmine.util.spectraldb.parser.UnsupportedFormatException;

public class LibraryMatchGraphicsExportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private final static String plNamePattern = "{}";
  private final static String DEL = ",";
  private final static String NL = "\n";
  private final static String Q = "\"";

  private AtomicInteger threads = new AtomicInteger(0);
  private AtomicInteger threadsFinished = new AtomicInteger(0);

  private final PeakList[] peakLists;
  private File fileName;
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

  private String[] formats;

  private boolean substitute;

  private int graphicsWidth;

  private boolean useGraphicsFolder;


  @Override
  public double getFinishedPercentage() {
    return (totalRows == 0 ? 0.0 : (double) finishedRows / (double) totalRows);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting spectral libary matches in feature list(s) " + Arrays.toString(peakLists)
        + " to graphics file(s)";
  }

  LibraryMatchGraphicsExportTask(ParameterSet parameters) {
    this.peakLists = parameters.getParameter(LibraryMatchResultsExportParameters.PEAK_LISTS)
        .getValue().getMatchingPeakLists();
    this.fileName =
        parameters.getParameter(LibraryMatchResultsExportParameters.FILENAME).getValue();
    this.massListName =
        parameters.getParameter(LibraryMatchResultsExportParameters.MASS_LIST).getValue();


    boolean useGraphicsFileName =
        parameters.getParameter(LibraryMatchResultsExportParameters.FILENAME_GRAPHICS).getValue();
    if (useGraphicsFileName) {
      this.fileName = parameters.getParameter(LibraryMatchResultsExportParameters.FILENAME_GRAPHICS)
          .getEmbeddedParameter().getValue();
    }
    useGraphicsFolder =
        parameters.getParameter(LibraryMatchResultsExportParameters.GRAPHICS_FOLDER).getValue();
    graphicsWidth = (int) ((double) parameters
        .getParameter(LibraryMatchResultsExportParameters.WIDTH).getValue());

    // graphics formats
    Formats[] form =
        parameters.getParameter(LibraryMatchResultsExportParameters.GRAPHICS).getValue();

    boolean all = Arrays.stream(form).anyMatch(f -> f.equals(Formats.ALL));
    if (all) {
      formats = new String[] {"all"};
    } else {
      formats = Arrays.stream(form).map(f -> f.toString()).toArray(String[]::new);
    }


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

    substitute = fileName.getPath().contains(plNamePattern);

    // Process feature lists
    for (PeakList pkl : peakLists) {
      try {
        exportPeakList(pkl);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Error while exporting graphics");
        logger.log(Level.SEVERE, "Error while exporting graphics", e);
        return;
      }
    }

    while (true) {
      if (threads.get() > 0 && threads.get() == threadsFinished.get())
        break;
      else {
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          logger.log(Level.WARNING, "Cannot sleep thread in graphics export");
          break;
        }
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
   * @param match
   * @return
   */
  private File getRealFileName(boolean substitute, PeakList peakList,
      SpectralDBPeakIdentity match) {
    // Cleanup from illegal filename characters
    String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9-]", "_");

    String newFilename = FileAndPathUtil.eraseFormat(fileName).getAbsolutePath();
    if (useGraphicsFolder) {
      newFilename = new File(newFilename, cleanPlName).getAbsolutePath();
    }

    newFilename += "_" + match.getEntry().getField(DBEntryField.NAME).orElse("").toString()
        .replaceAll("[^a-zA-Z0-9-]", "_");
    newFilename +=
        "_" + match.getEntry().getField(DBEntryField.COLLISION_ENERGY).orElse("").toString();
    newFilename += "_" + match.getEntry().getField(DBEntryField.ION_TYPE).orElse("").toString();
    newFilename += "_" + finishedRows;
    newFilename += "_ID" + match.getEntry().getField(DBEntryField.ENTRY_ID).orElse("").toString();

    if (substitute) {
      // Substitute
      newFilename = newFilename.replaceAll(Pattern.quote(plNamePattern), cleanPlName);

      return FileAndPathUtil.getRealFilePath(new File(newFilename), "pdf", true);
    } else
      return FileAndPathUtil.getRealFilePath(new File(newFilename), "pdf", true);
  }

  private void exportPeakList(PeakList pkl)
      throws IOException, DocumentException, UnsupportedFormatException {
    List<SpectralDBPeakIdentity> matches = getMatches(pkl);
    // collapse multiples
    if (collapse) {
      matches =
          SpectralMatchUtils.collapseList(matches, collapseFields, compareDataPoints, factorScore);
    }
    // sort
    SpectralMatchUtils.sort(matches, sorting, factorScore);
    totalRows += matches.size();

    SpectraIdentificationResultsWindow window = new SpectraIdentificationResultsWindow();
    window.setSize(graphicsWidth, 1000);
    ParameterSet param =
        MZmineCore.getConfiguration().getModuleParameters(SpectraIdentificationResultsModule.class);
    param.getParameter(SpectraIdentificationResultsParameters.collapse).setValue(false);

    window.addMatches(matches);


    // export to csv
    for (Map.Entry<SpectralDBPeakIdentity, SpectralMatchPanel> match : window.getMatchPanels()
        .entrySet()) {
      try {
        // Filename
        File curFile = getRealFileName(substitute, pkl, match.getKey());
        exportMatch(window, match, curFile);
      } catch (Exception e) {
        logger.log(Level.WARNING, "Cannot export graphics", e);
      }
      finishedRows++;
    }
  }

  private void exportMatch(SpectraIdentificationResultsWindow window,
      final Entry<SpectralDBPeakIdentity, SpectralMatchPanel> match, final File curFile)
      throws DocumentException, UnsupportedFormatException, IOException {
    logger.info("Exporting graphics for library entry to " + curFile.getAbsolutePath());
    SwingUtilities.invokeLater(() -> {
      Thread t = new Thread(() -> {
        threads.getAndIncrement();
        if (!isCanceled()) {
          boolean exported = match.getValue().exportToGraphics(curFile, formats);
          if (!exported)
            logger.log(Level.WARNING, "Cannot export graphics to " + curFile.getAbsolutePath());
        }
        if (threadsFinished.incrementAndGet() >= threads.get()) {
          window.setVisible(false);
        }
      });
      t.start();
    });
  }


  private List<SpectralDBPeakIdentity> getMatches(PeakList pkl) {
    return Arrays.stream(pkl.getRows()).flatMap(row -> Arrays.stream(row.getPeakIdentities()))
        .filter(pi -> pi instanceof SpectralDBPeakIdentity).map(pi -> ((SpectralDBPeakIdentity) pi))
        .collect(Collectors.toList());
  }

}
