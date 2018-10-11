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
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateModule;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrMap;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.io.TxtWriter;

public class ExportCorrAnnotationTask extends AbstractTask {

  private enum ANNOTATION {
    ID1, ID2, MZ1, MZ2, DELTA_MZ, DELTA_AVG_RT, ANNOTATION1, ANNOTATION2, AVG_CORR, AVG_DP, CORRELATED_F2F, TOTAL_CORR, IMAX_CORR;
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

  // Logger.
  private static final Logger LOG = Logger.getLogger(ExportCorrAnnotationTask.class.getName());

  private int finishedRows;
  private int totalRows;

  private final PeakList peakList;


  private final ParameterSet parameters;

  private File filename;

  /**
   * {@link MetaMSEcorrelateModule} or {@link MSAnnotationModule}
   */
  private boolean exAnnotations;
  /**
   * by {@link MetaMSEcorrelateModule}
   */
  private boolean exAvgCorr;

  private boolean exImaxCorr;

  private boolean exTotalCorr;

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

    finishedRows = 0;
    totalRows = 0;

    // tolerances
    filename = parameterSet.getParameter(ExportCorrAnnotationParameters.FILENAME).getValue();
    exAnnotations =
        parameterSet.getParameter(ExportCorrAnnotationParameters.EX_ANNOTATIONS).getValue();
    exAvgCorr = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_AVGCORR).getValue();
    exTotalCorr = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_TOTALCORR).getValue();
    exImaxCorr = parameterSet.getParameter(ExportCorrAnnotationParameters.EX_IMAX_CORR).getValue();
    minR = parameterSet.getParameter(ExportCorrAnnotationParameters.MIN_AVGCORR).getValue();
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : finishedRows / totalRows;
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

      // sort not needed? TODO
      totalRows = corrMap.size();

      // start document
      StringBuilder ann;

      ann = new StringBuilder();

      // add header
      ann.append(StringUtils.join(ANNOTATION.values(), ','));
      ann.append("\n");

      // all columns
      ANNOTATION[] col = ANNOTATION.values();

      // for all rows
      for (Entry<String, R2RCorrelationData> e : corrMap.entrySet()) {
        if (isCanceled()) {
          cancelExport();
          return;
        }

        // for correlation
        R2RCorrelationData r2r = e.getValue();
        if (r2r.hasFeatureShapeCorrelation() && r2r.getAvgPeakShapeR() >= minR) {
          int[] ids = corrMap.toKeyIDs(e.getKey());
          PeakListRow r = pkl.findRowByID(ids[0]);
          PeakListRow link = pkl.findRowByID(ids[1]);

          // the data
          Object[] data = new Object[col.length];
          // add all data
          for (int d = 0; d < col.length; d++) {
            switch (col[d]) {
              case ANNOTATION1:
                data[d] = ESIAdductIdentity.getIdentityOf(r, link);
                break;
              case ANNOTATION2:
                data[d] = ESIAdductIdentity.getIdentityOf(link, r);
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
                data[d] = r2r.getCorrIProfile();
                break;
              case TOTAL_CORR:
                data[d] = r2r.getTotalCorrelation();
                break;
              case AVG_CORR:
                data[d] = r2r.getAvgPeakShapeR();
                break;
              case CORRELATED_F2F:
                data[d] = r2r.getCorrPeakShape().size();
                break;
              case AVG_DP:
                data[d] = r2r.getAvgDPcount();
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
        finishedRows++;
      }

      LOG.info("ALl data created for export");
      // export ann
      // Filename
      TxtWriter writer = new TxtWriter();
      writer.openNewFileOutput(filename);
      writer.write(ann.toString());
      writer.closeDatOutput();
      LOG.info("File created: " + filename);
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Export of correlation and MS annotation results error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void cancelExport() {

  }
}
