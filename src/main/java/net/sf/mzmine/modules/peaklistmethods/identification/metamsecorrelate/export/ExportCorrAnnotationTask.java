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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateModule;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupPeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationModule;
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

  public enum ANNOTATION {
    ID1, ID2, ANNOTATION1, ANNOTATION2, ANNO_NETID, MZ1, MZ2, DELTA_MZ, DELTA_AVG_RT, RT1, RT2, INTENSITY1, INTENSITY2, AREA1, AREA2, CORR_GROUP, AVG_CORR, AVG_DP, CORRELATED_F2F, TOTAL_CORR, IMAX_CORR;
    private String header;

    ANNOTATION() {
      this.header = toString().replaceAll("_", " ");
    }

    ANNOTATION(String header) {
      this.header = header;
    }

    @Override
    public String toString() {
      return header != null ? header : super.toString();
    }
  }

  public enum NODES {
    ID, ANNOTATION, ANNO_NETID, MZ, RT, INTENSITY, AREA, CORR_GROUP;
    private String header;

    NODES() {
      this.header = toString().replaceAll("_", " ");
    }

    NODES(String header) {
      this.header = header;
    }

    @Override
    public String toString() {
      return header != null ? header : super.toString();
    }
  }

  private Double progress = 0d;

  private final PeakList peakList;


  private final ParameterSet parameters;

  private File filename;

  /**
   * {@link MetaMSEcorrelateModule} or {@link MSAnnotationModule}
   */
  private boolean exAnnotationsFile;
  /**
   * by {@link MetaMSEcorrelateModule}
   */
  private boolean exAvgCorrFile;

  private boolean exMZ, exDMZ, exDRT, exRT, exI, exArea;

  private double minR;

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public ExportCorrAnnotationTask(final MZmineProject project, final ParameterSet parameterSet,
      final PeakList peakLists) {
    this.peakList = peakLists;
    parameters = parameterSet;

    // tolerances
    filename = parameterSet.getParameter(ExportCorrAnnotationParameters.FILENAME).getValue();
    exAnnotationsFile =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_ANNOTATIONS_FILE).getValue();
    exAvgCorrFile =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_AVGCORR_FILE).getValue();
    exMZ = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_MZ).getValue();
    exDMZ = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_DMZ).getValue();
    exRT = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_RT).getValue();
    exDRT = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_DRT).getValue();
    exI = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_I).getValue();
    exArea = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_AREA).getValue();
    minR = parameterSet.getParameter(ExportCorrAnnotationParameters.MIN_AVGCORR).getValue();
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

      // work
      MSEGroupedPeakList pkl = (MSEGroupedPeakList) peakList;
      R2RCorrMap corrMap = pkl.getCorrelationMap();

      // export annotation networks to file
      if (exAnnotationsFile) {
        // edges file
        ANNOTATION[] ann = createColumns(exMZ, exDMZ, exDRT, exRT, exI, exArea, false);
        exportAnnotationsToCSV(pkl, corrMap, filename, progress, ann, this);

        // nodes file
        NODES[] ann2 = NODES.values();
        exportAnnotationNodesToCSV(pkl, corrMap, filename, progress, ann2, this);
      }
      // export r2rcorr map to file
      if (exAvgCorrFile) {
        ANNOTATION[] ann = createColumns(exMZ, exDMZ, exDRT, exRT, exI, exArea, true);
        exportCorrelationMapToCSV(pkl, corrMap, minR, filename, progress, ann, this);
      }
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of correlation and MS annotation results error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Creates columns with filter
   * 
   * @param exMZ
   * @param exDMZ
   * @param exDRT
   * @param exRT
   * @param exI
   * @param exArea
   * @param exCorr
   * @return
   */
  public static ANNOTATION[] createColumns(boolean exMZ, boolean exDMZ, boolean exDRT, boolean exRT,
      boolean exI, boolean exArea, boolean exCorr) {
    List<ANNOTATION> ann = new ArrayList<>();
    // add all
    ann.add(ANNOTATION.ID1);
    ann.add(ANNOTATION.ID2);
    ann.add(ANNOTATION.ANNOTATION1);
    ann.add(ANNOTATION.ANNOTATION2);
    ann.add(ANNOTATION.ANNO_NETID);
    if (exMZ) {
      ann.add(ANNOTATION.MZ1);
      ann.add(ANNOTATION.MZ2);
    }
    if (exDMZ) {
      ann.add(ANNOTATION.DELTA_MZ);
    }
    if (exDRT) {
      ann.add(ANNOTATION.DELTA_AVG_RT);
    }
    if (exRT) {
      ann.add(ANNOTATION.RT1);
      ann.add(ANNOTATION.RT2);
    }
    if (exI) {
      ann.add(ANNOTATION.INTENSITY1);
      ann.add(ANNOTATION.INTENSITY2);
    }
    if (exArea) {
      ann.add(ANNOTATION.AREA1);
      ann.add(ANNOTATION.AREA2);
    }

    ann.add(ANNOTATION.CORR_GROUP);
    ann.add(ANNOTATION.AVG_CORR);

    if (exCorr) {
      ann.add(ANNOTATION.AVG_DP);
      ann.add(ANNOTATION.CORRELATED_F2F);
      ann.add(ANNOTATION.TOTAL_CORR);
      ann.add(ANNOTATION.IMAX_CORR);
    }
    return ann.toArray(new ANNOTATION[ann.size()]);
  }


  /**
   * Exports all PeakListRows with their favored annotation
   * 
   * @param pkl
   * @param corrMap can be null
   * @param filename
   * @param progress
   * @param col
   * @param task can be null
   * @return
   */
  public static boolean exportAnnotationNodesToCSV(MSEGroupedPeakList pkl, R2RCorrMap corrMap,
      File filename, Double progress, NODES[] col, AbstractTask task) {
    try {
      PeakListRow[] rows = pkl.getRows();
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
      int totalRows = rows.length;

      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(col, ','));
      ann.append("\n");

      int added = 0;
      // for all rows
      for (PeakListRow r : rows) {
        if (task != null && task.isCanceled()) {
          return false;
        }

        int rowID = r.getID();

        PeakIdentity pi = r.getPreferredPeakIdentity();
        // identity by ms annotation module
        if (pi instanceof ESIAdductIdentity) {
          ESIAdductIdentity id = (ESIAdductIdentity) pi;

          // the data
          Object[] data = new Object[col.length];

          // add all data
          for (int d = 0; d < col.length; d++) {
            switch (col[d]) {
              case ANNOTATION:
                data[d] = id != null ? id.getAdduct() : null;
                break;
              case ANNO_NETID:
                data[d] = id != null ? id.getNetID() : null;
                break;
              case ID:
                data[d] = r.getID();
                break;
              case MZ:
                data[d] = r.getAverageMZ();
                break;
              case AREA:
                data[d] = r.getBestPeak().getArea();
                break;
              case INTENSITY:
                data[d] = r.getBestPeak().getHeight();
                break;
              case RT:
                data[d] = r.getAverageRT();
                break;
              case CORR_GROUP:
                MSEGroupPeakIdentity gid = MSEGroupPeakIdentity.getIdentityOf(r);
                if (gid != null)
                  data[d] = gid.getGroup().getGroupID();
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
          added++;
        }
        progress += 1.0 / totalRows;
      }
      LOG.info("ALl data created for export. Total of " + added + " adduct nodes");

      // export ann
      // Filename
      TxtWriter writer = new TxtWriter();
      File realFile = FileAndPathUtil.eraseFormat(filename);
      realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
          realFile.getName() + "_annotation_nodes", ".csv");
      writer.openNewFileOutput(realFile);
      writer.write(ann.toString());
      writer.closeDatOutput();
      LOG.info("File created: " + realFile);
      return true;
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of MS annotation nodes results error", t);
      if (task != null) {
        task.setStatus(TaskStatus.ERROR);
        task.setErrorMessage(t.getMessage());
      }
      return false;
    }
  }

  /**
   * Creates Annotations edge file with all annotation links in this file
   * 
   * @param pkl
   * @param corrMap can be null
   * @param filename
   * @param progress
   * @param col
   * @param task can be null
   * @return
   */
  public static boolean exportAnnotationsToCSV(MSEGroupedPeakList pkl, R2RCorrMap corrMap,
      File filename, Double progress, ANNOTATION[] col, AbstractTask task) {
    try {
      PeakListRow[] rows = pkl.getRows();
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
      int totalRows = rows.length;

      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(col, ','));
      ann.append("\n");

      int added = 0;
      // for all rows
      for (PeakListRow r : rows) {
        if (task != null && task.isCanceled()) {
          return false;
        }

        int rowID = r.getID();
        for (PeakIdentity pi : r.getPeakIdentities()) {
          // identity by ms annotation module
          if (pi instanceof ESIAdductIdentity) {
            ESIAdductIdentity adduct = (ESIAdductIdentity) pi;
            int[] ids = adduct.getPartnerRowsID();

            // add all connection for ids>rowID
            for (int id2 : ids) {
              if (id2 > rowID) {
                PeakListRow link = pkl.findRowByID(id2);
                if (link != null) {
                  // the data
                  Object[] data = new Object[col.length];
                  ESIAdductIdentity id = null;
                  // r2r correlation
                  R2RCorrelationData r2r = corrMap != null ? corrMap.get(r, link) : null;

                  // add all data
                  for (int d = 0; d < col.length; d++) {
                    switch (col[d]) {
                      case ANNOTATION1:
                        id = ESIAdductIdentity.getIdentityOf(r, link);
                        data[d] = id != null ? id.getAdduct() : null;
                        break;
                      case ANNOTATION2:
                        id = ESIAdductIdentity.getIdentityOf(link, r);
                        data[d] = id != null ? id.getAdduct() : null;
                        break;
                      case ANNO_NETID:
                        id = ESIAdductIdentity.getIdentityOf(r, link);
                        data[d] = id != null ? id.getNetID() : null;
                        break;
                      case DELTA_MZ:
                        data[d] = link.getAverageMZ() - r.getAverageMZ();
                        break;
                      case DELTA_AVG_RT:
                        data[d] = link.getAverageRT() - r.getAverageRT();
                        break;
                      case ID1:
                        data[d] = r.getID();
                        break;
                      case ID2:
                        data[d] = link.getID();
                        break;
                      case MZ1:
                        data[d] = r.getAverageMZ();
                        break;
                      case MZ2:
                        data[d] = link.getAverageMZ();
                        break;
                      case AREA1:
                        data[d] = r.getBestPeak().getArea();
                        break;
                      case AREA2:
                        data[d] = link.getBestPeak().getArea();
                        break;
                      case INTENSITY1:
                        data[d] = r.getBestPeak().getHeight();
                        break;
                      case INTENSITY2:
                        data[d] = link.getBestPeak().getHeight();
                        break;
                      case RT1:
                        data[d] = r.getAverageRT();
                        break;
                      case RT2:
                        data[d] = link.getAverageRT();
                        break;
                      case CORR_GROUP:
                        MSEGroupPeakIdentity gid = MSEGroupPeakIdentity.getIdentityOf(r);
                        if (gid != null)
                          data[d] = gid.getGroup().getGroupID();
                        break;
                      case AVG_CORR:
                        if (r2r != null && r2r.hasFeatureShapeCorrelation())
                          data[d] = r2r.getAvgPeakShapeR();
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
                  added++;
                }
                progress += 1.0 / totalRows;
              }
            }
          }
        }
      }

      LOG.info("ALl data created for export. Total of " + added + " links");
      // export ann
      // Filename
      TxtWriter writer = new TxtWriter();
      File realFile = FileAndPathUtil.eraseFormat(filename);
      realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
          realFile.getName() + "_annotations", ".csv");
      writer.openNewFileOutput(realFile);
      writer.write(ann.toString());
      writer.closeDatOutput();
      LOG.info("File created: " + realFile);
      return true;
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of MS annotation results error", t);
      if (task != null) {
        task.setStatus(TaskStatus.ERROR);
        task.setErrorMessage(t.getMessage());
      }
      return false;
    }
  }

  /**
   * Export correlation map to CSV
   * 
   * @param pkl
   * @param corrMap
   * @param minR
   * @param filename
   * @param progress
   * @param task can be null
   * @return
   */
  public static boolean exportCorrelationMapToCSV(MSEGroupedPeakList pkl, R2RCorrMap corrMap,
      double minR, File filename, Double progress, ANNOTATION[] col, AbstractTask task) {
    try {
      int totalRows = corrMap.size();
      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(col, ','));
      ann.append("\n");

      // for all rows
      for (Entry<String, R2RCorrelationData> e : corrMap.entrySet()) {
        if (task != null && task.isCanceled()) {
          return false;
        }

        // for correlation
        R2RCorrelationData r2r = e.getValue();
        if (r2r.hasFeatureShapeCorrelation() && r2r.getAvgPeakShapeR() >= minR) {
          int[] ids = R2RCorrMap.toKeyIDs(e.getKey());
          PeakListRow r = pkl.findRowByID(ids[0]);
          PeakListRow link = pkl.findRowByID(ids[1]);

          // the data
          Object[] data = new Object[col.length];
          ESIAdductIdentity id = null;
          // add all data
          for (int d = 0; d < col.length; d++) {
            switch (col[d]) {
              case ANNOTATION1:
                id = ESIAdductIdentity.getIdentityOf(r, link);
                data[d] = id != null ? id.getAdduct() : null;
                break;
              case ANNOTATION2:
                id = ESIAdductIdentity.getIdentityOf(link, r);
                data[d] = id != null ? id.getAdduct() : null;
                break;
              case ANNO_NETID:
                id = ESIAdductIdentity.getIdentityOf(r, link);
                data[d] = id != null ? id.getNetID() : null;
                break;
              case DELTA_MZ:
                data[d] = link.getAverageMZ() - r.getAverageMZ();
                break;
              case DELTA_AVG_RT:
                data[d] = link.getAverageRT() - r.getAverageRT();
                break;
              case ID1:
                data[d] = r.getID();
                break;
              case ID2:
                data[d] = link.getID();
                break;
              case MZ1:
                data[d] = r.getAverageMZ();
                break;
              case MZ2:
                data[d] = link.getAverageMZ();
                break;
              case IMAX_CORR:
                if (r2r.hasIMaxCorr())
                  data[d] = r2r.getCorrIProfile().getR();
                break;
              case TOTAL_CORR:
                if (r2r.hasFeatureShapeCorrelation())
                  data[d] = r2r.getTotalCorrelation().getR();
                break;
              case AVG_CORR:
                if (r2r.hasFeatureShapeCorrelation())
                  data[d] = r2r.getAvgPeakShapeR();
                break;
              case CORRELATED_F2F:
                if (r2r.hasFeatureShapeCorrelation())
                  data[d] = r2r.getCorrPeakShape().size();
                break;
              case CORR_GROUP:
                MSEGroupPeakIdentity pi = MSEGroupPeakIdentity.getIdentityOf(r);
                if (pi != null)
                  data[d] = pi.getGroup().getGroupID();
                break;
              case AVG_DP:
                data[d] = r2r.getAvgDPcount();
                break;
              case AREA1:
                data[d] = r.getBestPeak().getArea();
                break;
              case AREA2:
                data[d] = link.getBestPeak().getArea();
                break;
              case INTENSITY1:
                data[d] = r.getBestPeak().getHeight();
                break;
              case INTENSITY2:
                data[d] = link.getBestPeak().getHeight();
                break;
              case RT1:
                data[d] = r.getAverageRT();
                break;
              case RT2:
                data[d] = link.getAverageRT();
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
        progress += 1.0 / totalRows;
      }

      LOG.info("ALl data created for export");
      // export ann
      // Filename
      TxtWriter writer = new TxtWriter();
      File realFile = FileAndPathUtil.eraseFormat(filename);
      realFile = FileAndPathUtil.getRealFilePath(filename.getParentFile(),
          realFile.getName() + "_correlations", ".csv");
      writer.openNewFileOutput(realFile);
      writer.write(ann.toString());
      writer.closeDatOutput();
      LOG.info("File created: " + realFile);
      return true;
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of correlation and MS annotation results error", t);
      if (task != null) {
        task.setStatus(TaskStatus.ERROR);
        task.setErrorMessage(t.getMessage());
      }
      return false;
    }
  }

}
