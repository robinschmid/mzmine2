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

package net.sf.mzmine.modules.peaklistmethods.identification.carbonblack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopeprediction.IsotopePatternCalculator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.DataPointSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.scans.similarity.HandleUnmatchedSignalOptions;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarity;
import net.sf.mzmine.util.scans.similarity.Weights;
import net.sf.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarity;
import net.sf.mzmine.util.scans.similarity.impl.cosine.WeightedCosineSpectralSimilarityParameters;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

class CarbonBlackTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private static final double ELECTRON_MASS = 0.00054858;
  private static final double C13 = 1.0033548;

  private PeakList peakList;

  private AtomicInteger finished = new AtomicInteger(0);
  private int total = 0;
  private MZTolerance mzTolerance;
  private ParameterSet parameters;
  private int minConsecutive;
  private final double ionDiff;
  private final double minHeight;
  private final double minCosine;
  private String masses;
  private PolarityType polarity;
  private WeightedCosineSpectralSimilarityParameters param;
  private WeightedCosineSpectralSimilarity simFunc;
  private IsotopePattern[] patternCalculated;

  CarbonBlackTask(PeakList peakList, ParameterSet parameters) {
    this.peakList = peakList;
    this.parameters = parameters;
    total = peakList.getRows().length;

    minConsecutive = parameters.getParameter(CarbonBlackParameters.minConsecutive).getValue();
    mzTolerance = parameters.getParameter(CarbonBlackParameters.mzTolerance).getValue();
    masses = parameters.getParameter(CarbonBlackParameters.masses).getValue();
    polarity = parameters.getParameter(CarbonBlackParameters.polarity).getValue();
    minHeight = parameters.getParameter(CarbonBlackParameters.minHeight).getValue();
    minCosine = parameters.getParameter(CarbonBlackParameters.minCosine).getValue();


    param = new WeightedCosineSpectralSimilarityParameters();
    param.getParameter(WeightedCosineSpectralSimilarityParameters.handleUnmatched)
        .setValue(HandleUnmatchedSignalOptions.REMOVE_ALL);
    param.getParameter(WeightedCosineSpectralSimilarityParameters.weight).setValue(Weights.NONE);
    param.getParameter(WeightedCosineSpectralSimilarityParameters.minCosine).setValue(minCosine);

    simFunc = new WeightedCosineSpectralSimilarity();

    switch (polarity) {
      case NEGATIVE:
        ionDiff = ELECTRON_MASS;
        break;
      case POSITIVE:
        ionDiff = -ELECTRON_MASS;
        break;
      case UNKNOWN:
      case NEUTRAL:
      default:
        ionDiff = 0d;
        break;
    }
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (total == 0)
      return 0;
    return ((double) finished.get()) / total;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "Searching for carbon black signals in " + peakList;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // caclulate isotope pattern
    logger.log(Level.INFO, "Building theoretical C pattern");
    patternCalculated = new IsotopePattern[100];
    for (int i = 1; i < 100; i++)
      patternCalculated[i] = IsotopePatternCalculator.calculateIsotopePattern("C" + i, 0.00001,
          0.01, 1, polarity, false);

    peakList.parallelStream().forEach(row -> {
      if (getStatus().equals(TaskStatus.PROCESSING)) {
        MassList scan = row.getBestPeak().getRepresentativeScan().getMassList(masses);
        if (scan == null) {
          logger.log(Level.WARNING, "No mass list with name " + masses);
          setErrorMessage("No mass list with name " + masses);
          setStatus(TaskStatus.ERROR);
          return;
        }
        DataPoint[] dps = scan.getDataPoints();
        DataPointSorter sorter =
            new DataPointSorter(SortingProperty.MZ, SortingDirection.Ascending);
        Arrays.sort(dps, sorter);

        // try to find carbon signals in mass list
        // C0 to C100
        DataPoint[] carbon = new DataPoint[100];

        for (DataPoint dp : dps) {
          int c = Math.round((float) dp.getMZ() / 12.f);
          if (c < carbon.length
              && mzTolerance.checkWithinTolerance(dp.getMZ(), c * 12.d + ionDiff)) {
            if (carbon[c] == null || carbon[c].getIntensity() < dp.getIntensity())
              carbon[c] = dp;
          }
        }

        List<List<DataPoint>> isotopes = new ArrayList<>();
        for (int i = 1; i < carbon.length; i++) {
          if (carbon[i] != null) {
            IsotopePattern pattern = patternCalculated[i];
            List<DataPoint> cpattern = new ArrayList<>();
            // main signal
            cpattern.add(carbon[i]);
            for (int p = 1; p < pattern.getDataPoints().length; p++) {
              // calculated intensity relative to measured monoisotopic mz
              DataPoint pdp = pattern.getDataPoints()[p];
              double intensity = carbon[i].getIntensity() * pdp.getIntensity();
              if (intensity >= minHeight)
                cpattern.add(new SimpleDataPoint(pdp.getMZ(), intensity));
            }
            // min 2 signals
            // if (cpattern.size() > 1)
            isotopes.add(cpattern);
          }
        }
        int maxConsecutive = 0;
        DataPoint[] library = isotopes.stream().flatMap(List::stream).toArray(DataPoint[]::new);

        List<DataPoint> tmp = new ArrayList<>();
        for (List<DataPoint> list : isotopes) {
          for (int k = 1; k < list.size(); k++) {
            tmp.add(list.get(k));
          }
        }
        DataPoint[] libraryOnlyIsotopes = tmp.toArray(new DataPoint[tmp.size()]);

        SpectralSimilarity sim =
            simFunc.getSimilarity(param, mzTolerance, 3, libraryOnlyIsotopes, dps);
        if (sim != null) {
          String name = "";
          int first = -1;
          for (int i = 1; i < carbon.length; i++) {
            if (first == -1 && carbon[i] != null) {
              first = i;
            }
            if (first != -1 && carbon[i] == null) {
              if (i - first > 1) {
                name += "C" + first + "-" + "C" + (i - 1) + " ";
                maxConsecutive = i - first;
              } else
                name += "C" + first + " ";
              first = -1;
            }
          }

          // enough consecutive?
          if (minConsecutive <= maxConsecutive) {
            name = "Carbon Black:" + name;
            Map<DBEntryField, Object> fields = new HashMap<>();
            fields.put(DBEntryField.NAME, name);
            fields.put(DBEntryField.COMMENT,
                "Carbon Black without main signals (only isotopes); " + name);
            SpectralDBEntry entry = new SpectralDBEntry(fields, libraryOnlyIsotopes);

            SpectralDBPeakIdentity id = new SpectralDBPeakIdentity(
                row.getBestPeak().getRepresentativeScan(), masses, entry, sim, "Carbon Black ID");
            addIdentity(row, id);


            // add with main signals
            SpectralSimilarity sim2 = simFunc.getSimilarity(param, mzTolerance, 3, library, dps);

            fields = new HashMap<>();
            fields.put(DBEntryField.NAME, name);
            fields.put(DBEntryField.COMMENT, "Carbon Black all signals; " + name);
            entry = new SpectralDBEntry(fields, library);

            id = new SpectralDBPeakIdentity(row.getBestPeak().getRepresentativeScan(), masses,
                entry, sim2, "Carbon Black ID");
            addIdentity(row, id);
          }
        }

        finished.getAndIncrement();
      }
    });

    logger.log(Level.INFO, "Finished Carbon Black identification");
    setStatus(TaskStatus.FINISHED);
  }

  private void addIdentity(PeakListRow row, SpectralDBPeakIdentity id) {
    // add new identity to the row
    row.addPeakIdentity(id, false);
    if (row.getComment().isEmpty())
      row.setComment("Carbon Black");
    else
      row.setComment(row.getComment() + ";");

    // Notify the GUI about the change in the project
    MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
  }
}
