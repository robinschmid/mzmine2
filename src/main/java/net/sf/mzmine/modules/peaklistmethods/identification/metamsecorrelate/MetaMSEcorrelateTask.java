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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.FeatureShapeCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroupList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.RowCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.modules.peaklistmethods.isotopes.aligneddeisotoper.AlignedIsotopeGrouperTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

import org.apache.commons.math.stat.regression.SimpleRegression;

public class MetaMSEcorrelateTask extends AbstractTask {

	// Logger.
	private static final Logger LOG = Logger.getLogger(MetaMSEcorrelateTask.class
			.getName());

	private int finishedRows;
	private int totalRows;
	private double progress;
	private final PeakList[] peakLists;

	private final RTTolerance rtTolerance;
	private final MZTolerance mzTolerance;
	// adducts
	private final ESIAdductType[] selectedAdducts, selectedMods;
	private Vector<ESIAdductType> allAdducts = new Vector<ESIAdductType>();
	private Vector<ESIAdductType> allModification = new Vector<ESIAdductType>();
	private final boolean useAdductBonusR, searchAdducts, isPositive;
	private final double adductBonusR;
	private final int maxMolecules, maxCombinations, maxCharge, maxMods;
	//
	private final String groupingParameter;
	// sample group size
	private UserParameter<?, ?> sgroupPara;
	private HashMap<Object, Integer> sgroupSize;
	private boolean hasToFilterMinFInSampleSets;

	// filter
	private final double mainPeakIntensity, percContainedInSamples;
	// pearson correlation r to identify negative correlation
	private final double minIntensityProfileR, minShapeCorrR;
	private static double noiseLevelShapeCorr=1E4, minCorrelatedDataPoints=3;

	private final ParameterSet parameters;
	private final MZmineProject project; 

	// output
	private MSEGroupedPeakList[] groupedPKL;


	/**
	 * Create the task.
	 *
	 * @param parameterSet
	 *            the parameters.
	 * @param list
	 *            peak list.
	 */
	public MetaMSEcorrelateTask(final MZmineProject project, final ParameterSet parameterSet, final PeakList[] peakLists) {
		this.project = project;
		this.peakLists = peakLists;
		parameters = parameterSet;
		
		finishedRows = 0;
		totalRows = 0;

		// sample groups parameter
		groupingParameter = (String) parameters.getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).getValue();
		// tolerances
		rtTolerance = parameterSet.getParameter(MetaMSEcorrelateParameters.RT_TOLERANCE).getValue();
		mzTolerance = parameterSet.getParameter(MetaMSEcorrelateParameters.MZ_TOLERANCE).getValue();

		// filter
		// start with high abundant features >= mainPeakIntensity
		// In this way we directly filter out groups with no abundant features
		// fill in smaller features after
		mainPeakIntensity = parameterSet.getParameter(MetaMSEcorrelateParameters.MAIN_PEAK_HEIGHT).getValue();
		// by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
		percContainedInSamples = parameterSet.getParameter(MetaMSEcorrelateParameters.MIN_SAMPLES).getValue();
		// intensity correlation across samples
		minIntensityProfileR = parameterSet.getParameter(MetaMSEcorrelateParameters.MIN_R_I_PROFILE).getValue();
		// feature shape correlation 
		minShapeCorrR = parameterSet.getParameter(MetaMSEcorrelateParameters.MIN_R_SHAPE_INTRA).getValue();
		noiseLevelShapeCorr = parameterSet.getParameter(MetaMSEcorrelateParameters.NOISE_LEVEL_PEAK_SHAPE).getValue();
		// min of 3! TODO
		minCorrelatedDataPoints = parameterSet.getParameter(MetaMSEcorrelateParameters.MIN_DP_CORR_PEAK_SHAPE).getValue();

