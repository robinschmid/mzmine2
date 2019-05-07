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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.masslistmethods.recalibrate;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleMassList;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class RecalibrateMassListsTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private MZmineProject project;
	private RawDataFile dataFile;

	// scan counter
	private int processedScans = 0, totalScans;
	private ScanSelection scanSelection;
	private Scan[] scans;

	// User parameters
	private String suffix, massListName;
	private MZTolerance mzTolerance;
	private double minimumHeight;

	/**
	 * @param dataFile
	 * @param parameters
	 */
	public RecalibrateMassListsTask(MZmineProject project, RawDataFile dataFile,
			ParameterSet parameters) {

		this.project = project;
		this.dataFile = dataFile;
		this.scanSelection = parameters
				.getParameter(RecalibrateMassListsParameters.scanSelection)
				.getValue();
		this.massListName = parameters
				.getParameter(RecalibrateMassListsParameters.massList)
				.getValue();

		this.mzTolerance = parameters
				.getParameter(RecalibrateMassListsParameters.mzTolerance)
				.getValue();
		this.minimumHeight = parameters
				.getParameter(RecalibrateMassListsParameters.minimumHeight)
				.getValue();

		this.suffix = parameters
				.getParameter(RecalibrateMassListsParameters.suffix).getValue();

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Recalibrating mass list for " + dataFile;
	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
	 */
	public double getFinishedPercentage() {
		if (totalScans == 0)
			return 0;
		else
			return (double) processedScans / totalScans;
	}

	public RawDataFile getDataFile() {
		return dataFile;
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		logger.info("Started recalibration on " + dataFile);

		scans = scanSelection.getMatchingScans(dataFile);
		int allScanNumbers[] = scanSelection.getMatchingScanNumbers(dataFile);
		totalScans = scans.length;


		// list of lock masses - sorted by mz
		LockMass[] locks = new LockMass[] {new LockMass(448.1410, "0"), new LockMass(580.1090, "1"), new LockMass(716.1248, "2"), new LockMass(741.5306, "tissue1"), new LockMass(852.1400, "3")};
		
		for (Scan scan : scans) {

			if (isCanceled())
				return;

			MassList massList = scan.getMassList(massListName);
			if (massList == null) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Scan " + dataFile + " #" + scan.getScanNumber()
				+ " does not have a mass list " + massListName);
				return;
			}

			// sorted by mz!
			DataPoint mzValues[] = massList.getDataPoints();

			if (mzValues == null) {
				setStatus(TaskStatus.ERROR);
				setErrorMessage("Mass list " + massListName
						+ " does not contain m/z values for scan #"
						+ scan.getScanNumber() + " of file " + dataFile);
				return;
			}
			
			// difference to the lock masses (Double.NaN if not found or multiple suitable mz in scan)
			double[] diff = new double[locks.length];
			double[] suitableMZ = new double[locks.length];
			int currentLock = 0;
			// for all data points
			try {
				// find lock masses and error to exact mass
				for (int i = 0; i < mzValues.length && currentLock<locks.length; i++) {
					DataPoint dp = mzValues[i];
					if(dp.getIntensity()>=minimumHeight && mzTolerance.checkWithinTolerance(locks[currentLock].getMz(), dp.getMZ())) {
						diff[currentLock] = dp.getMZ()-locks[currentLock].getMz();
						suitableMZ[currentLock]++;
					}
					else if(locks[currentLock].getMz()<dp.getMZ()) {
						// do not use this lock mass if more than 1 suitingLockMZ 
						if(suitableMZ[currentLock]!=1)
							diff[currentLock] = Double.NaN;

						// dp MZ is out of the upper range 
						// move to next lock mass
						currentLock++;
					}
				}
				
				// print lock mass errors
				NumberFormat f = new DecimalFormat("#0.0000");
				NumberFormat f2 = new DecimalFormat("#0.0");
				for (int i = 0; i < locks.length; i++) {
					if(Double.isNaN(diff[i]))
						System.out.println(locks[i]+ (suitableMZ[i]==0? " not found" : "found "+suitableMZ[i]+" times"));
					else System.out.println("mz="+locks[i].getMz()+"  Delta="+f.format(diff[i])+"  ppm="+f2.format((diff[i]/locks[i].getMz())*1000000));
				} 

				// new array of datapoints for
				DataPoint newValues[] = new DataPoint[mzValues.length];
				
				// apply lock mass differences to all masses
				// the two lock masses that effect the current mass
				int firstLock = -1, lastLock = 0; 
				while(lastLock<diff.length && Double.isNaN(diff[lastLock]))
					lastLock++;

				if(lastLock>locks.length-1)
					lastLock = -1;
				
				for (int i = 0; i < mzValues.length; i++) {
					DataPoint dp = mzValues[i];
					
					// if > than last turn last to first and get next lock
					if(lastLock!=-1 && dp.getMZ()>locks[lastLock].getMz()) {
						firstLock = lastLock;
						lastLock++;
						while(lastLock<diff.length && Double.isNaN(diff[lastLock]))
							lastLock++;
						
						if(lastLock>locks.length-1)
							lastLock = -1;
					}
					
					// shift
					// one sided or two sided?
					double factor = firstLock==-1? 1 : lastLock==-1? 0 : 
						(dp.getMZ()-locks[firstLock].getMz())/(locks[lastLock].getMz()-locks[firstLock].getMz());
					
					double dmz = 0;
					if(firstLock!=-1)
						dmz = diff[firstLock]*(1-factor);
					if(lastLock!=-1)
						dmz = diff[lastLock]*(factor);
					
					// shift
					newValues[i] = new SimpleDataPoint(dp.getMZ()-dmz, dp.getIntensity());
				}

				// create new mass list
				MassList newMassList = new SimpleMassList(massListName+suffix, scan, newValues);
				scan.addMassList(newMassList);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			processedScans++;
		}

		setStatus(TaskStatus.FINISHED);

		logger.info("Finished chromatogram builder on " + dataFile);

	}


	private void addZeros(TreeMap<Integer, Double> signals) {
		// add zeros within half of minimum spacing
		Iterator<Entry<Integer, Double>> it = signals.entrySet().iterator();
		if(it.hasNext()) {
			// temp map
			TreeMap<Integer, Double> tmp = new TreeMap<Integer, Double>();
			//
			Entry<Integer, Double> last = it.next();
			for (int i = 1; i < signals.size() && it.hasNext(); i++) {
				Entry<Integer, Double> e = it.next();
				// is the spacing higher than 1 significance?
				// the key is the mz value times a factor
				if(e.getKey()-last.getKey()>2) {
					// add end of peak and start of peak
					tmp.put(last.getKey()+1, 0.0);
					tmp.put(e.getKey()-1, 0.0);
				}
				else if(e.getKey()-last.getKey()>1) {
					// add separation between two values that are only separated by 1
					tmp.put(last.getKey()+1, 0.0);
				}
				last = e;
			}

			// add all
			signals.putAll(tmp);
		}
	}

}
