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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateModule;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationModule;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class ExportCorrAnnotationTask extends AbstractTask {

  private enum ANNOTATION {
    ID1, ID2, DELTA_MZ, ANNOTATION1, ANNOTATION2, AVG_CORR, TOTAL_CORR, IMAX_CORR;
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
  private Boolean exAnnotations;
  /**
   * by {@link MetaMSEcorrelateModule}
   */
  private Boolean exAvgCorr;

  private Boolean exImaxCorr;

  private Boolean exTotalCorr;

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
      PeakListRow[] rows = pkl.getRows();
      Arrays.sort(rows, new PeakListRowSorter(SortingProperty.ID, SortingDirection.Ascending));
      totalRows = rows.length;
      
      // start document
      StringBuilder ann, corr;
      if(exAnnotations) {
        ann = new StringBuilder();

      if(exAvgCorr) 
        corr = new StringBuilder();
      
      // add header
      ann.append(StringUtils.join(ANNOTATION.values(), ','));
      ann.append("\n");
      
      // all columns
      ANNOTATION[] col = ANNOTATION.values();
      
      // for all rows
      for (int i = 0; i < rows.length; i++) {
        if (isCanceled()) {
          cancelExport();
          return;
        }
        
        PeakListRow r = rows[i];
        
        // get all links
        List<PeakListRow> links = MSAnnotationNetworkLogic.findAllAnnotationConnections(rows, r);

        // the data
        Object[] data = new Object[col.length];
        
        // only for links with higher ID
        for (PeakListRow link : links) {
          if(r.getID()<link.getID()) {
            // add all data
            for(int d=0; d<col.length; d++) {
              switch(col[d]) {
                case ANNOTATION1:
                  data[d] = ESIAdductIdentity.getIdentityOf(r, link);
                  break;
                case ANNOTATION2:
                  data[d] = ESIAdductIdentity.getIdentityOf(link,r);
                  break;
                case DELTA_MZ:
                  data[d] = link.getAverageMZ() - r.getAverageMZ();
                  break;
                case ID1:
                  data[d] = r.getID();
                  break;
                case ID2:
                  data[d] = link.getID();
                  break;
                case IMAX_CORR:
                  break;
                case TOTAL_CORR:
                  break;
                case AVG_CORR:
                  data[d] = 
                  break;
              }
            }
            // replace null
            for (int j = 0; j < data.length; j++) {
              if(data[j]==null)
                data[j] = "";
            }
            // add data
            ann.append(StringUtils.join(data, ','));
            ann.append("\n");
          }
        }
        
        // export adduct connections
        
        

        // export correlation connections
        
        

        finishedRows = i;
      }

      LOG.info("A total of " + compared + " row2row comparisons with " + annotPairs
          + " annotation pairs");
      List net = MSAnnotationNetworkLogic.createAnnotationNetworks(peakList, true);
      LOG.info("A total of " + net.size() + " networks");

      LOG.info("Show most likely annotations");
      MSAnnotationNetworkLogic.showMostlikelyAnnotations(peakList);


      // finish
      if (!isCanceled()) {
        peakList.addDescriptionOfAppliedTask(
            new SimplePeakListAppliedMethod("Identification of adducts", parameters));

        // Repaint the window to reflect the change in the peak list
        Desktop desktop = MZmineCore.getDesktop();
        if (!(desktop instanceof HeadLessDesktop))
          desktop.getMainWindow().repaint();

        // Done.
        setStatus(TaskStatus.FINISHED);
        LOG.info("Finished adducts search in " + peakList);
      }
    } catch (Exception t) {
      LOG.log(Level.SEVERE, "Adduct search error", t);
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
    }
  }
}
