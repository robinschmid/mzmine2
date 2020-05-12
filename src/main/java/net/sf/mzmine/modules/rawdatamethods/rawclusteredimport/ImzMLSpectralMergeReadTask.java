/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.rawdatamethods.rawclusteredimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.alanmrace.jimzmlparser.exceptions.ImzMLParseException;
import com.alanmrace.jimzmlparser.imzml.ImzML;
import com.alanmrace.jimzmlparser.mzml.BinaryDataArray;
import com.alanmrace.jimzmlparser.mzml.BinaryDataArrayList;
import com.alanmrace.jimzmlparser.mzml.CVParam;
import com.alanmrace.jimzmlparser.mzml.Precursor;
import com.alanmrace.jimzmlparser.mzml.PrecursorList;
import com.alanmrace.jimzmlparser.mzml.Scan;
import com.alanmrace.jimzmlparser.mzml.ScanList;
import com.alanmrace.jimzmlparser.mzml.SelectedIon;
import com.alanmrace.jimzmlparser.mzml.SelectedIonList;
import com.alanmrace.jimzmlparser.mzml.Spectrum;
import com.alanmrace.jimzmlparser.mzml.SpectrumList;
import com.alanmrace.jimzmlparser.parser.ImzMLHandler;
import com.google.common.base.Stopwatch;
import net.sf.mzmine.datamodel.Coordinates;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.RawDataFileWriter;
import net.sf.mzmine.datamodel.impl.CoordinatesXY;
import net.sf.mzmine.datamodel.impl.CoordinatesXYZ;
import net.sf.mzmine.datamodel.impl.ImagingParameters;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleImagingScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan;
import net.sf.mzmine.datamodel.impl.SimpleMergedScan.Result;
import net.sf.mzmine.modules.tools.msmsspectramerge.IntensityMergeMode;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.project.impl.ImagingRawDataFileImpl;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.ExceptionUtils;
import net.sf.mzmine.util.scans.ScanUtils;

/**
 * This class reads mzML 1.0 and 1.1.0 files (http://www.psidev.info/index.php?q=node/257) using the
 * jmzml library (http://code.google.com/p/jmzml/).
 */
public class ImzMLSpectralMergeReadTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(ImzMLSpectralMergeReadTask.class.getName());

  private File file;
  private MZmineProject project;
  private RawDataFileWriter newMZmineFile;
  private ImagingRawDataFileImpl finalRawDataFile;
  private int totalScans = 0, parsedScans;

  private int lastScanNumber = 0;

  private Map<String, Integer> scanIdTable = new Hashtable<String, Integer>();

  // define by user input
  private double minCosine = 0.90;
  private MZTolerance mzTol = new MZTolerance(0.02, 30);
  private double noiseLevel = 0;
  private double minHeight = 0;
  private int minMatch = 5;
  private double minPercentSpectra = 0.10;
  private int minSpectra = 1;


  public ImzMLSpectralMergeReadTask(MZmineProject project, File fileToOpen,
      RawDataFileWriter newMZmineFile, ParameterSet parameters) {
    this.project = project;
    this.file = fileToOpen;
    this.newMZmineFile = newMZmineFile;

    minCosine = parameters.getParameter(RawClusteredImportParameters.minCosine).getValue();
    mzTol = parameters.getParameter(RawClusteredImportParameters.mzTol).getValue();
    minHeight = parameters.getParameter(RawClusteredImportParameters.minHeight).getValue();
    noiseLevel = parameters.getParameter(RawClusteredImportParameters.noiseCutoff).getValue();
    minMatch = parameters.getParameter(RawClusteredImportParameters.minMatch).getValue();
    boolean usePercent =
        parameters.getParameter(RawClusteredImportParameters.minPercentSpectra).getValue();
    minPercentSpectra = !usePercent ? 0d
        : parameters.getParameter(RawClusteredImportParameters.minPercentSpectra)
            .getEmbeddedParameter().getValue();
    minSpectra = parameters.getParameter(RawClusteredImportParameters.minSpectra).getValue();
    if (minHeight <= noiseLevel)
      minHeight = 0d;
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) parsedScans / totalScans;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);
    //
    List<SimpleMergedScan> mergedScans = new ArrayList<SimpleMergedScan>();
    List<SimpleImagingScan> ms2Scans = new ArrayList<SimpleImagingScan>();

    Stopwatch watch = Stopwatch.createStarted();

    logger.info("Started parsing file " + file);
    // file = new File("C:/DATA/MALDI Sh/examples/Example_Processed.imzML");
    ImzML imzml;
    try {
      imzml = ImzMLHandler.parseimzML(file.getAbsolutePath());
    } catch (ImzMLParseException e1) {
      logger.log(Level.SEVERE, "Error while parsing imzML", e1);
      setErrorMessage("Cannot load imzML");
      setStatus(TaskStatus.ERROR);
      return;
    }
    SpectrumList spectra = imzml.getRun().getSpectrumList();
    totalScans = spectra.size();

    try {
      for (int i = 0; i < totalScans; i++) {
        if (isCanceled())
          break;

        Spectrum spectrum = spectra.get(i);

        // Ignore scans that are not MS, e.g. UV
        if (!isMsSpectrum(spectrum)) {
          parsedScans++;
          continue;
        }

        // get data points and try to merge
        int msLevel = extractMSLevel(spectrum);
        DataPoint dataPoints[] = extractDataPoints(spectrum, noiseLevel);

        // add MS2 scan
        if (msLevel > 1) {
          ms2Scans.add(createScan(spectrum, dataPoints));
        }
        // try to merge MS1 scans
        // check min signals and add new scan
        else if (dataPoints.length >= minMatch
            && !mergeWithFirst(mergedScans, spectrum, dataPoints)) {
          // was not merged
          // create scan and add new merged scan
          logger.log(Level.INFO, "Scan " + i + " not merged");
          SimpleImagingScan rawscan = createScan(spectrum, dataPoints);
          mergedScans.add(new SimpleMergedScan(rawscan, IntensityMergeMode.AVERAGE, 1));
        }
        parsedScans++;
      }

      logger.log(Level.INFO, "add all scans to raw file");
      int i = 1;
      for (SimpleMergedScan scan : mergedScans) {
        // add merged
        if (scan.getScanCount() > 1) {
          // clean up
          scan.clean(minPercentSpectra, minSpectra);

          // add average
          scan.setScanNumber(i);
          newMZmineFile.addScan(scan);
          i++;
          // add maximum merged scan
          SimpleMergedScan maxScan = new SimpleMergedScan(scan, IntensityMergeMode.MAXIMUM);
          maxScan.setScanNumber(i);
          newMZmineFile.addScan(maxScan);
          i++;

          // sum
          SimpleMergedScan sumScan = new SimpleMergedScan(scan, IntensityMergeMode.SUM);
          sumScan.setScanNumber(i);
          newMZmineFile.addScan(sumScan);
          i++;

          // add best scan
          ((SimpleImagingScan) scan.getBestScan()).setScanNumber(i);
          newMZmineFile.addScan(scan.getBestScan());
          i++;
        }
        // add best
        else {
          ((SimpleImagingScan) scan.getBestScan()).setScanNumber(i + 1);
          newMZmineFile.addScan(scan.getBestScan());
          i++;
        }
      }
      for (SimpleImagingScan scan : ms2Scans) {
        // add ms2 at the end
        scan.setScanNumber(i);
        newMZmineFile.addScan(scan);
        i++;
      }

      finalRawDataFile = (ImagingRawDataFileImpl) newMZmineFile.finishWriting();
      // set settings of image
      finalRawDataFile.setImagingParam(new ImagingParameters(imzml));
      //
      project.addFile(finalRawDataFile);

      // TODO
      // add each spectrum as a signal to a peaklist with the most abundant peak in the spectrum
      // for(int s=1; s<i; s++) {
      // Scan scan = finalRawDataFile.getScan(s);
      //
      // }

    } catch (Throwable e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error parsing mzML: " + ExceptionUtils.exceptionToString(e));
      e.printStackTrace();
      return;
    }

    if (parsedScans == 0) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("No scans found");
      return;
    }

    watch.stop();
    logger.info("TIME: " + watch.elapsed(TimeUnit.SECONDS) + "; Finished parsing " + file
        + ", parsed " + parsedScans + " scans");
    setStatus(TaskStatus.FINISHED);
  }

  public static SimpleImagingScan createScan(Spectrum spectrum, DataPoint[] dataPoints) {
    String scanId = spectrum.getID();
    int scanNumber = 0;
    int msLevel = extractMSLevel(spectrum);

    // Extract scan data
    double retentionTime = extractRetentionTime(spectrum);
    PolarityType polarity = extractPolarity(spectrum);
    int parentScan = extractParentScanNumber(spectrum);
    double precursorMz = extractPrecursorMz(spectrum);
    int precursorCharge = extractPrecursorCharge(spectrum);
    String scanDefinition = extractScanDefinition(spectrum);

    // imaging
    Coordinates coord = extractCoordinates(spectrum);

    // Auto-detect whether this scan is centroided
    MassSpectrumType spectrumType = ScanUtils.detectSpectrumType(dataPoints);

    return new SimpleImagingScan(null, scanNumber, msLevel, retentionTime, precursorMz,
        precursorCharge, null, dataPoints, spectrumType, polarity, scanDefinition, null, coord);
  }

  /**
   * Merge datapoints into first matching scan. Sort MergedScans list by number of merged scans
   * 
   * @param mergedScans
   * @param spectrum
   * @param dataPoints
   * @return
   */
  public boolean mergeWithFirst(List<SimpleMergedScan> mergedScans, Spectrum spectrum,
      DataPoint[] dataPoints) {
    DataPoint[] filtered =
        minHeight > noiseLevel ? null : ScanUtils.getFiltered(dataPoints, minHeight);
    for (int m = 0; m < mergedScans.size(); m++) {
      SimpleMergedScan scan = mergedScans.get(m);
      // try to merge
      Result res = scan.merge(dataPoints, filtered, mzTol, minHeight, minCosine, minMatch);
      if (!res.equals(Result.FALSE)) {
        logger.info("MERGED SCANS in list index " + m + "; total: " + scan.getScanCount());
        if (res.equals(Result.MERGED_REPLACE_BEST_SCAN)) {
          // replace best scan in merged with this rawscan
          scan.setBestScan(createScan(spectrum, dataPoints));
          logger.info("Scan is new best in merged");
        }
        // was merged into the scan
        int mergedScanCount = scan.getScanCount();
        // insert sort list
        for (int s = 0; s < m; s++) {
          if (mergedScans.get(s).getScanCount() <= mergedScanCount) {
            mergedScans.remove(m);
            mergedScans.add(s, scan);
            return true;
          }
        }
        return true;
      }
    }
    return false;
  }

  public static int extractMSLevel(Spectrum spectrum) {
    // Browse the spectrum parameters
    // MS level MS:1000511
    try {
      CVParam param = spectrum.getCVParam("MS:1000511");
      if (param != null) {
        int level = param.getValueAsInteger();
        return level > 0 ? level : 0;
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Cannot parse MS level", e);
    }
    return 1;
  }

  public static double extractRetentionTime(Spectrum spectrum) {
    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement == null)
      return 0;

    for (Scan scan : scanListElement) {
      try {
        // scan start time correct?
        CVParam param = scan.getCVParam(Scan.SCAN_START_TIME_ID);
        if (param != null)
          return param.getValueAsDouble();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return 0;
  }

  public static DataPoint[] extractDataPoints(Spectrum spectrum, double noiseLevel) {
    try {
      BinaryDataArrayList dataList = spectrum.getBinaryDataArrayList();

      BinaryDataArray mzArray = dataList.getmzArray();
      double mzValues[] = mzArray.getDataAsDouble();
      if ((dataList == null) || (mzValues.length == 0))
        return new DataPoint[0];

      BinaryDataArray intensityArray = dataList.getIntensityArray();
      double intensityValues[] = intensityArray.getDataAsDouble();
      List<DataPoint> dataPoints = new ArrayList<>();
      for (int i = 0; i < intensityValues.length; i++) {
        double intensity = intensityValues[i];
        if (intensity > 0.000001d && intensity > noiseLevel) {
          double mz = mzValues[i];
          dataPoints.add(new SimpleDataPoint(mz, intensity));
        }
      }
      return dataPoints.toArray(DataPoint[]::new);

    } catch (IOException e) {
      e.printStackTrace();
    }
    return new DataPoint[0];
  }

  public static Coordinates extractCoordinates(Spectrum spectrum) {
    ScanList list = spectrum.getScanList();
    if (list != null) {
      for (Scan scan : spectrum.getScanList()) {
        CVParam xValue = scan.getCVParam(Scan.POSITION_X_ID);
        CVParam yValue = scan.getCVParam(Scan.POSITION_Y_ID);
        CVParam zValue = scan.getCVParam(Scan.POSITION_Z_ID);

        if (xValue != null && yValue != null) {
          int x = xValue.getValueAsInteger() - 1;
          int y = yValue.getValueAsInteger() - 1;

          if (zValue != null)
            return new CoordinatesXYZ(x, y, zValue.getValueAsInteger() - 1);
          else
            return new CoordinatesXY(x, y);
        }
      }
    }
    return null;
  }


  /**
   * TODO how to get precursor id
   * 
   * @param spectrum
   * @return
   */
  public static int extractParentScanNumber(Spectrum spectrum) {
    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.size() == 0))
      return -1;

    for (Precursor parent : precursorListElement) {
      // Get the precursor scan number
      // String precursorScanId = parent.getRefSpectrumRef().getID();
      // if (precursorScanId == null) {
      // return -1;
      // }
      // int parentScan = convertScanIdToScanNumber(precursorScanId);
      // return parentScan;
    }
    return -1;
  }

  public static double extractPrecursorMz(Spectrum spectrum) {
    PrecursorList precursorListElement = spectrum.getPrecursorList();
    if ((precursorListElement == null) || (precursorListElement.size() == 0))
      return 0;

    for (Precursor parent : precursorListElement) {

      SelectedIonList selectedIonListElement = parent.getSelectedIonList();
      if ((selectedIonListElement == null) || (selectedIonListElement.size() == 0))
        return 0;

      // MS:1000040 is used in mzML 1.0,
      // MS:1000744 is used in mzML 1.1.0
      for (SelectedIon sion : selectedIonListElement) {
        CVParam param = sion.getCVParam("MS:1000040");
        if (param != null)
          return param.getValueAsDouble();

        param = sion.getCVParam("MS:1000744");
        if (param != null)
          return param.getValueAsDouble();
      }
    }
    return 0;
  }

  public static int extractPrecursorCharge(Spectrum spectrum) {
    PrecursorList precursorList = spectrum.getPrecursorList();
    if ((precursorList == null) || (precursorList.size() == 0))
      return 0;

    for (Precursor parent : precursorList) {
      SelectedIonList selectedIonListElement = parent.getSelectedIonList();
      if ((selectedIonListElement == null) || (selectedIonListElement.size() == 0))
        return 0;

      for (SelectedIon sion : selectedIonListElement) {

        // precursor charge
        CVParam param = sion.getCVParam("MS:1000041");
        if (param != null)
          return param.getValueAsInteger();
      }
    }
    return 0;
  }

  public static PolarityType extractPolarity(Spectrum spectrum) {
    CVParam cv = spectrum.getCVParam(Spectrum.SCAN_POLARITY_ID);
    if (spectrum.getCVParam("MS:1000130") != null)
      return PolarityType.POSITIVE;
    else if (spectrum.getCVParam("MS:1000129") != null)
      return PolarityType.NEGATIVE;

    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement != null) {
      for (int i = 0; i < scanListElement.size(); i++) {
        Scan scan = scanListElement.get(i);

        if (scan.getCVParam("MS:1000130") != null)
          return PolarityType.POSITIVE;
        else if (scan.getCVParam("MS:1000129") != null)
          return PolarityType.NEGATIVE;
      }
    }
    return PolarityType.UNKNOWN;
  }

  public static String extractScanDefinition(Spectrum spectrum) {
    CVParam cvParams = spectrum.getCVParam("MS:1000512");
    if (cvParams != null)
      return cvParams.getValueAsString();

    ScanList scanListElement = spectrum.getScanList();
    if (scanListElement != null) {
      for (int i = 0; i < scanListElement.size(); i++) {
        Scan scan = scanListElement.get(i);

        cvParams = scan.getCVParam("MS:1000512");
        if (cvParams != null)
          return cvParams.getValueAsString();
      }
    }
    return spectrum.getID();
  }


  public static boolean isMsSpectrum(Spectrum spectrum) {
    // one thats not MS (code for UV?)
    CVParam cvParams = spectrum.getCVParam("MS:1000804");

    // By default, let's assume unidentified spectra are MS spectra
    return cvParams == null;
  }

  @Override
  public String getTaskDescription() {
    return "Opening file and merging spectra " + file;
  }
}
