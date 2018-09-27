package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.HashMap;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.PeakUtils;

public class MSEGroupedPeakList extends SimplePeakList {
	// parameter which groups the sample sets
	private UserParameter<?, ?> sgroupPara = null;
	// sample sets and size of this set as values
	private HashMap<Object, Integer> sgroups = null;
	// list of groups with rows
	private PKLRowGroupList groups;
	// save last viewed group
	private int lastViewedIndex = 0;

	public MSEGroupedPeakList(String name, RawDataFile raw) {
		super(name, raw); 
	}
	public MSEGroupedPeakList(String name, RawDataFile[] raw) {
		super(name, raw); 
	} 
	/**
	 * creates a copy of the given pkl by copying every row
	 * @param raw
	 * @param pkl
	 */
	public MSEGroupedPeakList(RawDataFile[] raw, PeakList pkl) {
		this(pkl.getName() + " MSEgr", raw);
		// copy all rows and fill in groups
		for(PeakListRow r : pkl.getRows()) {
			PeakListRow nr = copyPeakRow(r); 
			// add to new list
			this.addRow(nr);
		}
	}
	/**
	 * adds the groups to this peak list
	 * adds annotation to each feature in a group
	 * recalculates correlation of each row to the group
	 * @param groups
	 */
	public void setGroups(PKLRowGroupList groups) { 
		this.groups = groups;
		// for all rows in a group:
		for (int i = groups.size()-1; i >= 0; i--) {
			PKLRowGroup g = groups.get(i); 
			// add all identities 
			for (PeakListRow row : g) { 
				row.addPeakIdentity(new MSEGroupPeakIdentity(g), true);
			}
			// recalc correlation
			g.recalcGroupCorrelation();
		}
	}

	public PKLRowGroupList getGroups() {
		return groups;
	}
	/**
	 * Index of the last active group
	 * @return
	 */
	public int getLastViewedIndex() {
		return lastViewedIndex;
	}
	public void setLastViewedIndex(int lastViewedIndex) {
		this.lastViewedIndex = lastViewedIndex;
		checkLastViewedIndex();
	}
	/**
	 * Sums the last viewed index and i. Keeps in the constrains of the groups list.
	 * @param i
	 */
	public void addLastViewedIndex(int i) {
		lastViewedIndex += i;
		checkLastViewedIndex();
	}

	private void checkLastViewedIndex() {
		if(lastViewedIndex<0) lastViewedIndex = 0;
		else if(lastViewedIndex>=groups.size()) lastViewedIndex = groups.size()-1;
	}
	/**
	 * 
	 * @return The last viewed group of this peak list.
	 */
	public PKLRowGroup getLastViewedGroup() {
		return groups==null || lastViewedIndex>=groups.size()? null : groups.get(lastViewedIndex);
	}

	/**
	 * Create a copy of a peak list row.
	 */
	public static PeakListRow copyPeakRow(final PeakListRow row) {
		// Copy the peak list row.
		final PeakListRow newRow = new SimplePeakListRow(row.getID());
		PeakUtils.copyPeakListRowProperties(row, newRow);
		// Copy the peaks.
		for (final Feature peak : row.getPeaks()) {

			// Only keep peak if it fulfills the filter criteria
			final Feature newPeak = new SimpleFeature(peak);
			PeakUtils.copyPeakProperties(peak, newPeak);
			newRow.addPeak(peak.getDataFile(), newPeak);
		}
		return newRow;
	}
	public void setSampleGroupsParameter(UserParameter<?, ?> sgroupPara) {
		this.sgroupPara = sgroupPara;
	}
	public void setSampleGroups(HashMap<Object, Integer> sgroups) {
		this.sgroups = sgroups;
	}
	public UserParameter<?, ?> getSampleGroupsParameter() {
		return sgroupPara;
	}
	public HashMap<Object, Integer> getSampleGroups() {
		return sgroups;
	}
}
