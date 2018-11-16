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

package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.table;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.AbstractTableModel;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2GroupCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RFullCorrelationData;

public class GroupedPeakListTableModel extends AbstractTableModel {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private PeakList peakList;
  private RowGroup group;
  private SimilarityMeasure type = SimilarityMeasure.COSINE_SIM;

  // Logger.
  private static final Logger LOG = Logger.getLogger(MetaMSEcorrelateTask.class.getName());

  /**
   * Constructor, assign given dataset to this table
   */
  public GroupedPeakListTableModel(PeakList peakList) {
    this.peakList = peakList;
  }

  // TODO real count of columns
  @Override
  public int getColumnCount() {
    return CommonColumnType2.values().length + CorrelationColumnType.values().length;
  }

  @Override
  public int getRowCount() {
    return group != null ? group.size() : 0;
  }

  @Override
  public String getColumnName(int col) {
    return "column" + col;
  }

  @Override
  public Class<?> getColumnClass(int col) {

    if (isCommonColumn(col)) {
      CommonColumnType2 commonColumn = getCommonColumn(col);
      return commonColumn.getColumnClass();
    } else {
      CorrelationColumnType corColumn = getCorrelationColumn(col);
      return corColumn.getColumnClass();
    }

  }

  /**
   * This method returns the value at given coordinates of the dataset or null if it is a missing
   * value
   */

  @Override
  public Object getValueAt(int row, int col) {
    // get groups
    PeakListRow selectedRow = group.getLastViewedRow();
    // row of group
    if (group != null && row < group.size()) {
      PeakListRow pklRow = group.get(row);
      if (isCommonColumn(col)) {
        CommonColumnType2 commonColumn = getCommonColumn(col);

        switch (commonColumn) {
          case ROWID:
            return pklRow.getID();
          case GROUPID: // feature in group has a groupid
            return row;
          case COLOR:
            return CorrelationRowGroup.colors[row % CorrelationRowGroup.colors.length];
          case AVERAGEMZ:
            return pklRow.getAverageMZ();
          case AVERAGERT:
            if (pklRow.getAverageRT() <= 0)
              return null;
            return pklRow.getAverageRT();
          case COMMENT:
            return pklRow.getComment();
          case IDENTITY:
            return pklRow.getPreferredPeakIdentity();
        }
      } else {
        CorrelationColumnType corrCol = getCorrelationColumn(col);
        R2GroupCorrelationData corr = null;
        R2RFullCorrelationData r2r = null;

        if (group instanceof CorrelationRowGroup) {
          corr = ((CorrelationRowGroup) group).getCorr(row);
          r2r = corr.getCorrelationToRow(selectedRow);
        }


        switch (corrCol) {
          case MAXHEIGHT:
            return corr == null ? 0 : corr.getMaxHeight();
          case AVERAGE_DP_COUNT:
            return corr == null ? 0 : corr.getAvgDPCount();
          case AVERAGE_R_IPROFILE:
            return corr == null ? 0 : corr.getAvgIProfileR();
          case AVERAGE_R_PEAKSHAPE:
            return corr == null ? 0 : corr.getAvgPeakShapeR();
          case MAX_R_IPROFILE:
            return corr == null ? 0 : corr.getMaxIProfileR();
          case MAX_R_PEAKSHAPE:
            return corr == null ? 0 : corr.getMaxPeakShapeR();
          case AVG_TOTAL_R_PEAKSHAPE:
            return corr == null ? 0 : corr.getAvgTotalPeakShapeR();
          case MIN_R_IPROFILE:
            return corr == null ? 0 : corr.getMinIProfileR();
          case MIN_R_PEAKSHAPE:
            return corr == null ? 0 : corr.getMinPeakShapeR();
          case AVERAGE_COSINE_HEIGHT:
            if (selectedRow.getID() == pklRow.getID())
              return 1;
            else
              return r2r == null ? Double.NaN : r2r.getHeightSimilarity(type);
          case AVG_COSINE_SIM:
            if (selectedRow.getID() == pklRow.getID())
              return 1;
            else
              return r2r == null ? Double.NaN : r2r.getAvgPeakShapeSimilarity(type);
          case AVERAGE_TOTAL_SIM:
            if (selectedRow.getID() == pklRow.getID())
              return 1;
            else
              return r2r == null ? Double.NaN : r2r.getTotalSimilarity(type);
        }
      }
    } else {
      // should not happen! table was not updated correctly
      LOG.log(Level.SEVERE, "Error: table was not updated correctly - see getValueAt");
      return null;
    }
    return null;
  }

  public void setSimilarityMeasure(SimilarityMeasure type) {
    this.type = type;
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    CommonColumnType2 columnType = getCommonColumn(col);
    return ((columnType == CommonColumnType2.COMMENT)
        || (columnType == CommonColumnType2.IDENTITY));
  }

  @Override
  public void setValueAt(Object value, int row, int col) {
    CommonColumnType2 columnType = getCommonColumn(col);
    // row of group
    if (row < group.size()) {
      PeakListRow peakListRow = group.get(row);

      if (columnType == CommonColumnType2.COMMENT) {
        peakListRow.setComment((String) value);
      }
      if (columnType == CommonColumnType2.IDENTITY) {
        if (value instanceof PeakIdentity)
          peakListRow.setPreferredPeakIdentity((PeakIdentity) value);
      }
    }
  }

  public boolean isCommonColumn(int col) {
    return col < CommonColumnType2.values().length;
  }

  public CommonColumnType2 getCommonColumn(int col) {
    CommonColumnType2 commonColumns[] = CommonColumnType2.values();
    if (isCommonColumn(col))
      return commonColumns[col];

    return null;
  }

  public CorrelationColumnType getCorrelationColumn(int col) {

    CommonColumnType2 commonColumns[] = CommonColumnType2.values();
    CorrelationColumnType corrColumns[] = CorrelationColumnType.values();

    if (isCommonColumn(col))
      return null;
    else {
      // substract common columns from the index
      col -= commonColumns.length;
      return corrColumns[col];
    }
  }

  public RowGroup getGroup() {
    return group;
  }

  public void setGroup(RowGroup group) {
    boolean update = !group.equals(this.group);
    this.group = group;
    if (update)
      fireTableDataChanged();
  }

}
