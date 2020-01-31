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

package net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.export;

import java.io.File;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.datamodel.identities.iontype.IonNetwork;
import net.sf.mzmine.datamodel.identities.iontype.IonNetworkLogic;
import net.sf.mzmine.datamodel.identities.iontype.networks.IonNetworkRelationInterf;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.datamodel.impl.RowGroupList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.MetaCorrelateModule;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.MS2SimilarityProviderGroup;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2RMap;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.R2RMS2Similarity;
import net.sf.mzmine.modules.peaklistmethods.io.gnpsexport.GNPSExportParameters.RowFilter;
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
   * by {@link MetaCorrelateModule}
   */
  private boolean exportAnnotationEdges = true, exportCorrelationEdges = false;
  private boolean exportIinRelationships = false;
  private boolean exportMS2SimilarityEdges = false;
  private boolean exportMS2DiffSimilarityEdges = false;
  private double minR;
  private final PeakList peakList;
  private File filename;


  private RowFilter filter;



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
    exportAnnotationEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_ANNOT).getValue();
    exportIinRelationships =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_IIN_RELATIONSHIP).getValue();
    exportCorrelationEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_CORR).getValue();
    exportMS2DiffSimilarityEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_MS2_DIFF_SIMILARITY).getValue();
    exportMS2SimilarityEdges =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_MS2_SIMILARITY).getValue();
    filter = parameterSet.getParameter(ExportCorrAnnotationParameters.FILTER).getValue();
  }

  /**
   * Create the task.
   *
   * @param parameterSet the parameters.
   * @param list peak list.
   */
  public ExportCorrAnnotationTask(PeakList peakList, File filename, double minR, RowFilter filter,
      boolean exportAnnotationEdges, boolean exportCorrelationEdges,
      boolean exportIinRelationships) {
    this.peakList = peakList;
    this.filename = filename;
    this.minR = minR;
    this.filter = filter;
    this.exportAnnotationEdges = exportAnnotationEdges;
    this.exportCorrelationEdges = exportCorrelationEdges;
    this.exportIinRelationships = exportIinRelationships;
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

      // export edges of annotations
      if (exportAnnotationEdges)
        exportAnnotationEdges(peakList, filename, filter.equals(RowFilter.ONLY_WITH_MS2), progress,
            this);

      // relationships between ion identity networks (+O) ...
      if (exportIinRelationships)
        exportIINRelationships(peakList, filename, filter.equals(RowFilter.ONLY_WITH_MS2), progress,
            this);

      // export MS2Similarity edges
      if (exportMS2DiffSimilarityEdges)
        exportMS2DiffSimilarityEdges(peakList, filename, filter, progress, this);
      if (exportMS2SimilarityEdges)
        exportMS2SimilarityEdges(peakList, filename, filter, progress, this);

      // export edges of corr
      if (exportCorrelationEdges)
        exportCorrelationEdges(peakList, filename, progress, this, minR, filter);

    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of correlation and MS annotation results error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }

    setStatus(TaskStatus.FINISHED);
  }


  public static boolean exportAnnotationEdges(PeakList pkl, File filename, boolean limitToMSMS,
      Double progress, AbstractTask task) {
    LOG.info("Export annotation edge file");
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
        if (r.hasIonIdentity()) {
          r.getIonIdentities().forEach(adduct -> {
            ConcurrentHashMap<PeakListRow, IonIdentity> links = adduct.getPartner();

            // add all connection for ids>rowID
            links.entrySet().stream().filter(Objects::nonNull)
                .filter(e -> e.getKey().getID() > rowID).forEach(e -> {
                  PeakListRow link = e.getKey();
                  if (!limitToMSMS || link.getBestFragmentation() != null) {
                    IonIdentity id = e.getValue();
                    double dmz = Math.abs(r.getAverageMZ() - link.getAverageMZ());
                    // the data
                    exportEdge(ann, "MS1 annotation", rowID, e.getKey().getID(),
                        corrForm.format((id.getScore() + adduct.getScore()) / 2d), //
                        id.getAdduct() + " " + adduct.getAdduct() + " dm/z=" + mzForm.format(dmz));
                    added.incrementAndGet();
                  }
                });
          });
        }
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


  public static boolean exportIINRelationships(PeakList pkl, File filename, boolean limitToMSMS,
      Double progress, AbstractTask task) {
    LOG.info("Export IIN relationships edge file");
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat corrForm = new DecimalFormat("0.000");

    try {
      StringBuilder ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);

      IonNetwork[] nets = IonNetworkLogic.getAllNetworks(pkl, true);
      for (IonNetwork n : nets) {
        Map<IonNetwork, IonNetworkRelationInterf> relations = n.getRelations();
        if (relations != null && !relations.isEmpty()) {
          for (Entry<IonNetwork, IonNetworkRelationInterf> rel : relations.entrySet()) {
            // export all relations where n.id is smaller than the related network
            if (rel.getValue().isLowestIDNetwork(n)) {
              // relationship can be between multiple nets
              for (IonNetwork net2 : rel.getValue().getAllNetworks()) {
                if (net2.equals(n))
                  continue;

                // find best two nodes
                PeakListRow[] rows = getBestRelatedRows(n, net2, limitToMSMS);
                // export lowest mz -> highest mz
                if (rows[0].getAverageMZ() > rows[1].getAverageMZ()) {
                  exportEdge(ann, "IIN M relationship", rows[1].getID(), rows[0].getID(), "0", //
                      rel.getValue().getName(net2));
                } else {
                  exportEdge(ann, "IIN M relationship", rows[0].getID(), rows[1].getID(), "0", //
                      rel.getValue().getName(n));
                }

                added.incrementAndGet();
              }
            }
          }
        }
      }
      LOG.info("IIN relationship edges exported " + added.get() + "");

      // export ann edges
      // Filename
      if (added.get() > 0) {
        writeToFile(ann.toString(), filename, "_edges_iin_relations", ".csv");
        return true;
      } else
        return false;
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
  }

  private static PeakListRow[] getBestRelatedRows(IonNetwork a, IonNetwork b, boolean limitToMSMS) {
    PeakListRow[] rows = new PeakListRow[2];
    double sumIntensity = 0;
    for (Entry<PeakListRow, IonIdentity> entryA : a.entrySet()) {
      if (!limitToMSMS || entryA.getKey().getBestFragmentation() != null) {
        IonIdentity iinA = entryA.getValue();
        for (Entry<PeakListRow, IonIdentity> entryB : b.entrySet()) {
          if (!limitToMSMS || entryB.getKey().getBestFragmentation() != null) {
            IonIdentity iinB = entryB.getValue();
            if (iinA.getAdduct().equals(iinB.getAdduct())) {
              double sum = entryA.getKey().getAverageHeight() + entryB.getKey().getAverageHeight();
              if (sum >= sumIntensity) {
                sumIntensity = sum;
                rows[0] = entryA.getKey();
                rows[1] = entryB.getKey();
              }
            }
          }
        }
      }
    }
    if (rows[0] == null) {
      try {
        rows[0] = a.keySet().iterator().next();
        rows[1] = b.keySet().iterator().next();
      } catch (Exception ex) {
      }
    }
    return rows;
  }

  public static boolean exportMS2SimilarityEdges(PeakList pkl, File filename, RowFilter filter,
      Double progress, AbstractTask task) {
    try {
      RowGroupList groups = pkl.getGroups();
      if (groups != null && !groups.isEmpty()) {
        LOG.info("Export MS2 similarities edge file");
        NumberFormat corrForm = new DecimalFormat("0.000");
        NumberFormat overlapForm = new DecimalFormat("0.0");

        StringBuilder ann = new StringBuilder();
        // add header
        ann.append(StringUtils.join(EDGES.values(), ','));
        ann.append("\n");
        AtomicInteger added = new AtomicInteger(0);

        for (RowGroup g : groups) {
          if (task != null && task.isCanceled()) {
            return false;
          }

          if (g instanceof MS2SimilarityProviderGroup) {
            R2RMap<R2RMS2Similarity> map = ((MS2SimilarityProviderGroup) g).getMS2SimilarityMap();
            for (Entry<String, R2RMS2Similarity> e : map.entrySet()) {
              R2RMS2Similarity r2r = e.getValue();
              if (r2r.getDiffAvgCosine() == 0 && r2r.getDiffMaxOverlap() == 0)
                continue;
              PeakListRow a = r2r.getA();
              PeakListRow b = r2r.getB();
              // no self-loops
              if (a.getID() != b.getID() && filter.filter(a) && filter.filter(b)) {
                // the data
                exportEdge(ann, "MS2 sim", a.getID(), b.getID(),
                    corrForm.format(r2r.getDiffAvgCosine()), //
                    MessageFormat.format("cos={0} ({1})", corrForm.format(r2r.getDiffAvgCosine()),
                        overlapForm.format(r2r.getDiffMaxOverlap())));
                added.incrementAndGet();
              }
            }
          }
        }

        LOG.info("MS2 similarity edges exported " + added.get() + "");

        // export ann edges
        // Filename
        if (added.get() > 0) {
          writeToFile(ann.toString(), filename, "_edges_ms2similarity", ".csv");
          return true;
        } else
          return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
    return false;
  }

  public static boolean exportMS2DiffSimilarityEdges(PeakList pkl, File filename, RowFilter filter,
      Double progress, AbstractTask task) {
    try {
      RowGroupList groups = pkl.getGroups();
      if (groups != null && !groups.isEmpty()) {
        LOG.info("Export MS2 diff similarities edge file");
        NumberFormat corrForm = new DecimalFormat("0.000");
        NumberFormat overlapForm = new DecimalFormat("0.0");

        StringBuilder ann = new StringBuilder();
        // add header
        ann.append(StringUtils.join(EDGES.values(), ','));
        ann.append("\n");
        AtomicInteger added = new AtomicInteger(0);

        for (RowGroup g : groups) {
          if (task != null && task.isCanceled()) {
            return false;
          }

          if (g instanceof MS2SimilarityProviderGroup) {
            R2RMap<R2RMS2Similarity> map = ((MS2SimilarityProviderGroup) g).getMS2SimilarityMap();
            for (Entry<String, R2RMS2Similarity> e : map.entrySet()) {
              R2RMS2Similarity r2r = e.getValue();
              if (r2r.getSpectralAvgCosine() == 0 && r2r.getSpectralMaxOverlap() == 0)
                continue;
              PeakListRow a = r2r.getA();
              PeakListRow b = r2r.getB();
              // no self-loops
              if (a.getID() != b.getID() && filter.filter(a) && filter.filter(b)) {
                // the data
                exportEdge(ann, "MS2 diff sim", a.getID(), b.getID(),
                    corrForm.format(r2r.getSpectralAvgCosine()), //
                    MessageFormat.format("diff cos={0} ({1})",
                        corrForm.format(r2r.getSpectralAvgCosine()),
                        overlapForm.format(r2r.getSpectralMaxOverlap())));
                added.incrementAndGet();
              }
            }
          }
        }

        LOG.info("MS2 diff similarity edges exported " + added.get() + "");

        // export ann edges
        // Filename
        if (added.get() > 0) {
          writeToFile(ann.toString(), filename, "_edges_ms2diffsimilarity", ".csv");
          return true;
        } else
          return false;
      }
    } catch (Exception e) {
      throw new MSDKRuntimeException(e);
    }
    return false;
  }

  public static boolean exportCorrelationEdges(PeakList pkl, File filename, Double progress,
      AbstractTask task, double minCorr, RowFilter filter) {

    NumberFormat corrForm = new DecimalFormat("0.000");
    try {
      StringBuilder ann = new StringBuilder();
      // add header
      ann.append(StringUtils.join(EDGES.values(), ','));
      ann.append("\n");

      AtomicInteger added = new AtomicInteger(0);
      // for all rows
      R2RCorrelationData.streamFrom(pkl).filter(r2r -> r2r.getAvgShapeR() >= minCorr)
          .forEach(r2r -> {
            PeakListRow a = r2r.getRowA();
            PeakListRow b = r2r.getRowB();
            //
            boolean export = true;
            if (!filter.equals(RowFilter.ALL)) {
              // only export rows with MSMS
              export = filter.filter(a) && filter.filter(b);
            }

            //
            if (export) {
              exportEdge(ann, "MS1 shape correlation", a.getID(), b.getID(),
                  corrForm.format(r2r.getAvgShapeR()), "r=" + corrForm.format(r2r.getAvgShapeR()));
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
