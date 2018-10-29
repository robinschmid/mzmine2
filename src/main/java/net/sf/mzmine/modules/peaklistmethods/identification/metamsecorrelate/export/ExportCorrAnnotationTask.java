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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.export;

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateModule;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.io.TxtWriter;

public class ExportCorrAnnotationTask extends AbstractTask {
  // Logger.
  private static final Logger LOG = Logger.getLogger(ExportCorrAnnotationTask.class.getName());

  public enum EDGES {
    ID1, ID2, EdgeType, Score, Annotation;
  }

  private Double progress = 0d;


  /**
   * by {@link MetaMSEcorrelateModule}
   */
  private double minR;
  private final PeakList peakList;
  private File filename;
  private boolean exportOnlyAnnotated;


  private boolean limitToMSMS;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public ExportCorrAnnotationTask(final ParameterSet parameterSet, final PeakList peakLists) {
    this.peakList = peakLists;

    // tolerances
    filename = parameterSet.getParameter(ExportCorrAnnotationParameters.FILENAME).getValue();
    minR = parameterSet.getParameter(ExportCorrAnnotationParameters.MIN_R).getValue();
    exportOnlyAnnotated =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_ONLY_ANNOTATED).getValue();
  }

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public ExportCorrAnnotationTask(PeakList peakList, File filename, double minR,
      boolean exportOnlyAnnotated, boolean limitToMSMS) {
    this.peakList = peakList;
    this.filename = filename;
    this.minR = minR;
    this.exportOnlyAnnotated = exportOnlyAnnotated;
    this.limitToMSMS = limitToMSMS;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  @Override
  public String getTaskDescription() {
    return "Export adducts and correlation networks " + peakList.getName() + " ";
  }

  @Override
  public void run() {
    try {
      setStatus(TaskStatus.PROCESSING);
      LOG.info("Starting export of adduct and correlation networks" + peakList.getName());

      // export edges of corr
      exportAnnotationEdges(peakList, filename, limitToMSMS, progress, this);
      // export edges of ann
      exportCorrelationEdges(peakList, filename, progress, this, minR, exportOnlyAnnotated,
          limitToMSMS);

    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of correlation and MS annotation results error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }

    setStatus(TaskStatus.FINISHED);
  }


  public static boolean exportAnnotationEdges(PeakList pkl, File filename, boolean limitToMSMS,
      Double progress, AbstractTask task) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat corrForm = new DecimalFormat("0.000");
    try {
      PeakListRow[] rows = pkl.getRows();
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);
      // for all rows
      for (PeakListRow r : rows) {

        if (limitToMSMS && r.getBestFragmentation() == null)
          continue;

        if (task != null && task.isCanceled()) {
          return false;
        }
        // row1
        int rowID = r.getID();

        //
        MSAnnotationNetworkLogic.getAllAnnotations(r).stream().forEach(adduct -> {
          ConcurrentHashMap<PeakListRow, ESIAdductIdentity> links = adduct.getPartner();

          // add all connection for ids>rowID
          links.entrySet().stream().filter(e -> e != null).filter(e -> e.getKey().getID() > rowID)
              .forEach(e -> {
                PeakListRow link = e.getKey();
                ESIAdductIdentity id = e.getValue();
                double dmz = Math.abs(r.getAverageMZ() - link.getAverageMZ());
                // the data
                exportEdge(ann, "MS1 annotation", rowID, e.getKey().getID(),
                    corrForm.format((id.getScore() + adduct.getScore()) / 2d), //
                    id.getAdduct() + " " + adduct.getAdduct() + " dm/z=" + mzForm.format(dmz));
                added.incrementAndGet();
              });
        });
      }

      LOG.info("Annotation edges exported " + added.get() + "");

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_msannotation", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  public static boolean exportCorrelationEdges(PeakList pkl, File filename, Double progress,
      AbstractTask task, double minCorr, boolean onlyAnnotated, boolean limitToMSMS) {
    if (!(pkl instanceof MSEGroupedPeakList))
      return false;

    R2RCorrMap map = ((MSEGroupedPeakList) pkl).getCorrelationMap();

    NumberFormat corrForm = new DecimalFormat("0.000");
    try {
      StringBuilder ann = new StringBuilder();
      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);
      // for all rows
      map.streamCorrDataEntries().filter(e -> e.getValue().getAvgPeakShapeR() >= minCorr)
          .forEach(e -> {
            int[] ids = R2RCorrMap.toKeyIDs(e.getKey());
            //
            boolean export = true;
            if (limitToMSMS || onlyAnnotated) {
              PeakListRow a = pkl.findRowByID(ids[0]);
              PeakListRow b = pkl.findRowByID(ids[1]);
              // only export rows with MSMS
              if (limitToMSMS)
                export = a.getBestFragmentation() != null && b.getBestFragmentation() != null;
              if (export && onlyAnnotated) {
                // find annotations
                export = MSAnnotationNetworkLogic.hasIonAnnotation(a)
                    && MSAnnotationNetworkLogic.hasIonAnnotation(b);
              }
            }

            //
            if (export) {
              exportEdge(ann, "MS1 shape correlation", ids[0], ids[1],
                  corrForm.format(e.getValue().getAvgPeakShapeR()),
                  "r=" + corrForm.format(e.getValue().getAvgPeakShapeR()));
              added.incrementAndGet();
            }
          });

      LOG.info("Correlation edges exported " + added.get() + "");

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_ms1correlation", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  private static void writeToFile(String data, File filename, String suffix, String format) {
    TxtWriter writer = new TxtWriter();
    File realFile = FileAndPathUtil.eraseFormat(filename);
    realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
        realFile.getName() + suffix, format);
    writer.openNewFileOutput(realFile);
    writer.write(data);
    writer.closeDatOutput();
    LOG.info("File created: " + realFile);
  }

  private static void exportEdge(StringBuilder ann, String type, int id1, int id2, String score,
      String annotation) {
    // the data
    Object[] data = new Object[EDGES.values().length];

    // add all data
    for (int d = 0; d < EDGES.values().length; d++) {
      switch (EDGES.values()[d]) {
        case ID1:
          data[d] = id1 + "";
          break;
        case ID2:
          data[d] = id2 + "";
          break;
        case EdgeType:
          data[d] = type;
          break;
        case Annotation:
          data[d] = annotation;
          break;
        case Score:
          data[d] = score;
          break;
      }
    }
    // replace null
    for (int j = 0; j < data.length; j++) {
      if (data[j] == null)
        data[j] = "";
    }
    // add data
    ann.append(StringUtils.join(data, ','));
    ann.append("\n");
  }

}
