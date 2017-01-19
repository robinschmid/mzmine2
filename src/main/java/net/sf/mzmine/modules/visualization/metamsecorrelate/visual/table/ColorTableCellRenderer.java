package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.table;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class ColorTableCellRenderer extends DefaultTableCellRenderer {  
	private static final long serialVersionUID = 1L;

	@Override
	public Component getTableCellRendererComponent(JTable table, Object color,
			boolean isSelected, boolean hasFocus, int row, int column) {

		Component rendererComp = super.getTableCellRendererComponent(table, color, isSelected, hasFocus, row, column);

		//Set foreground color
		rendererComp.setForeground((Color)color);

		//Set background color
		rendererComp.setBackground((Color)color);

		return rendererComp ;
	} 
}
