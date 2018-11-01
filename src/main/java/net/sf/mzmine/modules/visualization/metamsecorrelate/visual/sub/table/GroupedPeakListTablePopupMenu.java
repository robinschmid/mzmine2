package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.table;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTablePopupMenu;

public class GroupedPeakListTablePopupMenu extends PeakListTablePopupMenu {

  private static final long serialVersionUID = 1L;
  private PKLRowGroup group;

  public GroupedPeakListTablePopupMenu(JFrame window, JTable listTable,
      DefaultTableColumnModel model, PeakList list, PKLRowGroup group) {
    super(window, listTable, model, list);
    this.group = group;
  }

  @Override
  protected PeakListRow getPeakListRow(int modelIndex) {
    if (modelIndex < 0 || modelIndex >= group.size())
      return null;
    return group.get(modelIndex);
  }
}
