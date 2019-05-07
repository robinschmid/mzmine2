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

package net.sf.mzmine.modules.peaklistmethods.isotopes.aligneddeisotoper;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

/**
 * 
 */
public class AlignedIsotopeGrouperTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  // mass 1.003354838
  private static final double isotopeDistance = ESIAdductType.C13.getMassDifference();

  private final MZmineProject project;
  // peaks counter
  private ParameterSet parameters;
  private int processedPeaks, totalPeaks;
  private String suffix;
  private boolean removeOriginal;

  // inputs
  // peaklist and resulting peaklist
  private PeakList peakList, deisotopedPeakList;
  // mass list as extra input
  private final String massList;

  // mz and rt tolerances
  private MZTolerance mzTolerance;
  private RTTolerance rtTolerance;
  // needs monotonic shape? Keep most intense ion? match raw (mass lists)?
  private boolean monotonicShape, chooseMostIntense, matchRaw;
  // all charge states from 1-maxCharge;
  private int maximumCharge;
  // minimum matched raw data points for positive
  private int minMatchRawDP;

  /**
   * @param rawDataFile
   * @param parameters
   */
  public AlignedIsotopeGrouperTask(MZmineProject project, PeakList peakList,
      ParameterSet parameters) {

    this.project = project;
    this.peakList = peakList;
    this.parameters = parameters;

    // Get parameter values for easier use
    suffix = parameters.getParameter(AlignedIsotopeGrouperParameters.suffix).getValue();
    mzTolerance = parameters.getParameter(AlignedIsotopeGrouperParameters.mzTolerance).getValue();
    rtTolerance = parameters.getParameter(AlignedIsotopeGrouperParameters.rtTolerance).getValue();
    monotonicShape =
        parameters.getParameter(AlignedIsotopeGrouperParameters.monotonicShape).getValue();
    maximumCharge =
        parameters.getParameter(AlignedIsotopeGrouperParameters.maximumCharge).getValue();
    chooseMostIntense =
        (parameters.getParameter(AlignedIsotopeGrouperParameters.representativeIsotope)
            .getValue() == AlignedIsotopeGrouperParameters.ChooseTopIntensity);
    removeOriginal = parameters.getParameter(AlignedIsotopeGrouperParameters.autoRemove).getValue();
    massList = parameters.getParameter(AlignedIsotopeGrouperParameters.massList).getValue();
    matchRaw = parameters.getParameter(AlignedIsotopeGrouperParameters.minMatchRawDP).getValue();
    minMatchRawDP = parameters.getParameter(AlignedIsotopeGrouperParameters.minMatchRawDP)
        .getEmbeddedParameter().getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Isotopic peaks grouper on " + peakList;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalPeaks == 0)
      return 0.0f;
    return (double) processedPeaks / (double) totalPeaks;
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    logger.info("Running aligned isotopic peak grouper on " + peakList);

    // Create a new deisotoped peakList
    deisotopedPeakList = new SimplePeakList(peakList + " " + suffix, peakList.getRawDataFiles());

    // Sort peaks by ascending avgMZ
    // peaks already identified as isotopes will be set to null
    PeakListRow[] rows = peakList.getRows();
    Arrays.sort(rows, new PeakListRowSorter(SortingProperty.MZ, SortingDirection.Ascending));

    int rawFilledIn = 0;
    int totalNoCharge = 0;
    int foundIsotopeRow = 0;
    // for all rows
    for (int r = 0; r < rows.length; r++) {
      if (isCanceled())
        return;

      if (rows[r] == null)
        continue;
      // search for a higher row in distance
      // row could be an artifact of a high peak -> ms shoulder peak
      // or bad mass detection
      int maxRow = searchForMaxRowInDist(peakList.getRawDataFiles(), rows, r);
      PeakListRow row = rows[maxRow];
      // finish maxRow first and then do rows[r] again
      if (r != maxRow)
        r--;

      // Check which charge state fits best around this peak
      int bestFitCharge = 0;
      int bestFitScore = -1;
      Vector<PeakListRow> bestFitRows = null;
      for (int charge = 1; charge <= maximumCharge; charge++) {

        Vector<PeakListRow> fittedRows = new Vector<PeakListRow>();
        fittedRows.add(row);
        fitPattern(peakList.getRawDataFiles(), fittedRows, maxRow, charge, rows);

        int score = fittedRows.size();
        if ((score > bestFitScore) || ((score == bestFitScore) && (bestFitCharge > charge))) {
          bestFitScore = score;
          bestFitCharge = charge;
          bestFitRows = fittedRows;
        }

      }

      assert bestFitRows != null;

      // Verify the number of detected isotopes. If there is only one
      // Isotope, we skip this left the original peak in the peak list.
      if (bestFitRows.size() <= 1) {
        PeakListRow copy = MSEGroupedPeakList.copyPeakRow(row);
        deisotopedPeakList.addRow(copy);
        // try to find and fill in isotope peaks from the highest 2 raw files
        if (matchRaw) {
          totalNoCharge++;
          int z = findChargeStateInRaw(peakList.getRawDataFiles(), copy);
          // add charge if found
          if (z > 0) {
            rawFilledIn++;

            // Create a new IsotopePattern

            SimpleIsotopePattern newPattern = null;
            if (bestRawIsoPattern != null) {
              DataPoint[] isotopes =
                  bestRawIsoPattern.toArray(new DataPoint[bestRawIsoPattern.size()]);
              newPattern =
                  new SimpleIsotopePattern(isotopes, IsotopePatternStatus.RAW, copy.toString());
            }

            for (Feature f : copy.getPeaks()) {
              f.setCharge(z);
              if (newPattern != null) {
                f.setIsotopePattern(newPattern);
              }
            }
          }
        }
        // erase
        rows[maxRow] = null;
        processedPeaks++;
        continue;
      }

      foundIsotopeRow++;
      // Convert the peak pattern to array
      PeakListRow originalRows[] = bestFitRows.toArray(new PeakListRow[bestFitRows.size()]);

      // Create a new SimpleIsotopePattern
      DataPoint isotopes[] = new DataPoint[bestFitRows.size()];
      for (int i = 0; i < isotopes.length; i++) {
        PeakListRow p = originalRows[i];
        double avgHeight = 0;
        for (Feature f : p.getPeaks())
          avgHeight += f.getHeight();
        avgHeight = avgHeight / p.getPeaks().length;

        isotopes[i] = new SimpleDataPoint(p.getAverageMZ(), avgHeight);
      }

      // Depending on user's choice, we leave either the most intenst, or
      // the lowest m/z peak
      if (chooseMostIntense) {
        Arrays.sort(originalRows,
            new PeakListRowSorter(SortingProperty.Height, SortingDirection.Descending));
      }
      // selected row
      PeakListRow selRow = originalRows[0];

      SimpleIsotopePattern newPattern =
          new SimpleIsotopePattern(isotopes, IsotopePatternStatus.DETECTED, selRow.toString());

      // copy
      PeakListRow newRow = MSEGroupedPeakList.copyPeakRow(selRow);
      // set charge and isotope pattern
      for (Feature f : newRow.getPeaks()) {
        f.setIsotopePattern(newPattern);
        f.setCharge(bestFitCharge);
      }
      deisotopedPeakList.addRow(newRow);

      // Remove all peaks already assigned to isotope pattern
      for (int i = maxRow; i < rows.length; i++) {
        if (bestFitRows.contains(rows[i]))
          rows[i] = null;
      }

      // Update completion rate
      processedPeaks++;
    }
    //
    logger.info("allRows\tdeiso\tnoCharge\trawFilled\tsumCharged\tdirect");
    logger.info(peakList.getNumberOfRows() + "\t\t" + deisotopedPeakList.getNumberOfRows() + "\t"
        + totalNoCharge + "\t\t" + rawFilledIn + "\t\t" + (foundIsotopeRow + rawFilledIn) + "\t\t"
        + foundIsotopeRow);

    // Add new peakList to the project
    project.addPeakList(deisotopedPeakList);

    // Load previous applied methods
    for (PeakListAppliedMethod proc : peakList.getAppliedMethods()) {
      deisotopedPeakList.addDescriptionOfAppliedTask(proc);
    }

    // Add task description to peakList
    deisotopedPeakList.addDescriptionOfAppliedTask(
        new SimplePeakListAppliedMethod("Aligned isotopic peaks grouper", parameters));

    // Remove the original peakList if requested
    if (removeOriginal)
      project.removePeakList(peakList);

    logger.info("Finished aligned isotopic peak grouper on " + peakList);
    setStatus(TaskStatus.FINISHED);

  }

  private Vector<DataPoint> bestRawIsoPattern = null;

  /**
   * try to find isotope peak in highest raw files highest data point +- FWHM/2
   * 
   * @param rawDataFiles
   * @param copy
   * @return
   */
  private int findChargeStateInRaw(RawDataFile[] rawFiles, PeakListRow row) {
    // TODO Auto-generated method stub
    RawDataFile raw = null;
    Feature f = null;
    double height = 0;
    for (RawDataFile r : rawFiles) {
      Feature p = row.getPeak(r);
      if (p != null && (raw == null || height < p.getHeight())) {
        raw = r;
        f = p;
        height = f.getHeight();
      }
    }
    // starting peak as datapoint
    DataPoint dp = new SimpleDataPoint(f.getMZ(), height);
    // range of FWHM and rt of highest point
    int[] sn = f.getScanNumbers();
    double maxH = Double.NEGATIVE_INFINITY;
    int snAtMaxH = 0;
    for (int i = 0; i < sn.length; i++) {
      double h = f.getDataPoint(sn[i]).getIntensity();
      if (maxH < h) {
        maxH = h;
        snAtMaxH = sn[i];
      }
    }
    double rtAtMaxH = raw.getScan(snAtMaxH).getRetentionTime();
    // count scans
    int c = 0;
    RTTolerance newTol = f.getFWHM() != null ? new RTTolerance(true, f.getFWHM() / 2) : rtTolerance;
    for (int i = 0; i < sn.length; i++) {
      Scan scan = raw.getScan(sn[i]);
      if (newTol.checkWithinTolerance(rtAtMaxH, scan.getRetentionTime())) {
        c++;
      }
    }
    // create scans array
    int[] scans = new int[c];
    c = 0;
    for (int i = 0; i < sn.length; i++) {
      Scan scan = raw.getScan(sn[i]);
      if (newTol.checkWithinTolerance(rtAtMaxH, scan.getRetentionTime())) {
        scans[c] = sn[i];
        c++;
      }
    }

    // search for best isotope pattern
    // Check which charge state fits best around this peak
    int bestFitCharge = 0;
    int bestFitScore = -1;
    bestRawIsoPattern = null;
    for (int charge = 1; charge <= maximumCharge; charge++) {

      Vector<DataPoint> fittedDP = new Vector<DataPoint>();
      fittedDP.add(dp);
      findRawPattern(raw, scans, fittedDP, dp, charge);

      int score = fittedDP.size() * 1;
      // monotonic?
      if (monotonicShape && fittedDP.size() > 1) {
        boolean isMono = true;
        int add = 0;
        for (int i = 0; i < fittedDP.size() - 1; i++) {
          if (fittedDP.get(i).getIntensity() < fittedDP.get(i + 1).getIntensity()) {
            isMono = false;
            add += -1;
          } else
            add += 1;
        }
        score = add;
      }
      if ((score > bestFitScore) || ((score == bestFitScore) && (bestFitCharge > charge))) {
        bestFitScore = score;
        bestFitCharge = charge;
        bestRawIsoPattern = fittedDP;
      }

    }
    return bestRawIsoPattern != null && bestRawIsoPattern.size() > 1 ? bestFitCharge : 0;
  }

  /**
   * find pattern in raw file with charge only keeps searching if the matched data points are
   * greater than minMatchRawDP
   * 
   * @param raw
   * @param scanNumbers
   * @param fittedDP
   * @param dp
   * @param charge
   * @return
   */
  private int findRawPattern(RawDataFile raw, int[] scanNumbers, Vector<DataPoint> fittedDP,
      DataPoint dp, int charge) {
    // start with isotope 1
    int n = 0;
    int totalMatches = 0;
    // no match?
    DataPoint dpMaxHeight = null;
    do {
      // next isotope
      n++;
      dpMaxHeight = null;
      // mass
      double isotopeMZ = dp.getMZ() + isotopeDistance * n / charge;

      double maxHeight = Double.NEGATIVE_INFINITY;
      // count how many scans contain a matching mass
      int count = 0;
      for (int s = 0; s < scanNumbers.length; s++) {
        Scan scan = raw.getScan(scanNumbers[s]);
        MassList mass = scan.getMassList(massList);
        if (mass != null) {
          boolean match = false;
          DataPoint[] allDP = mass.getDataPoints();
          for (int i = 0; i < allDP.length; i++) {
            // in mz tolerance?
            if ((!monotonicShape || allDP[i].getIntensity() < dp.getIntensity())
                && mzTolerance.checkWithinTolerance(allDP[i].getMZ(), isotopeMZ)) {
              if (maxHeight < allDP[i].getIntensity()) {
                maxHeight = allDP[i].getIntensity();
                dpMaxHeight = allDP[i];
              }
              match = true;
            }
          }
          if (match)
            count++;
        }
      }
      if (dpMaxHeight != null && count >= minMatchRawDP) {
        fittedDP.add(dpMaxHeight);
        totalMatches = count;
      }
    } while (dpMaxHeight != null);
    return totalMatches;
  }

  /**
   * searches for the maximum row in distance avoiding bad mass detection and shoulder peaks
   * 
   * @param rawFiles
   * @param rows
   * @param r
   * @return
   */
  private int searchForMaxRowInDist(RawDataFile[] rawFiles, PeakListRow[] rows, int r) {
    int c = 0;
    double maxH = Double.NEGATIVE_INFINITY;
    for (RawDataFile raw : rawFiles) {
      Feature p = rows[r].getPeak(raw);
      if (p != null) {
        c++;
        if (maxH < p.getHeight())
          maxH = p.getHeight();
      }
    }
    return searchForMaxRowInDist(rawFiles, rows, r + 1, r, c, maxH);
  }

  /**
   * gets called by other search method
   * 
   * @param rawFiles
   * @param rows
   * @param maxRow
   * @param detections
   * @return
   */
  private int searchForMaxRowInDist(RawDataFile[] rawFiles, PeakListRow[] rows, int r, int maxRow,
      int detections, double maxH) {
    // escape
    if (maxRow + 1 >= rows.length)
      return maxRow;
    // next row if rows[r] == null
    if (rows[r] == null)
      return searchForMaxRowInDist(rawFiles, rows, r + 1, maxRow, detections, maxH);

    // check alternativeRow
    PeakListRow row = rows[maxRow];
    PeakListRow altRow = rows[r];
    // count detected peaks (hasPeak)
    int altC = 0;
    double altMaxH = Double.NEGATIVE_INFINITY;
    for (RawDataFile raw : rawFiles) {
      Feature p = row.getPeak(raw);
      Feature altP = altRow.getPeak(raw);
      if (altP != null)
        altC++;
      if (p != null && altP != null) {
        double mainMZ = p.getMZ();
        double altMZ = altP.getMZ();
        double mainRT = p.getRT();
        double altRT = altP.getRT();
        double altHeight = altP.getHeight();
        // escape if mz is already to high (save runtime)
        // END OF SEARCH
        if (!mzTolerance.checkWithinTolerance(mainMZ, altMZ))
          return maxRow;
        // next row[r] if not in RT tolerance
        else if (!rtTolerance.checkWithinTolerance(mainRT, altRT))
          return searchForMaxRowInDist(rawFiles, rows, r + 1, maxRow, detections, maxH);
        else {
          if (altMaxH < altHeight) {
            altMaxH = altHeight;
          }
        }
      }
    }
    // higher?
    if (maxH < altMaxH && detections <= altC)
      return searchForMaxRowInDist(rawFiles, rows, r + 1, r, altC, altMaxH);
    else
      return searchForMaxRowInDist(rawFiles, rows, r + 1, maxRow, detections, maxH);
  }

  /**
   * Fits isotope pattern around one peak.
   * 
   * @param p Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   */
  private void fitPattern(RawDataFile[] rawFiles, Vector<PeakListRow> fittedRows, int rowI,
      int charge, PeakListRow[] sortedRows) {
    if (charge == 0)
      return;

    PeakListRow row = sortedRows[rowI];
    for (RawDataFile raw : rawFiles) {
      Feature p = row.getPeak(raw);
      if (p != null) {
        Vector<PeakListRow> tmpFitted = new Vector<PeakListRow>();
        tmpFitted.add(row);

        // Search for peaks after the start peak
        findPattern(raw, p, charge, tmpFitted, sortedRows, rowI);

        // add all new rows
        for (PeakListRow r : tmpFitted)
          addMaxToList(fittedRows, r);
      }
    }
  }

  /**
   * adds the row if there is no other isotope peak (same m/z) or if the intensity is higher
   * 
   * @param list
   * @param row
   * @return
   */
  private boolean addMaxToList(Vector<PeakListRow> list, PeakListRow row) {
    for (int i = 0; i < list.size(); i++) {
      if (mzTolerance.checkWithinTolerance(list.get(i).getAverageMZ(), row.getAverageMZ())) {
        if (list.get(i).getAverageArea() < row.getAverageHeight()) {
          list.remove(i);
          list.add(row);
          return true;
        } else
          return false;
      }
    }
    list.add(row);
    return false;
  }

  /**
   * Helper method for fitPattern.
   * 
   * @param p Pattern is fitted around this peak
   * @param charge Charge state of the fitted pattern
   * @param tmpFitted Defines which half to fit: -1=fit to peaks before start M/Z, +1=fit to peaks
   *        after start M/Z
   * @param rows All matching peaks will be added to this set
   */
  private void findPattern(RawDataFile raw, Feature p, int charge, Vector<PeakListRow> tmpFitted,
      PeakListRow[] rows, int rowI) {

    // Use M/Z and RT of the strongest peak of the pattern (peak 'p')
    double mainMZ = p.getMZ();
    double mainRT = p.getRT();

    // Variable n is the number of peak we are currently searching. 1=first
    // peak after start peak, 2=peak after previous, 3=...
    PeakListRow bestCandidateRow = null;
    int n = 0;
    // row to start with (sorted by mz)
    int ind = rowI + 1;
    do {
      // Assume we don't find match for n:th peak in the pattern (which
      // will end the loop)
      bestCandidateRow = null;
      // next isotope
      n++;

      // only add best candidate per loop
      double maxHeight = Double.NEGATIVE_INFINITY;

      // Loop through all peaks, and collect candidates for the n:th peak
      // in the pattern
      for (; ind < rows.length; ind++) {

        PeakListRow candidateRow = rows[ind];
        if (candidateRow == null)
          continue;

        Feature candidatePeak = candidateRow.getPeak(raw);

        if (candidatePeak == null)
          continue;

        // Get properties of the candidate peak
        double candidatePeakMZ = candidatePeak.getMZ();
        double candidatePeakRT = candidatePeak.getRT();

        // Does this peak fill all requirements of a candidate?
        // - within tolerances from the expected location (M/Z and RT)
        // - not already a fitted peak (only necessary to avoid
        // conflicts when parameters are set too wide)
        double isotopeMZ = candidatePeakMZ - isotopeDistance * n / charge;

        if (mzTolerance.checkWithinTolerance(isotopeMZ, mainMZ)
            && rtTolerance.checkWithinTolerance(candidatePeakRT, mainRT)) {
          // not monotonic? do not add but keep searching for one
          if (monotonicShape && candidatePeak.getHeight() > p.getHeight()) {
            // do nothing
          }
          // highest candidate?
          else if (maxHeight < candidatePeak.getHeight()) {
            bestCandidateRow = candidateRow;
            maxHeight = candidatePeak.getHeight();
          }
        }

        // escape if mz is already to high (save runtime)
        // END OF SEARCH
        if (mzTolerance.getToleranceRange(mainMZ).upperEndpoint().doubleValue() < isotopeMZ)
          break;
      }

      if (bestCandidateRow != null) {
        tmpFitted.add(bestCandidateRow);
      }
    } while (bestCandidateRow != null);
  }


  /**
   * compares 2 rows for being isotopes
   * 
   * @param peakList
   * @param row
   * @param row2
   * @return absolute charge (1-maxCharge) or -1 if one row is not an isotope of the other row
   */
  public static int find13CIsotope(PeakList peakList, PeakListRow row1, PeakListRow row2,
      int maximumCharge, MZTolerance mzTolerance) {
    ESIAdductType iso = ESIAdductType.C13;
    // for all possible charge states
    for (int z = maximumCharge; z >= 1; z--) {
      // checks each raw file - only true if all m/z are in range
      if (checkIsotope(peakList, row1, row2, iso, z, mzTolerance)) {
        // Add adduct identity and notify GUI.
        // only if not already present
        if (row2.getAverageMZ() < row1.getAverageMZ()) {
          iso.addAdductIdentityToRow(row1, row2);
          MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row1, false);
        } else {
          iso.addAdductIdentityToRow(row2, row1);
          MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row2, false);
        }
        // there can only be one hit for a row-row comparison
        return z;
      }
    }
    return -1;
  }

  private static boolean checkIsotope(final PeakList peakList, final PeakListRow row1,
      final PeakListRow row2, final ESIAdductType iso, int charge, MZTolerance mzTolerance) {
    // for each peak[rawfile] in row
    boolean hasCommonPeak = false;
    //
    for (RawDataFile raw : peakList.getRawDataFiles()) {
      Feature f1 = row1.getPeak(raw);
      Feature f2 = row2.getPeak(raw);
      if (f1 != null && f2 != null) {
        hasCommonPeak = true;
        double mz1 = (f1.getMZ() * charge);
        double mz2 = (f2.getMZ() * charge);
        mz1 = mz1 + (iso.getMassDifference() * (mz1 < mz2 ? 1 : -1));
        if (!mzTolerance.checkWithinTolerance(mz1, mz2))
          return false;
      }
    }
    // directly returns false if not in range
    // so if has common peak = isAdduct
    return hasCommonPeak;
  }

}
