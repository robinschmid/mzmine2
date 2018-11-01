package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.table;

import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTablePopupMenu;

public class GroupedPeakListTablePopupMenu extends PeakListTablePopupMenu {

  private static final long serialVersionUID = 1L;

  public GroupedPeakListTablePopupMenu(JFrame window, JTable listTable,
      DefaultTableColumnModel model, MSEGroupedPeakList list) {
    super(window, listTable, model, list);
  }

  @Override
  protected PeakListRow getPeakListRow(int modelIndex) {
    PKLRowGroup group = getGroup();
    if (group == null || modelIndex < 0 || modelIndex >= group.size())
      return null;
    return group.get(modelIndex);
  }

  public PKLRowGroup getGroup() {
    return ((MSEGroupedPeakList) peakList).getLastViewedGroup();
  }
}
