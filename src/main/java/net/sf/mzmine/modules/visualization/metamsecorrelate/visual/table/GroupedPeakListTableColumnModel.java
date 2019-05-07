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

package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.table;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Enumeration;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.peaklisttable.table.CompoundIdentityCellRenderer;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.components.ColumnGroup;
import net.sf.mzmine.util.components.GroupableTableHeader;

/**
 * 
 */
public class GroupedPeakListTableColumnModel extends DefaultTableColumnModel
    implements MouseListener {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private static final Font editFont = new Font("SansSerif", Font.PLAIN, 10);

  private FormattedCellRenderer mzRenderer, rtRenderer, intensityRenderer, corrRenderer,
      dataPointsRenderer;
  private TableCellRenderer identityRenderer, datapointsRenderer, qcRenderer;
  private DefaultTableCellRenderer defaultRenderer, defaultRendererLeft;
  private ColorTableCellRenderer colorRenderer;

  private ParameterSet parameters;
  private PeakList peakList;
  private GroupableTableHeader header;

  private TableColumn columnBeingResized;

  /**
   * 
   */
  GroupedPeakListTableColumnModel(GroupableTableHeader header, GroupedPeakListTableModel tableModel,
      ParameterSet parameters, PeakList peakList) {

    this.parameters = parameters;
    this.peakList = peakList;

    this.header = header;

    header.addMouseListener(this);

    // prepare formatters
    NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

    // prepare cell renderers
    mzRenderer = new FormattedCellRenderer(mzFormat);
    rtRenderer = new FormattedCellRenderer(rtFormat);
    intensityRenderer = new FormattedCellRenderer(intensityFormat);
    identityRenderer = new CompoundIdentityCellRenderer();
    defaultRenderer = new DefaultTableCellRenderer();
    defaultRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    defaultRendererLeft = new DefaultTableCellRenderer();
    defaultRendererLeft.setHorizontalAlignment(SwingConstants.LEFT);
    datapointsRenderer = new FormattedCellRenderer(new DecimalFormat());
    qcRenderer = new FormattedCellRenderer(new DecimalFormat());
    corrRenderer = new FormattedCellRenderer(new DecimalFormat("0.0000"));
    dataPointsRenderer = new FormattedCellRenderer(new DecimalFormat("#0.0"));
    colorRenderer = new ColorTableCellRenderer();
  }

  public void createColumns() {

    // clear column groups
    ColumnGroup groups[] = header.getColumnGroups();
    if (groups != null) {
      for (ColumnGroup group : groups) {
        header.removeColumnGroup(group);
      }
    }

    // clear the column model
    while (getColumnCount() > 0) {
      TableColumn col = getColumn(0);
      removeColumn(col);
    }

    // create the "average" group
    ColumnGroup averageGroup = new ColumnGroup("Average");
    header.addColumnGroup(averageGroup);

    JTextField editorField = new JTextField();
    editorField.setFont(editFont);
    DefaultCellEditor defaultEditor = new DefaultCellEditor(editorField);

    //
    CommonColumnType2 visibleCommonColumns[] = CommonColumnType2.values();
    CorrelationColumnType corrColumns[] = CorrelationColumnType.values();

    // add group id col
    TableColumn newColumn = new TableColumn(0);
    newColumn.setHeaderValue(corrColumns[0].getColumnName());
    newColumn.setIdentifier(corrColumns[0]);
    this.addColumn(newColumn);

    // add common columns
    for (int i = 0; i < visibleCommonColumns.length; i++) {

      CommonColumnType2 commonColumn = visibleCommonColumns[i];

      newColumn = new TableColumn(i + 1);
      newColumn.setHeaderValue(commonColumn.getColumnName());
      newColumn.setIdentifier(commonColumn);

      switch (commonColumn) {
        case COLOR:
          newColumn.setCellRenderer(colorRenderer);
          break;
        case AVERAGEMZ:
          newColumn.setCellRenderer(mzRenderer);
          break;
        case AVERAGERT:
          newColumn.setCellRenderer(rtRenderer);
          break;
        case IDENTITY:
          newColumn.setCellRenderer(identityRenderer);
          break;
        case COMMENT:
          newColumn.setCellRenderer(defaultRendererLeft);
          newColumn.setCellEditor(defaultEditor);
          break;
        default:
          newColumn.setCellRenderer(defaultRenderer);
      }

      this.addColumn(newColumn);
      if ((commonColumn == CommonColumnType2.AVERAGEMZ)
          || (commonColumn == CommonColumnType2.AVERAGERT)) {
        averageGroup.add(newColumn);
      }
    }

    // add correlation columns
    ColumnGroup peakShapeGroup = new ColumnGroup("Peak shape correlation");
    header.addColumnGroup(peakShapeGroup);
    ColumnGroup iProfileGroup = new ColumnGroup("Intensity profile correlation");
    header.addColumnGroup(iProfileGroup);

    for (int i = 1; i < corrColumns.length; i++) {
      CorrelationColumnType corrCol = corrColumns[i];
      int modelIndex = CommonColumnType2.values().length + i;

      newColumn = new TableColumn(modelIndex);
      newColumn.setHeaderValue(corrCol.getColumnName());
      newColumn.setIdentifier(corrCol);

      switch (corrCol) {
        case AVERAGE_DP_COUNT:
          newColumn.setCellRenderer(dataPointsRenderer);
          break;
        case AVERAGE_R_IPROFILE:
          newColumn.setCellRenderer(corrRenderer);
          break;
        case AVERAGE_R_PEAKSHAPE:
          newColumn.setCellRenderer(corrRenderer);
          break;
        case MAX_R_IPROFILE:
          newColumn.setCellRenderer(corrRenderer);
          break;
        case MAX_R_PEAKSHAPE:
          newColumn.setCellRenderer(corrRenderer);
          break;
        case MIN_R_IPROFILE:
          newColumn.setCellRenderer(corrRenderer);
          break;
        case MIN_R_PEAKSHAPE:
          newColumn.setCellRenderer(corrRenderer);
          break;
        case MAXHEIGHT:
          newColumn.setCellRenderer(intensityRenderer);
          break;
        default:
          newColumn.setCellRenderer(defaultRenderer);
          break;
      }

      this.addColumn(newColumn);

      if (i >= 1 && i <= 5)
        peakShapeGroup.add(newColumn);
      else if (i >= 6 && i <= 8)
        iProfileGroup.add(newColumn);
    }

  }

  /**
   * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseClicked(MouseEvent e) {
    // ignore
  }

  /**
   * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseEntered(MouseEvent e) {
    // ignore
  }

  /**
   * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseExited(MouseEvent e) {
    // ignore
  }

  /**
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(MouseEvent e) {
    columnBeingResized = header.getResizingColumn();
  }

  /**
   * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
   */
  @Override
  public void mouseReleased(MouseEvent e) {

    if (columnBeingResized == null)
      return;

    /*
     * ColumnSettingParameter<CorrelationColumnType> csPar = parameters
     * .getParameter(PeakListTableParameters.commonColumns);
     * 
     * ColumnSettingParameter<DataFileColumnType> dfPar = parameters
     * .getParameter(PeakListTableParameters.dataFileColumns);
     * 
     * final int modelIndex = columnBeingResized.getModelIndex(); final int newWidth =
     * columnBeingResized.getPreferredWidth();
     * 
     * final int numOfCommonColumns = CorrelationColumnType.values().length; final int
     * numOfDataFileColumns = DataFileColumnType.values().length;
     * 
     * if (modelIndex < numOfCommonColumns) { csPar.setColumnWidth(modelIndex, newWidth); } else {
     * int dataFileColumnIndex = (modelIndex - numOfCommonColumns) % numOfDataFileColumns;
     * dfPar.setColumnWidth(dataFileColumnIndex, newWidth);
     * 
     * // set same width to other data file columns of this type for (int dataFileIndex =
     * peakList.getNumberOfRawDataFiles() - 1; dataFileIndex >= 0; dataFileIndex--) { int
     * columnIndex = numOfCommonColumns + (dataFileIndex * numOfDataFileColumns) +
     * dataFileColumnIndex;
     * 
     * TableColumn col = this.getColumnByModelIndex(columnIndex);
     * 
     * int currentWidth = col.getPreferredWidth();
     * 
     * if (currentWidth != newWidth) { col.setPreferredWidth(newWidth); } }
     * 
     * }
     */
  }

  public TableColumn getColumnByModelIndex(int modelIndex) {
    Enumeration<TableColumn> allColumns = this.getColumns();
    while (allColumns.hasMoreElements()) {
      TableColumn col = allColumns.nextElement();
      if (col.getModelIndex() == modelIndex)
        return col;
    }
    return null;
  }

}
