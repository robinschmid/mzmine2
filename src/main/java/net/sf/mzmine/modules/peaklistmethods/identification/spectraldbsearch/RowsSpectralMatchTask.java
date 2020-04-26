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

package net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.MZmineProcessingStep;
import net.sf.mzmine.modules.peaklistmethods.identification.spectraldbsearch.sort.SortSpectralDBIdentitiesTask;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoper;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.isotopes.MassListDeisotoperParameters;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.spectraldatabase.SpectralMatchTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.exceptions.MissingMassListException;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.scans.ScanUtils;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarity;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import net.sf.mzmine.util.scans.sorting.ScanSortMode;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

public class RowsSpectralMatchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String METHOD = "Spectral DB search";
  private static final int MAX_ERROR = 3;
  private int errorCounter = 0;
  private String description;
  private PeakListRow[] rows;
  private final @Nonnull String massListName;
  private final File dataBaseFile;
  private final MZTolerance mzToleranceSpectra;
  private final MZTolerance mzTolerancePrecursor;
  private final RTTolerance rtTolerance;
  private final boolean useRT;
  private int finishedRows = 0;
  private final int totalRows;

  private ParameterSet parameters;

  private final int msLevel;
  private final double noiseLevel;
  private final int minMatch;
  private List<SpectralDBEntry> list;

  private int count = 0;

  // as this module is started in a series the start entry is saved to track progress
  private int startEntry;
  private int listsize;
  private MZmineProcessingStep<SpectralSimilarityFunction> simFunction;

  // remove precursor dp from scans
  private boolean removePrecursor;

  // remove 13C isotopes
  private boolean removeIsotopes;
  private MassListDeisotoperParameters deisotopeParam;

  private final boolean cropSpectraToOverlap;
  // listen for matches
  private Consumer<SpectralDBPeakIdentity> matchListener;

  private boolean allMS2Scans;

  // needs any signals within mzToleranceSpectra for
  // 13C, H, 2H or Cl
  private boolean needsIsotopePattern;
  private int minMatchedIsoSignals;


  // init
  public RowsSpectralMatchTask(String description, @Nonnull PeakListRow[] rows,
      ParameterSet parameters, int startEntry, List<SpectralDBEntry> list) {
    this(description, rows, parameters, startEntry, list, null);
  }

  public RowsSpectralMatchTask(String description, @Nonnull PeakListRow[] rows,
      ParameterSet parameters, int startEntry, List<SpectralDBEntry> list,
      Consumer<SpectralDBPeakIdentity> matchListener) {
    this.description = description;
    this.rows = rows;
    this.parameters = parameters;
    this.startEntry = startEntry;
    this.list = list;
    this.matchListener = matchListener;
    listsize = list.size();
    dataBaseFile = parameters.getParameter(LocalSpectralDBSearchParameters.dataBaseFile).getValue();
    massListName = parameters.getParameter(LocalSpectralDBSearchParameters.massList).getValue();
    mzToleranceSpectra =
        parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerance).getValue();
    msLevel = parameters.getParameter(LocalSpectralDBSearchParameters.msLevel).getValue();
    noiseLevel = parameters.getParameter(LocalSpectralDBSearchParameters.noiseLevel).getValue();

    useRT = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance).getValue();
    rtTolerance = parameters.getParameter(LocalSpectralDBSearchParameters.rtTolerance)
        .getEmbeddedParameter().getValue();

    minMatch = parameters.getParameter(LocalSpectralDBSearchParameters.minMatch).getValue();
    simFunction =
        parameters.getParameter(LocalSpectralDBSearchParameters.similarityFunction).getValue();
    needsIsotopePattern =
        parameters.getParameter(LocalSpectralDBSearchParameters.needsIsotopePattern).getValue();
    minMatchedIsoSignals = !needsIsotopePattern ? 0
        : parameters.getParameter(LocalSpectralDBSearchParameters.needsIsotopePattern)
            .getEmbeddedParameter().getValue();
    removePrecursor =
        parameters.getParameter(LocalSpectralDBSearchParameters.removePrecursor).getValue();
    removeIsotopes =
        parameters.getParameter(LocalSpectralDBSearchParameters.deisotoping).getValue();
    deisotopeParam = parameters.getParameter(LocalSpectralDBSearchParameters.deisotoping)
        .getEmbeddedParameters();
    cropSpectraToOverlap =
        parameters.getParameter(LocalSpectralDBSearchParameters.cropSpectraToOverlap).getValue();
    if (msLevel > 1)
      mzTolerancePrecursor =
          parameters.getParameter(LocalSpectralDBSearchParameters.mzTolerancePrecursor).getValue();
    else
      mzTolerancePrecursor = null;

    allMS2Scans = parameters.getParameter(LocalSpectralDBSearchParameters.allMS2Spectra).getValue();

    totalRows = rows.length;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return ((double) finishedRows) / totalRows;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return MessageFormat.format(
        "(entry {2}-{3}) spectral database identification in {0} using database {1}", description,
        dataBaseFile.getName(), startEntry, startEntry + listsize - 1);
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    // for each row in list rows
    for (PeakListRow row : rows) {
      // stop if user canceled the task
      if (isCanceled()) {
        logger.info("Added " + count + " spectral library matches (before being cancelled)");
        repaintWindow();
        return;
      }

      try {
        // All MS2 or only best MS2 scan
        // best MS1 scan
        // check for MS1 or MSMS scan
        List<Scan> scans = getScans(row);
        List<DataPoint[]> rowMassLists = new ArrayList<>();
        for (Scan scan : scans) {
          // get mass list and perform deisotoping if active
          DataPoint[] rowMassList = getDataPoints(scan, true);
          if (removeIsotopes)
            rowMassList = removeIsotopes(rowMassList);
          rowMassLists.add(rowMassList);
        }

        // match against all library entries
        for (SpectralDBEntry ident : list) {
          SpectralDBPeakIdentity best = null;
          // match all scans against this ident to find best match
          for (int i = 0; i < scans.size(); i++) {
            SpectralSimilarity sim = spectraDBMatch(row, rowMassLists.get(i), ident);
            if (sim != null
                && (!needsIsotopePattern || SpectralMatchTask.checkForIsotopePattern(sim,
                    mzToleranceSpectra, minMatchedIsoSignals))
                && (best == null || best.getSimilarity().getScore() < sim.getScore())) {
              best = new SpectralDBPeakIdentity(scans.get(i), massListName, ident, sim, METHOD);
            }
          }
          // has match?
          if (best != null) {
            addIdentity(row, best);
            count++;
          }
        }
        // sort identities based on similarity score
        SortSpectralDBIdentitiesTask.sortIdentities(row);
      } catch (MissingMassListException e) {
        logger.log(Level.WARNING, "No mass list in spectrum for rowID=" + row.getID(), e);
        errorCounter++;
      }
      // check for max error (missing masslist)
      if (errorCounter > MAX_ERROR) {
        logger.log(Level.WARNING, "Data base matching failed. To many missing mass lists ");
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Data base matching failed. To many missing mass lists ");
        list = null;
        return;
      }
      // next row
      finishedRows++;
    }
    if (count > 0)
      logger.info("Added " + count + " spectral library matches");

    // Repaint the window to reflect the change in the feature list
    repaintWindow();

    list = null;

    setStatus(TaskStatus.FINISHED);
  }

  private void repaintWindow() {
    Desktop desktop = MZmineCore.getDesktop();
    if (!(desktop instanceof HeadLessDesktop))
      desktop.getMainWindow().repaint();
  }

  /**
   * Remove 13C isotopes from masslist
   * 
   * @param a
   * @return
   */
  private DataPoint[] removeIsotopes(DataPoint[] a) {
    return MassListDeisotoper.filterIsotopes(a, deisotopeParam);
  }

  /**
   * 
   * @param row
   * @param ident
   * @return spectral similarity or null if no match
   */
  private SpectralSimilarity spectraDBMatch(PeakListRow row, DataPoint[] rowMassList,
      SpectralDBEntry ident) {
    // retention time
    // MS level 1 or check precursorMZ
    if (checkRT(row, ident) && (msLevel == 1 || checkPrecursorMZ(row, ident))) {
      DataPoint[] library = ident.getDataPoints();
      if (removeIsotopes)
        library = removeIsotopes(library);

      // crop the spectra to their overlapping mz range
      // helpful when comparing spectra, acquired with different fragmentation energy
      DataPoint[] query = rowMassList;
      if (cropSpectraToOverlap) {
        DataPoint[][] cropped = ScanAlignment.cropToOverlap(mzToleranceSpectra, library, query);
        library = cropped[0];
        query = cropped[1];
      }

      if (msLevel > 1 && removePrecursor && ident.getPrecursorMZ() != null) {
        // precursor mz from library entry for signal filtering
        double precursorMZ = ident.getPrecursorMZ();
        // remove from both spectra
        library = removePrecursor(library, precursorMZ);
        query = removePrecursor(query, precursorMZ);
      }

      // check spectra similarity
      SpectralSimilarity sim = createSimilarity(library, query);
      if (sim != null) {
        return sim;
      }
    }
    return null;
  }


  private DataPoint[] removePrecursor(DataPoint[] masslist, double precursorMZ) {
    List<DataPoint> filtered = new ArrayList<DataPoint>();
    for (DataPoint dp : masslist) {
      double mz = dp.getMZ();
      // skip precursor mz
      if (!mzTolerancePrecursor.checkWithinTolerance(mz, precursorMZ)) {
        filtered.add(dp);
      }
    }
    return filtered.toArray(new DataPoint[filtered.size()]);
  }

  /**
   * Uses the similarity function and filter to create similarity.
   * 
   * @param a
   * @param b
   * @return positive match with similarity or null if criteria was not met
   */
  private SpectralSimilarity createSimilarity(DataPoint[] library, DataPoint[] query) {
    return simFunction.getModule().getSimilarity(simFunction.getParameterSet(), mzToleranceSpectra,
        minMatch, library, query);
  }

  private boolean checkPrecursorMZ(PeakListRow row, SpectralDBEntry ident) {
    if (ident.getPrecursorMZ() == null)
      return false;
    else
      return mzTolerancePrecursor.checkWithinTolerance(ident.getPrecursorMZ(), row.getAverageMZ());
  }

  private boolean checkRT(PeakListRow row, SpectralDBEntry ident) {
    Double rt = (Double) ident.getField(DBEntryField.RT).orElse(null);
    return (!useRT || rt == null || rtTolerance.checkWithinTolerance(rt, row.getAverageRT()));
  }

  /**
   * Thresholded masslist
   * 
   * @param row
   * @return
   * @throws MissingMassListException
   */
  private DataPoint[] getDataPoints(Scan scan, boolean noiseFilter)
      throws MissingMassListException {
    if (scan == null || scan.getMassList(massListName) == null) {
      return new DataPoint[0];
    }

    MassList masses = scan.getMassList(massListName);
    DataPoint[] dps = masses.getDataPoints();
    return noiseFilter ? ScanUtils.getFiltered(dps, noiseLevel) : dps;
  }

  public List<Scan> getScans(PeakListRow row) throws MissingMassListException {
    if (msLevel <= 1) {
      List<Scan> scans = new ArrayList<>();
      scans.add(row.getBestPeak().getRepresentativeScan());
      return scans;
    } else {
      // first entry is the best scan
      List<Scan> scans = ScanUtils.listAllFragmentScans(row, massListName, noiseLevel, minMatch,
          ScanSortMode.MAX_TIC);
      if (allMS2Scans)
        return scans;
      else {
        // only keep first (with highest TIC)
        while (scans.size() > 1) {
          scans.remove(1);
        }
        return scans;
      }
    }
  }

  private void addIdentity(PeakListRow row, SpectralDBPeakIdentity pid) {
    // add new identity to the row
    row.addPeakIdentity(pid, false);

    if (matchListener != null)
      matchListener.accept(pid);
  }

  public int getCount() {
    return count;
  }

}
