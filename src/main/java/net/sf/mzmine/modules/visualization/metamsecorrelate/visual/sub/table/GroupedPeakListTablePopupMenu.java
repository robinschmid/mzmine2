package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.table;

import javax.swing.JFrame;
import javax.swing.table.DefaultTableColumnModel;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTablePopupMenu;

public class GroupedPeakListTablePopupMenu extends PeakListTablePopupMenu {

  private static final long serialVersionUID = 1L;

  private GroupedPeakListTable table;

  public GroupedPeakListTablePopupMenu(JFrame window, GroupedPeakListTable listTable,
      DefaultTableColumnModel model, PeakList list) {
    super(window, listTable, model, list);
    this.table = table;
  }

  @Override
  protected PeakListRow getPeakListRow(int modelIndex) {
    RowGroup group = getGroup();
    if (group == null || modelIndex < 0 || modelIndex >= group.size())
      return null;
    return group.get(modelIndex);
  }

  public RowGroup getGroup() {
    return table.getTableModel().getGroup();
  }
}