		// TODO: maybe search for isotopes first hard coded and filter by isotope pattern
		searchAdducts = parameterSet.getParameter(MetaMSEcorrelateParameters.SEARCH_ADDUCTS).getValue(); 
		isPositive = parameterSet.getParameter(MetaMSEcorrelateParameters.POSITIVE_MODE).getValue(); 
		useAdductBonusR = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_BONUSR).getValue(); 
		adductBonusR = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCT_BONUSR).getEmbeddedParameter().getValue(); 
		maxMolecules = parameterSet.getParameter(MetaMSEcorrelateParameters.MAX_MOLECULES).getValue(); 
		maxCombinations = parameterSet.getParameter(MetaMSEcorrelateParameters.MAX_COMBINATION).getValue(); 
		maxCharge = parameterSet.getParameter(MetaMSEcorrelateParameters.MAX_CHARGE).getValue(); 
		maxMods = parameterSet.getParameter(MetaMSEcorrelateParameters.MAX_MODS).getValue(); 

		selectedAdducts = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCTS).getValue()[0]; 
		selectedMods = parameterSet.getParameter(MetaMSEcorrelateParameters.ADDUCTS).getValue()[1]; 

		createAllAdducts(isPositive, maxMolecules, maxCombinations, maxCharge);
	}


	/**
	 * only for adduct testing
	 */
	public MetaMSEcorrelateTask(boolean pos, ESIAdductType[] selectedAdducts, ESIAdductType[] selectedMods, int maxMolecules,
			int maxCombinations, int maxCharge, int maxMods) {
		super();
		this.selectedAdducts = selectedAdducts;
		this.selectedMods = selectedMods;
		this.maxMolecules = maxMolecules;
		this.maxCombinations = maxCombinations;
		this.maxCharge = maxCharge;
		this.maxMods = maxMods;
		this.project = null;
		this.peakLists = null;
		parameters = null;

		finishedRows = 0;
		totalRows = 0;
		groupingParameter = null;
		rtTolerance = null;
		mzTolerance = null;

		// filter
		// start with high abundant features >= mainPeakIntensity
		// In this way we directly filter out groups with no abundant features
		// fill in smaller features after
		mainPeakIntensity = 0;
		// by min percentage of samples in a sample set that contain this feature MIN_SAMPLES
		percContainedInSamples = 0;
		// intensity correlation across samples
		minIntensityProfileR = 0;
		// feature shape correlation 
		minShapeCorrR = 0;
		noiseLevelShapeCorr = 0;
		// min of 3! TODO
		minCorrelatedDataPoints = 0;

		// TODO: maybe search for isotopes first hard coded and filter by isotope pattern
		searchAdducts = true; 
		isPositive = pos; 
		useAdductBonusR = true; 
		adductBonusR = 0; 

		createAllAdducts(isPositive, maxMolecules, maxCombinations, maxCharge);
	}


	@Override
	public double getFinishedPercentage() {
		return progress;
	}

	@Override
	public String getTaskDescription() {
		return "Identification of groups in " + peakLists.length + " scan events (lists)" ;
	}

	@Override
	public void run() { 
		setStatus(TaskStatus.PROCESSING);
		LOG.info("Starting MSE correlation search in " + peakLists.length + " scan events (lists)"); 
		try {  
			groupedPKL = new MSEGroupedPeakList[peakLists.length];
			for(int e=0; e<peakLists.length; e++) {
				// create new PKL for grouping
				groupedPKL[e] = new MSEGroupedPeakList(peakLists[e].getRawDataFiles(), peakLists[e]);
				// find groups and size
				setSampleGroups(groupedPKL[e]);	 
				groupedPKL[e].setSampleGroupsParameter(sgroupPara);
				groupedPKL[e].setSampleGroups(sgroupSize); 
				
				// go through all samples in one peakList
				PKLRowGroupList groups;
				// group the features of one scan event of all samples
				// by RT and r-Pearson (intra)
				groups = groupFeatures(groupedPKL[e]);
				if(groups!=null) {
					// set groups to pkl
					groupedPKL[e].setGroups(groups);
					// add to project 
					project.addPeakList(groupedPKL[e]);

					// do deisotoping
					deisotopeGroups();
					// do adduct search
					// searchAdducts();
				}
			}
			// if events>1
			if(peakLists.length>1) {
				// combine groups across events by RT and r-Pearson (inter event)
				combineGroups();

				// search for in source fragments by profile in MS1 and MSE
				findInSourceFragments();
			}

			if (!isCanceled()) {
				for(int e=0; e<peakLists.length; e++) {
					// Add task description to peakList.
					peakLists[e].addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
							"Identification of adducts", parameters));

					// Repaint the window to reflect the change in the peak list
					Desktop desktop = MZmineCore.getDesktop();
					if (!(desktop instanceof HeadLessDesktop))
						desktop.getMainWindow().repaint();

					// Done.
					setStatus(TaskStatus.FINISHED);
					LOG.info("Finished adducts search in " + peakLists[e]);
				}
			}
		} catch (Throwable t) {

			LOG.log(Level.SEVERE, "Adduct search error", t);
			setStatus(TaskStatus.ERROR);
			setErrorMessage(t.getMessage());
		}
	}


	/**
	 * gets called to initialise variables for next peaklist
	 * @param peakList
	 */
	private void setSampleGroups(PeakList peakList) {
		if(groupingParameter==null || groupingParameter.length()==0) {
			this.sgroupSize = null;
			hasToFilterMinFInSampleSets = false;
			return;
	}
		else {
			HashMap<Object, Integer> sgroupSize = new HashMap<Object, Integer>();
			
	
			UserParameter<?, ?> params[] = project.getParameters();
			for (UserParameter<?, ?> p : params) {
				if (groupingParameter.equals(p.getName())) {
					// save parameter for sample groups
					sgroupPara = p;
					break; 
				}
			}
			int max = 0;
			// calc size of sample groups
			for (RawDataFile file : peakList.getRawDataFiles()) {
				String parameterValue = sgroupOf(file);
	
				Integer v = sgroupSize.get(parameterValue);
				int val = v==null? 0 : v;
				sgroupSize.put(parameterValue, val+1); 
				if(val+1>max) 
					max = val+1;
			} 
			this.sgroupSize = sgroupSize;
			// has to filter minimum samples with a feature in a set?
			// only if sample set has to contain more than one sample with a feature
			hasToFilterMinFInSampleSets = ((int)(max*percContainedInSamples))>1;
		}
	}

	/**
	 * 
	 * @param file
	 * @return sample group value of raw file
	 */
	private String sgroupOf(RawDataFile file) { 
		return String.valueOf(project.getParameterValue(sgroupPara, file));
	}

	/**
	 * First step is to bin the samples by average rt
	 * tolerance is higher to get all samples in the same group
	 * creates all groups possible
	 */ 
	/*
	private PKLRowGroupList groupFeatures1(PeakList peakList) {
		PeakListRow rows[] = peakList.getRows();
		totalRows = rows.length;

		if(totalRows>0) {
			// sort by RT of raw file peak not average!
			Arrays.sort(rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));

			// store groups in array
			PKLRowGroupList groups = new PKLRowGroupList();

			// group by larger tolerance for average RT 
			PKLRowGroup lastG = null;

			double tol = rtTolerance.getTolerance()*2;
			RTTolerance avrtTolerace = new RTTolerance(rtTolerance.isAbsolute(), tol);
			// start with feature one and create a new group every time the distance is to high
			for (int i = 0; i < totalRows; i++) {
				PeakListRow row = rows[i]; 
				// new group?
				if(lastG==null || !avrtTolerace.checkWithinTolerance(
						lastG.getCenterAVRT(), row.getAverageRT())){

					PKLRowGroup newG = new PKLRowGroup(row.getAverageRT()+tol);
					// add previous features to new group?
					if(lastG!=null) {
						for(PeakListRow old : lastG) {
							// check current feature of new group with old ones
							if(avrtTolerace.checkWithinTolerance(row.getAverageRT(), old.getAverageRT()))
								newG.add(0,old); 
						}
					}
					// set center of new group if some features were added
					if(newG.size()>0) {
						newG.setCenterAVRT(newG.firstElement().getAverageRT()+tol);
					}
					groups.add(newG);
					lastG = newG;
				}

				// add feature to current group
				lastG.add(row);
			} 
			return groups;
		}
		return null;
	}
	 */
	/**
	 * build a groups list with only intense features
	 * because they are showing higher correlation
	 * check correlation to high features in range -> same/different group
	 * -> can be inserted in multiple groups
	 * has to run 2 times for inserting first features in multiple groups
	 * 
	 * insert low abundant features
	 * @param peakList
	 * @return
	 */
	private PKLRowGroupList groupFeatures(PeakList peakList) {
		PeakListRow rows[] = peakList.getRows();
		totalRows = rows.length;
		final RawDataFile raw[] = peakList.getRawDataFiles();

		if(totalRows>1) {
			// sort by avgRT
			Arrays.sort(rows, new PeakListRowSorter(SortingProperty.RT, SortingDirection.Ascending));

			// store groups in list
			PKLRowGroupList groups = new PKLRowGroupList();  

			// start with feature one and create a new group every time 
			// the distance is to high or there is no peak shape correlation
			// try to fit all features to all groups twice 
			// giving the first feature the chance of beeing correlated to all gorups
			progress = 0.05; 
			// for all rows
			for (int i = 0; i < totalRows; i++) {
				PeakListRow row = rows[i]; 
				// start with high abundant features
				// higher than mainPeakIntensity? contained in X % samples of a sample set?
				if(row.getBestPeak().getHeight()>=mainPeakIntensity && filterMinFeaturesInSampleSet(raw, row)) {
					// new group?
					if(groups.size()==0) {  
						groups.add(new PKLRowGroup(raw, row, 0)); 
					}
					else {
						boolean added = false;
						// check all groups for correlation with row
						for(int g=0; g<groups.size(); g++) {
							PKLRowGroup pg = groups.get(g);
							// check and add if row is correlated
							// TODO maybe add it to group but if its to far off - give false as a result -> also new group
							if(corrRowToGroup(peakList, raw, row, pg)) {
								pg.add(row);
								added = true;
							}
						}
						// not added? create new group 
						if(!added)
							groups.add(new PKLRowGroup(raw, row, groups.size()));  
					}
				}
			} 
			progress = 0.3; 

			//#########################################################################
			// SECOND GO: all high abundant rows again
			for (int i = 0; i < totalRows; i++) {
				PeakListRow row = rows[i]; 
				// start with high abundant features
				// higher than mainPeakIntensity? contained in X % samples of a sample set?
				if(row.getBestPeak().getHeight()>=mainPeakIntensity && filterMinFeaturesInSampleSet(raw, row)) { 
					// check all groups for correlation with row
					for(int g=0; g<groups.size(); g++) {
						PKLRowGroup pg = groups.get(g);
						if(!pg.contains(row)) {
							// correlate and add if row fits 
							if(corrRowToGroup(peakList, raw, row, pg)) {
								pg.add(row); 
							}
						}
					} 
				}
			}
			progress = 0.6; 

			// ######################################################################
			// now correlate and add low abundant features to groups 
			if(groups.size()>0) {
				progress = 0.65;
				double step = (0.90-0.65)/totalRows;
				for (int i = 0; i < totalRows; i++) {
					PeakListRow row = rows[i]; 
					// start with high abundant features
					// lower than mainPeakIntensity? contained in X % samples of a sample set?
					if(row.getBestPeak().getHeight()<mainPeakIntensity && filterMinFeaturesInSampleSet(raw, row)) { 
						// check all groups for correlation with row
						for(int g=0; g<groups.size(); g++) {
							PKLRowGroup pg = groups.get(g);
							// correlate and add if row fits
							// TODO maybe add it to group but if its to far off - give false as a result
							if(corrRowToGroup(peakList, raw, row, pg)) {
								pg.add(row); 
							}
						} 
					}
					progress += step;
				}
				progress = 0.90;
				return groups;
			} 
		}
		return null;
	}

	/**
	 * only keep rows which contain features in at least X % samples in a set
	 * called before starting row processing
	 * @param raw
	 * @param row
	 * @return
	 */
	private boolean filterMinFeaturesInSampleSet(final RawDataFile raw[], PeakListRow row) {
		// short cut if minimum is only one sample in a set
		if(!hasToFilterMinFInSampleSets || sgroupSize == null)
			return true;
		// is present in X % samples of a sample set?
		// count sample in groups (no feature in a sample group->no occurrence in map)
		HashMap<Object, Integer> counter = new HashMap<Object, Integer>(); 
		for (RawDataFile file : raw) {
			if (row.hasPeak(file)) {
				String sgroup = sgroupOf(file);
				if (counter.containsKey(sgroup)) {
					counter.put(sgroup, counter.get(sgroup) + 1);
				} else {
					counter.put(sgroup, 1);
				}
			}
		}
		// only go on if feature was present in X % of the samples
		for(Object sg : counter.keySet()) {
			if(counter.get(sg)<sgroupSize.get(sg)*percContainedInSamples)
				return false;
		} 
		return true;
	}

	/**
	 * correlation between row and group(every row of the group)
	 * @param row
	 * @param pg
	 * @return
	 */
	private boolean corrRowToGroup(final PeakList peakList, final RawDataFile raw[], PeakListRow row, PKLRowGroup pg) {
		// simple fast correlation to groups RT min/max/avg
		// direct exclusion for high level filtering
		// check rt of all peaks of all raw files
		for(int r=0; r<raw.length; r++) {
			Feature f = row.getPeak(raw[r]); 
			if(f!=null && pg.hasPeak(r)) {
				if(!pg.isInRange(r, f, rtTolerance))
					return false; 
			}
		}

		// correlation to all rows and all peaks of a row (feature)
		double minShapeR = 1, avgShapeR = 0;
		int c = 0;
		int adductsInGroup = 0;
		ESIAdductType[] lastAdductCombination = null;
		// for all rows in group
		for(PeakListRow row2 : pg) {
			// returns corr<=0 if conditions were not met
			try {
				FeatureShapeCorrelationData[] data = corrRowToRowFeatureShape(raw, row, row2);
				// for min max avg
				RowCorrelationData rowCorr = new RowCorrelationData(0, 0, 0, data);
				// has bad correlation: exit
				if(rowCorr.hasPeakShapeCorrelation() && rowCorr.getAvgPeakShapeR()<=0)
					return false;
				else if(rowCorr.hasPeakShapeCorrelation()) {
					// avg and min shape correlation:
					c++;
					avgShapeR += rowCorr.getAvgPeakShapeR();
					if(rowCorr.getMinPeakShapeR()<minShapeR)
						minShapeR = rowCorr.getMinPeakShapeR();
				}
				
				// search adducts and 13C isotopologues
				if(searchAdducts) {
					// Deisotoping went wrong?
					int absCharge = AlignedIsotopeGrouperTask.find13CIsotope(peakList, row, row2, maxCharge, mzTolerance); 
					boolean isIsotope = absCharge!=-1; 
					// search for adducts and add correlation: IProfile doesnt have to be the same for adducts
					boolean isAdduct = false;
					if(!isIsotope) {
						ESIAdductType[] ad = findAdducts(peakList, row2, row, row2.getRowCharge(), row.getRowCharge());
						if(ad!=null) {
							isAdduct = true;
							lastAdductCombination = ad;
						}
					}
					adductsInGroup += isAdduct || isIsotope? 1 : 0;
				}
				
			} catch (Exception e) { 
				e.printStackTrace(); 
				LOG.log(Level.SEVERE, "Error: "+e.getLocalizedMessage(), e);
				return false;
			}
		} 
		// calculate combined correlation
		if(c!=0) {
			double corr = avgShapeR/c;
			if(useAdductBonusR) corr += adductBonusR * adductsInGroup;
			return corr>=minShapeCorrR;
		}
		else {
			int max = Math.max(maxCombinations, maxCharge) + 1;
			return adductsInGroup>1 || (lastAdductCombination!=null 
					&& lastAdductCombination[0].isMainAdduct(max) 
					&& lastAdductCombination[1].isMainAdduct(max));
		}
	}

	/**
	 * correlates one row to another
	 * peak shape correlation (Pearson) (if negative or zero direct escape)
	 * 		otherwise avg(corr)>= minCorrPeakShape
	 * intensity profile correlation (Pearson) >= minCorrIProfile
	 * 		TODO: maybe impute low values instead of 0 for not detected!
	 * @param row
	 * @param g
	 * @return average correlation over both factors ranging from minR to 1 (or 0 if minimum correlation was not met)
	 * @throws Exception 
	 */
	private double corrRowToRow(final PeakList peakList, final RawDataFile raw[], PeakListRow row, PeakListRow row2) throws Exception {
		double corr = 0;
		SimpleRegression reg = new SimpleRegression();
		// count
		int c = 0;
		// go through all raw files 
		for(int r=0; r<raw.length; r++) {
			Feature f1 = row.getPeak(raw[r]);
			Feature f2 = row2.getPeak(raw[r]);
			if(f1!=null && f2!=null) {
				// peak shape correlation
				FeatureShapeCorrelationData cFS = corrFeatureShape(f1,f2,true);
				if(cFS!=null) {
					double tmpcorr = cFS.getR();
					// escape if peak shapes are showing a negative correlation
					if(tmpcorr<=0)
						return tmpcorr;
					corr += tmpcorr;
					c++;
				}
				else {
					// correlation was not possible
					// maybe due to a small peak in this raw file
					// escape if features would be high enough for a correlation
					// this means the features are not intercepting
					if(countDPHigherThanNoise(f1)>=minCorrelatedDataPoints && countDPHigherThanNoise(f2)>=minCorrelatedDataPoints)
						return 0;
				}
			} 
			// I profile correlation
			// TODO: low value imputation?
			double I1 = f1!=null? f1.getHeight() : 0;
			double I2 = f2!=null? f2.getHeight() : 0;
			reg.addData(I1, I2);
		}
		// First search for isotopes TODO later fill in isotopes from raw
		int absCharge = AlignedIsotopeGrouperTask.find13CIsotope(peakList, row, row2, maxCharge, mzTolerance); 
		boolean isIsotope = absCharge!=-1; 
		// TODO search for adducts and add correlation: IProfile doesnt have to be the same for adducts
		boolean isAdduct = false;
		if(!isIsotope) findAdducts(peakList, row, row2, row.getRowCharge(), row2.getRowCharge());
		double adductBonus = (isIsotope || isAdduct) && useAdductBonusR? adductBonusR : 0;
		// TODO weighting of intensity corr and feature shape corr
		// there was no correlation possible due to small peaks
		if(c==0) {
			// 
			return isAdduct || isIsotope? 1 : 0;
		}
		else {
			corr = (corr/c);
			double corrIprofile = reg.getR();
			if(corr+adductBonus<minShapeCorrR) return 0;
			else if(corrIprofile<minIntensityProfileR) return 0;
			else return (corr+corrIprofile)/2;
		}
	}

	/**
	 * correlates the height profile of one row to another
	 * NO escape routine
	 * @param raw
	 * @param row
	 * @param g
	 * @return Pearson r of height correlation
	 */
	public static double corrRowToRowIProfile(final RawDataFile raw[], PeakListRow row, PeakListRow g) {
		SimpleRegression reg = new SimpleRegression();
		// go through all raw files 
		for(int r=0; r<raw.length; r++) {
			Feature f1 = row.getPeak(raw[r]);
			Feature f2 = g.getPeak(raw[r]);
			// I profile correlation
			// TODO: low value imputation?
			double I1 = f1!=null? f1.getHeight() : 0;
			double I2 = f2!=null? f2.getHeight() : 0;
			reg.addData(I1, I2);
		}
		// TODO weighting of intensity corr
		double corrIprofile = reg.getR();
		return corrIprofile;
	} 
	/**
	 * correlation of row to row peak shape
	 * NO escape routine
	 * @param raw raw files used to construct this peak list
	 * @param row
	 * @param g
	 * @return feature shape correlation as an array[rawFiles] 
	 * or null if one feature has no peak in a raw file
	 * or an empty correlation (corr.regression==null) if not
	 * not enough data points for a correlation
	 * @throws Exception
	 */
	public static FeatureShapeCorrelationData[] corrRowToRowFeatureShape(final RawDataFile raw[], PeakListRow row, PeakListRow g) throws Exception {
		FeatureShapeCorrelationData[] corrData = new FeatureShapeCorrelationData[raw.length];
		// go through all raw files 
		for(int r=0; r<raw.length; r++) {
			Feature f1 = row.getPeak(raw[r]);
			Feature f2 = g.getPeak(raw[r]);
			if(f1!=null && f2!=null) {
				// peak shape correlation
				corrData[r] = corrFeatureShape(f1,f2, true);
			} 
			else {
				corrData[r] = null;
			}
		}
		return corrData;
	}

	/**
	 * feature shape correlation
	 * @param f1
	 * @param f2
	 * @return feature shape correlation 
	 * or null if not possible
	 * not enough data points for a correlation
	 * @throws Exception 
	 */
	public static FeatureShapeCorrelationData corrFeatureShape(Feature f1, Feature f2, boolean sameRawFile) throws Exception { 
		//Range<Double> rt1 = f1.getRawDataPointsRTRange();
		//Range<Double> rt2 = f2.getRawDataPointsRTRange();
		if(sameRawFile) {
			// scan numbers (not necessary 1,2,3...)
			int[] sn1 = f1.getScanNumbers();
			int[] sn2 = f2.getScanNumbers();
			int offsetI1 = 0;
			int offsetI2 = 0;
			// find corresponding value
			if(sn2[0]>sn1[0]) {
				for(int i=1; i<sn1.length; i++) {
					if(sn1[i]==sn2[0]) {
						offsetI1 = i;
						break;
					}
				}
				// peaks are not overlapping
				if(offsetI1==0)
					return null;
			}
			if(sn2[0]<sn1[0]) {
				for(int i=1; i<sn2.length; i++) {
					if(sn1[0]==sn2[i]) {
						offsetI2 = i;
						break;
					}
				}
				// peaks are not overlapping
				if(offsetI2==0)
					return null;
			}
			// only correlate intercepting areas 0-max
			int max = 0;
			if(sn1.length-offsetI1<=sn2.length-offsetI2)
				max = sn1.length-offsetI1;
			if(sn1.length-offsetI1>sn2.length-offsetI2)
				max = sn2.length-offsetI2;
			if(max-offsetI1>minCorrelatedDataPoints && max-offsetI2>minCorrelatedDataPoints) {
				RawDataFile raw = f1.getDataFile();
				SimpleRegression reg = new SimpleRegression();
				
				// save max and min of intensity of val1(x)
				double maxX = 0;
				double minX = Double.POSITIVE_INFINITY;
				Vector<Double> I1 = new Vector<Double>();
				Vector<Double> I2 = new Vector<Double>();
				// add all data points over a given threshold
				// raw data (not smoothed)
				for(int i= 0; i<max; i++) {
					if(sn1[i+offsetI1]!=sn2[i+offsetI2])
						throw new Exception("Scans are not the same for peak shape corr");
					
					DataPoint dp = f1.getDataPoint(sn1[i+offsetI1]);
					DataPoint dp2 = f2.getDataPoint(sn2[i+offsetI2]);
					if(dp!=null && dp2!=null) {
						// raw data
						double val1 = dp.getIntensity();
						double val2 = dp2.getIntensity();
						
						if(val1>=noiseLevelShapeCorr && val2>=noiseLevelShapeCorr) {
							reg.addData(val1, val2);
							if(val1<minX) minX = val1;
							if(val1>maxX) maxX = val1;
							I1.add(val1);
							I2.add(val2);
						}
					}
				}
				// return pearson r
				if(reg.getN()>=minCorrelatedDataPoints) { 
					Double[][] data = new Double[][]{I1.toArray(new Double[I1.size()]),I2.toArray(new Double[I2.size()])};
					return new FeatureShapeCorrelationData(reg, data, minX, maxX);
				}
			} 
		}
		else {
			// TODO if different raw file search for same rt
			// impute rt/I values if between 2 data points
		}
		return null;
	}

	/**
	 * counts all data points >= noiseLevel
	 * @param f2
	 * @return
	 */
	private double countDPHigherThanNoise(Feature f) { 
		int c = 0;
		for(int i=0; i<f.getScanNumbers().length; i++) { 
			double val = f.getDataPoint(f.getScanNumbers()[i]).getIntensity(); 
			if(val>=noiseLevelShapeCorr)
				c++;
		}
		return c;
	}

	/**
	 * 1. Check for specific isotopes:
	 * 2. 13C
	 * 		1. Check group for isotopes
	 * 		2. Check raw data for isotopes
	 */
	private void deisotopeGroups() {
		// TODO Auto-generated method stub

	}

	/**
	 * Combine groups across different scan events
	 */
	private void combineGroups() {
		// TODO Auto-generated method stub

	}


	/**
	 * find in source fragments based on intensity profile in MS1 and MSE scans
	 * 
	 */
	private void findInSourceFragments() {
		// TODO Auto-generated method stub

	}

	/**
	 *
	 * @param mainRow
	 *            main peak.
	 * @param possibleAdduct
	 *            candidate adduct peak.
	 */
	private ESIAdductType[] findAdducts(final PeakList peakList, final PeakListRow row1, final PeakListRow row2, final int z1, final int z2) {
		// check all combinations of adducts
		for (final ESIAdductType adduct : allAdducts) {
			for (final ESIAdductType adduct2 : allAdducts) {
				// for one adduct use a maximum of 1 modification
				// second can have <=maxMods
				if (!adduct.equals(adduct2) && !(adduct.getModCount()>1 && adduct2.getModCount()>1)) {
					// check charge state if absCharge is not -1 or 0
					if((z1<=0 || adduct.getAbsCharge()== z1) && (z2<=0 || adduct2.getAbsCharge()== z2)) {
						// checks each raw file - only true if all m/z are in range
						if(checkAdduct(peakList, row1, row2, adduct, adduct2)) { 
							// is a2 a modification of a1? (same adducts - different mods
							if(adduct2.isModificationOf(adduct)) { 
								adduct2.subtractMods(adduct).addAdductIdentityToRow(row2, row1);
								MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row2, false);
							}
							else if(adduct.isModificationOf(adduct2)) {
								adduct.subtractMods(adduct2).addAdductIdentityToRow(row1, row2);
								MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row1, false);
							}
							else {
								// Add adduct identity and notify GUI.
								// only if not already present
								adduct.addAdductIdentityToRow(row1, row2);
								MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row1, false);
								adduct2.addAdductIdentityToRow(row2, row1);
								MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row2, false);
							}
							// there can only be one hit for a row-row comparison
							return new ESIAdductType[]{adduct, adduct2};
						}
					}
				}
			} 
		}
		// no adduct to be found
		return null;
	}

	/**
	 * Check if candidate peak is a given type of adduct of given main peak.
	 * is not checking retention time (has to be checked before)
	 * @param mainPeak
	 *            main peak.
	 * @param possibleAdduct
	 *            candidate adduct peak.
	 * @param adduct
	 *            adduct.
	 * @return true if mass difference, retention time tolerance and adduct peak
	 *         height conditions are met.
	 */
	private boolean checkAdduct(final PeakList peakList, final PeakListRow row1,
			final PeakListRow row2, final ESIAdductType adduct, final ESIAdductType adduct2) {
		// for each peak[rawfile] in row
		boolean hasCommonPeak = false;
		//
		for(RawDataFile raw : peakList.getRawDataFiles()) {
			Feature f1 = row1.getPeak(raw);
			Feature f2 = row2.getPeak(raw);
			if(f1!=null && f2!=null) {
				hasCommonPeak = true;
				double mz1 = ((f1.getMZ()*adduct.getAbsCharge())-adduct.getMassDifference())/adduct.getMolecules();
				double mz2 = ((f2.getMZ()*adduct2.getAbsCharge())-adduct2.getMassDifference())/adduct2.getMolecules();
				if(!mzTolerance.checkWithinTolerance(mz1,mz2))
					return false;
			}
		}
		// directly returns false if not in range
		// so if has common peak = isAdduct
		return hasCommonPeak;
	}

	/**
	 * create all possible adducts
	 */
	private void createAllAdducts(boolean positive, int maxMolecules, int maxCombination, int maxCharge) {
		// normal adducts
		for(ESIAdductType a : selectedAdducts)
			if((a.getCharge()>0 && positive) || (a.getCharge()<0 && !positive))
				allAdducts.add(a);
		// add or remove H from multi charged (Fe2+)
		// addRemoveHydrogen(positive);
		// combined adducts
		if(maxCombination>1) {
			combineAdducts(allAdducts, selectedAdducts, new Vector<ESIAdductType>(allAdducts), maxCombination, 1, false);
			for(int i=0; i<allAdducts.size(); i++){
				if(allAdducts.get(i).getAbsCharge()>maxCharge) {
					allAdducts.remove(i);
					i--;
				}
			} 	
		}
		// add or remove H from multi charged (Fe2+)
		addRemoveHydrogen(positive);
		// add modification
		addModification();
		// multiple molecules
		addMultipleMolecules(maxMolecules); 
		// print them out
		for(ESIAdductType a : allAdducts)
			System.out.println(a.toString());
	}

	/**
	 * adds modification to the existing adducts
	 */
	private void addModification() {
		// normal mods 
		for(ESIAdductType a : selectedMods)
				allModification.add(a);
		// combined modification
		combineAdducts(allModification, selectedMods, new Vector<ESIAdductType>(allModification), maxMods, 1, true);
		// add new modified adducts
		int size = allAdducts.size();
		for(int i=0; i<size; i++) {
			ESIAdductType a = allAdducts.get(i);
			// all mods
			for(ESIAdductType mod : allModification) {
				allAdducts.add(ESIAdductType.createModified(a, mod));
			}
		}
	}

	private void addMultipleMolecules(int maxMolecules) { 
		int size = allAdducts.size();
		for(int k=0; k<size; k++) {
			ESIAdductType a = allAdducts.get(k);
			for(int i=2; i<=maxMolecules; i++) {
				allAdducts.add(new ESIAdductType(a));
				allAdducts.lastElement().setMolecules(i);
			}
		}
	}

	/**
	 * does not check maxCharge-delete afterwards
	 * @param adducts
	 * @param maxCombination
	 * @param maxCharge
	 * @param run init with 1
	 */
	private void combineAdducts(Vector<ESIAdductType> targetList, ESIAdductType[] selectedList, final Vector<ESIAdductType> adducts, 
			int maxCombination, int run, boolean zeroChargeAllowed) {
		Vector<ESIAdductType> newAdducts = new Vector<ESIAdductType>(); 
		for(int i=0; i<adducts.size(); i++) {
			ESIAdductType a1 = adducts.get(i);
			for(int k=0; k<selectedList.length; k++) {
				ESIAdductType a2 = selectedList[k]; 
				ESIAdductType na = new ESIAdductType(a1, a2);
				if((zeroChargeAllowed || na.getCharge()!=0)  && !isContainedIn(targetList, na)) {
					newAdducts.add(na);
					targetList.add(na);
				}
			}
		}
		// first run = combination of two
		if(run+1<maxCombination) {
			combineAdducts(targetList, selectedList, newAdducts, maxCombination, run+1, zeroChargeAllowed);
		}
	}

	private boolean isContainedIn(Vector<ESIAdductType> adducts, ESIAdductType na) {
		for(ESIAdductType a : adducts) {
			if(a.sameMathDifference(na))
				return true;
		}
		return false;
	} 

	/**
	 * add or remove hydrogen to obtain more adduct types
	 * also to all positive adducts
	 * @param positive
	 */
	private void addRemoveHydrogen(boolean positive) {
		ESIAdductType H = ESIAdductType.H;
		ESIAdductType Hneg = ESIAdductType.H_NEG;
		// remove/add hydrogen from double charged ones to get single charge
		// example: M+Fe]2+ will be M+Fe-H]+
		for(int i=0; i<allAdducts.size(); i++) {
			ESIAdductType a = allAdducts.get(i);
			for(int z = a.getAbsCharge(); z>1; z--) { 
				// positive remove H ; negative add H
				ESIAdductType tmpA = new ESIAdductType(a, positive? Hneg : H);
				if(!isContainedIn(allAdducts, tmpA))
					allAdducts.add(tmpA);
				a = tmpA;
			} 
		}
		// find !positve selectedAdducts and 
		// add/remove as many H as possible
		for(int i=0; i<selectedAdducts.length; i++) {
			ESIAdductType a = selectedAdducts[i];
			// adduct has a different charge state than MS mode
			if(((a.getCharge()>0) != positive)) {
				// add/remove H to absCharge == 1 (+- like positive)
				ESIAdductType[] start = new ESIAdductType[a.getAbsCharge()+2];
				start[0] = a;
				for(int k=1; k<start.length; k++)
					start[k] = positive? H : Hneg;
				a = new ESIAdductType(start);
				if(!isContainedIn(allAdducts, a))
					allAdducts.add(a);
				// loop runs:
				for(int z = 2; z<=maxCharge; z++) { 
					ESIAdductType tmpA = new ESIAdductType(a, positive? H : Hneg);
					if(!isContainedIn(allAdducts, tmpA))
						allAdducts.add(tmpA);
					a = tmpA;
				}
			}
		}
	}
}
