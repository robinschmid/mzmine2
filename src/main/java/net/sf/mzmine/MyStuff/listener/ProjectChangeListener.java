package net.sf.mzmine.MyStuff.listener;

import net.sf.mzmine.desktop.impl.projecttree.PeakListTreeModel;
import net.sf.mzmine.desktop.impl.projecttree.RawDataTreeModel;

public abstract class ProjectChangeListener {
	public abstract void projectChanged();
}
