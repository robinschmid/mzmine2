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

package net.sf.mzmine.modules.masslistmethods.imagebuilder;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.masslistmethods.chromatogrambuilder.Chromatogram;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.ScanSelection;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakSorter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class ImageBuilderTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private MZmineProject project;
    private RawDataFile dataFile;

    // scan counter
    private int processedScans = 0, totalScans;
    private ScanSelection scanSelection;
    private int newPeakID = 1;
    private Scan[] scans;

    // User parameters
    private String suffix, massListName;
    private MZTolerance mzTolerance;
    private double minimumHeight;

    private SimplePeakList newPeakList;

    /**
     * @param dataFile
     * @param parameters
     */
    public ImageBuilderTask(MZmineProject project, RawDataFile dataFile,
            ParameterSet parameters) {

        this.project = project;
        this.dataFile = dataFile;
        this.scanSelection = parameters
                .getParameter(ImageBuilderParameters.scanSelection)
                .getValue();
        this.massListName = parameters
                .getParameter(ImageBuilderParameters.massList)
                .getValue();

        this.mzTolerance = parameters
                .getParameter(ImageBuilderParameters.mzTolerance)
                .getValue();
        this.minimumHeight = parameters
                .getParameter(ImageBuilderParameters.minimumHeight)
                .getValue();

        this.suffix = parameters
                .getParameter(ImageBuilderParameters.suffix).getValue();

    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Detecting images in " + dataFile;
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

        logger.info("Started chromatogram builder on " + dataFile);

        scans = scanSelection.getMatchingScans(dataFile);
        int allScanNumbers[] = scanSelection.getMatchingScanNumbers(dataFile);
        totalScans = scans.length;

        // Create new peak list
        newPeakList = new SimplePeakList(dataFile + " " + suffix, dataFile);

        Chromatogram[] chromatograms;
        
        // insert all mz in order and count them
        // mz as integer to avoid floating point * decimals
        //      m/z      number    
        TreeMap<Integer, Integer> signals = new TreeMap<Integer, Integer>();
        

        int decimals = 3;
        double factor = Math.pow(10, decimals);
        
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

            DataPoint mzValues[] = massList.getDataPoints();

            if (mzValues == null) {
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Mass list " + massListName
                        + " does not contain m/z values for scan #"
                        + scan.getScanNumber() + " of file " + dataFile);
                return;
            }

            // add all m/z values in bins
            // insert all mz in order and count them
            for (int i = 0; i < mzValues.length; i++) {
				Integer mz = (int)Math.round(mzValues[i].getMZ()*factor);
				Integer number = signals.get(mz);
				if(number!=null)
					signals.put(mz, number+1);
				else signals.put(mz, 1);
				
			}
            		
            processedScans++;
        }
        
        MassListMzDistribution frame = new MassListMzDistribution();
        frame.createChart(signals, decimals);
        frame.setVisible(true);
        

        setStatus(TaskStatus.FINISHED);

        logger.info("Finished chromatogram builder on " + dataFile);

    }

}
