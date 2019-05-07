/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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

package net.sf.mzmine.MyStuff.MyModule.imaging;

import java.util.logging.Logger;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 *
 */
public class ImagingTask extends AbstractTask {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	private PeakList[] peakLists;

	// scan counter
	private int processedScans = 0, totalScans;
	private int newPeakID = 1;
	private int[] scanNumbers;

	// User parameters
	private String suffix;
	private MZTolerance mzTolerance;
	private double minimumTimeSpan, minimumHeight;

	private SimplePeakList newPeakList;

	/**
	 * @param dataFile
	 * @param parameters
	 */
	public ImagingTask(PeakList[] peakList, ParameterSet parameters) {

		this.peakLists = peakList; 


		this.suffix = parameters.getParameter(ImagingParameters.suffix).getValue();

	}

	/**
	 * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
	 */
	public String getTaskDescription() {
		return "Detecting chromatograms in " + peakLists;
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

	public PeakList[] getPeakLists() {
		return peakLists;
	}

	/**
	 * @see Runnable#run()
	 */
	public void run() {

		setStatus(TaskStatus.PROCESSING);

		logger.info("Started imaging builder on " + peakLists);
		
		// Process each peaklist as Imageline 
		for(PeakList peakList : peakLists) {
			// Process each row.
			for(final PeakListRow row : peakList.getRows()) {   
	
					// Process each peak.
					for (final Feature peak : row.getPeaks()) { 
						
					}
			}
		}

/*
		// Add the chromatograms to the new peak list
		for (Feature finishedPeak : chromatograms) {
			SimplePeakListRow newRow = new SimplePeakListRow(newPeakID);
			newPeakID++;
			newRow.addPeak(peakLists, finishedPeak);
			newPeakList.addRow(newRow);
		}
*/

		// Add new peaklist to the project

		setStatus(TaskStatus.FINISHED);

		logger.info("Finished chromatogram builder on " + peakLists);

	}

}
