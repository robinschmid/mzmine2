/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.table;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.table.AbstractTableModel;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.GroupCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupPeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;  

public class GroupedPeakListTableModel extends AbstractTableModel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MSEGroupedPeakList peakList;

	// Logger.
	private static final Logger LOG = Logger.getLogger(MetaMSEcorrelateTask.class
			.getName());
	/**
	 * Constructor, assign given dataset to this table
	 */
	public GroupedPeakListTableModel(MSEGroupedPeakList peakList) {
		this.peakList = peakList; 
	}

	// TODO real count of columns
	public int getColumnCount() {
		return CommonColumnType2.values().length + CorrelationColumnType.values().length;
	}

	public int getRowCount() {
		PKLRowGroup group = peakList.getLastViewedGroup(); 
		return group!=null? group.size() : 0;
	}

	public String getColumnName(int col) {
		return "column" + col;
	}

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
	 * This method returns the value at given coordinates of the dataset or null
	 * if it is a missing value
	 */

	public Object getValueAt(int row, int col) { 
		// get groups 
		PKLRowGroup group = peakList.getLastViewedGroup();
		// row of group
		if(group!=null && row<group.size()) {
			PeakListRow pklRow = group.get(row);
			if (isCommonColumn(col)) {
				CommonColumnType2 commonColumn = getCommonColumn(col);

				switch (commonColumn) {
				case COLOR:
					return PKLRowGroup.colors[row%PKLRowGroup.colors.length];
				case ROWID:
					return new Integer(pklRow.getID());
				case AVERAGEMZ:
					return new Double(pklRow.getAverageMZ());
				case AVERAGERT:
					if (pklRow.getAverageRT() <= 0)
						return null;
					return new Double(pklRow.getAverageRT());
				case COMMENT:
					return pklRow.getComment();
				case IDENTITY:
					return pklRow.getPreferredPeakIdentity(); 
				}
			} else {

				CorrelationColumnType corrCol = getCorrelationColumn(col); 
				GroupCorrelationData corr = group.getCorr(row);

				switch (corrCol) {
				case GROUPID: // feature in group has a groupid
					return row;
				case MAXHEIGHT:
					return corr.getMaxHeight(); 
				case AVERAGE_DP_COUNT:
					return corr.getAvgDPCount();
				case AVERAGE_R_IPROFILE:
					return corr.getAvgIProfileR();
				case AVERAGE_R_PEAKSHAPE:
					return corr.getAvgPeakShapeR();
				case MAX_R_IPROFILE:
					return corr.getMaxIProfileR();
				case MAX_R_PEAKSHAPE:
					return corr.getMaxPeakShapeR();
				case MIN_R_IPROFILE:
					return corr.getMinIProfileR();
				case MIN_R_PEAKSHAPE:
					return corr.getMinPeakShapeR();
				}
			}
		}
		else {
			// should not happen! table was not updated correctly 
			LOG.log(Level.SEVERE, "Error: table was not updated correctly - see getValueAt");
			return null;
		}  
		return null; 
	}

	public boolean isCellEditable(int row, int col) {
		CommonColumnType2 columnType = getCommonColumn(col);
		return ((columnType == CommonColumnType2.COMMENT) || (columnType == CommonColumnType2.IDENTITY));
	}

	public void setValueAt(Object value, int row, int col) {
		CommonColumnType2 columnType = getCommonColumn(col);

		// get groups 
		PKLRowGroup group = peakList.getLastViewedGroup();
		// row of group
		if(row<group.size()) {
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
		return col!=0 && col < CommonColumnType2.values().length+1;
	}

	public CommonColumnType2 getCommonColumn(int col) {
		CommonColumnType2 commonColumns[] = CommonColumnType2.values();
		if (isCommonColumn(col))
			return commonColumns[col-1];

		return null; 
	}

	public CorrelationColumnType getCorrelationColumn(int col) {

		CommonColumnType2 commonColumns[] = CommonColumnType2.values();
		CorrelationColumnType corrColumns[] = CorrelationColumnType.values();

		if (isCommonColumn(col))
			return null;
		else if(col==0) {
			return corrColumns[0];
		} 
		else { 
			// substract common columns from the index
			col -= commonColumns.length;  
			return corrColumns[col];
		} 
	} 

}
