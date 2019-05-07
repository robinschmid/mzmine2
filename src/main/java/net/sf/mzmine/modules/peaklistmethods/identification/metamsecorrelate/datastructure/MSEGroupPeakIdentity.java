package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.mascot.MascotSearchTask;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.MetaMSEcorrelateTask;

public class MSEGroupPeakIdentity extends SimplePeakIdentity {
	private PKLRowGroup group;
	
	public MSEGroupPeakIdentity(PKLRowGroup group) { 
		this.group = group;
		int i = group.getGroupID();
		setPropertyValue(PROPERTY_NAME, "G("+ (i<100? (i<10? "00" : "0") : "") +i+")");
	} 

	public PKLRowGroup getGroup() {
		return group;
	} 
}
