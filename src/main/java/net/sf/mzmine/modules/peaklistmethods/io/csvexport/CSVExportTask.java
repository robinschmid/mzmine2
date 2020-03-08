/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.io.csvexport;

import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Lists;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.Feature.FeatureStatus;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.identities.iontype.IonIdentity;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.io.gnpsexport.GNPSExportParameters.RowFilter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.RangeUtils;

public class CSVExportTask extends AbstractTask {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private PeakList[] peakLists;
  private int processedRows = 0, totalRows = 0;

  // parameter values
  private File fileName;
  private String plNamePattern = "{}";
  private String fieldSeparator;
  private ExportRowCommonElement[] commonElements;
  private ExportRowDataFileElement[] dataFileElements;
  private Boolean exportAllPeakInfo;
  private String idSeparator;
  private RowFilter filter;
  private boolean mergeLists = false;
  private int lastID = -1;

  public CSVExportTask(ParameterSet parameters) {
    this.peakLists =
        parameters.getParameter(CSVExportParameters.peakLists).getValue().getMatchingPeakLists();
    fileName = parameters.getParameter(CSVExportParameters.filename).getValue();
    fieldSeparator = parameters.getParameter(CSVExportParameters.fieldSeparator).getValue();
    commonElements = parameters.getParameter(CSVExportParameters.exportCommonItems).getValue();
    dataFileElements = parameters.getParameter(CSVExportParameters.exportDataFileItems).getValue();
    exportAllPeakInfo = parameters.getParameter(CSVExportParameters.exportAllPeakInfo).getValue();
    idSeparator = parameters.getParameter(CSVExportParameters.idSeparator).getValue();
    this.filter = parameters.getParameter(CSVExportParameters.filter).getValue();

    // if best annotation and best annotation plus support was selected - deselect
    refineCommonElements();
  }

  /**
   * 
   * @param peakLists
   * @param fileName
   * @param fieldSeparator
   * @param commonElements
   * @param dataFileElements
   * @param exportAllPeakInfo
   * @param idSeparator
   * @param filter Row filter
   */
  public CSVExportTask(PeakList[] peakLists, File fileName, String fieldSeparator,
      ExportRowCommonElement[] commonElements, ExportRowDataFileElement[] dataFileElements,
      Boolean exportAllPeakInfo, String idSeparator, RowFilter filter, boolean mergeLists) {
    super();
    this.peakLists = peakLists;
    this.fileName = fileName;
    this.fieldSeparator = fieldSeparator;
    this.commonElements = commonElements;
    this.dataFileElements = dataFileElements;
    this.exportAllPeakInfo = exportAllPeakInfo;
    this.idSeparator = idSeparator;
    this.filter = filter;
    this.mergeLists = mergeLists;

    // if best annotation and best annotation plus support was selected - deselect
    refineCommonElements();
  }


  private void refineCommonElements() {
    List<ExportRowCommonElement> list = Lists.newArrayList(commonElements);

    if (list.contains(ExportRowCommonElement.ROW_BEST_ANNOTATION)
        && list.contains(ExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT)) {
      list.remove(ExportRowCommonElement.ROW_BEST_ANNOTATION);
      commonElements = list.toArray(new ExportRowCommonElement[list.size()]);
    }
  }


  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting peak list(s) " + Arrays.toString(peakLists) + " to CSV file(s)";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (mergeLists) {
      exportMergedLists();
    } else {
      exportLists();
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);
  }

  private void exportLists() {
    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (PeakList peakList : peakLists) {
      totalRows += peakList.getNumberOfRows();
    }

    // Process peak lists
    for (PeakList peakList : peakLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Open file
      FileWriter writer;
      try {
        writer = new FileWriter(curFile);
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        return;
      }

      exportPeakList(peakList, writer, curFile);

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Close file
      try {
        writer.close();
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not close file " + curFile);
        return;
      }

      // If peak list substitution pattern wasn't found,
      // treat one peak list only
      if (!substitute)
        break;
    }
  }

  private void exportMergedLists() {
    // Total number of rows
    for (PeakList peakList : peakLists) {
      totalRows += peakList.getNumberOfRows();
    }

    // Filename
    File curFile = fileName;
    // Open file
    FileWriter writer;
    try {
      writer = new FileWriter(curFile);
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not open file " + curFile + " for writing.");
      return;
    }

    // all raw data files
    HashMap<String, RawDataFile> raws = new HashMap<>();
    Arrays.stream(peakLists).flatMap(pkl -> Arrays.stream(pkl.getRawDataFiles())).forEach(r -> {
      raws.put(r.getName(), r);
    });
    RawDataFile rawDataFiles[] = raws.values().toArray(RawDataFile[]::new);


    // peak Information
    Set<String> peakInformationFields = new HashSet<>();
    HashMap<String, Integer> renumbered = new HashMap<>();
    int lastID = 0;
    for (PeakList pkl : peakLists) {
      for (PeakListRow r : pkl.getRows()) {
        if (!filter.filter(r))
          continue;
        renumbered.put(getRowMapKey(r), lastID);
        lastID++;

        if (r.getPeakInformation() != null) {
          for (String key : r.getPeakInformation().getAllProperties().keySet()) {
            peakInformationFields.add(key);
          }
        }
      }
    }

    // renumber networks
    HashMap<String, Integer> netIDs = new HashMap<>();
    lastID = 0;
    for (PeakList pkl : peakLists) {
      for (PeakListRow r : pkl.getRows()) {
        if (!filter.filter(r))
          continue;

        if (r.getBestIonIdentity() != null && r.getBestIonIdentity().getNetwork() != null) {
          String key = getNetKey(pkl, r);
          if (!netIDs.containsKey(key)) {
            netIDs.put(key, lastID);
            lastID++;
          }
        }
      }
    }
    logger.info("nets=" + netIDs.size());

    // write header
    writeMergedHeader(writer, curFile, rawDataFiles, renumbered, peakInformationFields);
    // Process peak lists
    for (PeakList peakList : peakLists) {
      exportMergedPeakList(peakList, writer, curFile, rawDataFiles, renumbered,
          peakInformationFields, netIDs);

      // Cancel?
      if (isCanceled()) {
        // Close file
        try {
          writer.close();
        } catch (Exception e) {
        }
        return;
      }
    }
    // Close file
    try {
      writer.close();
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not close file " + curFile);
      return;
    }
  }

  private String getNetKey(PeakList pkl, PeakListRow r) {
    return r.getBestIonIdentity().getNetID() + "netid_" + pkl.getName();
  }

  private String getRowMapKey(PeakListRow r) {
    String rawnames = Arrays.stream(r.getRawDataFiles()).map(RawDataFile::getName)
        .collect(Collectors.joining(","));
    return rawnames + r.getID();
  }

  private void exportPeakList(PeakList peakList, FileWriter writer, File fileName) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();

    // all raw data files
    RawDataFile rawDataFiles[] = peakList.getRawDataFiles();

    // Buffer for writing
    StringBuffer line = new StringBuffer();

    // Write column headers

    // Common elements
    int length = commonElements.length;
    String name;
    for (int i = 0; i < length; i++) {
      if (commonElements[i].equals(ExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT)) {
        line.append("best ion" + fieldSeparator);
        line.append("auto MS2 verify" + fieldSeparator);
        line.append("identified by n=" + fieldSeparator);
        line.append("partners" + fieldSeparator);
      } else if (commonElements[i].equals(ExportRowCommonElement.ROW_BEST_ANNOTATION)) {
        line.append("best ion" + fieldSeparator);
      } else {
        name = commonElements[i].toString();
        name = name.replace("Export ", "");
        name = escapeStringForCSV(name);
        line.append(name + fieldSeparator);
      }
    }

    // peak Information
    Set<String> peakInformationFields = new HashSet<>();

    for (PeakListRow row : peakList.getRows()) {
      if (!filter.filter(row))
        continue;
      if (row.getPeakInformation() != null) {
        for (String key : row.getPeakInformation().getAllProperties().keySet()) {
          peakInformationFields.add(key);
        }
      }
    }

    if (exportAllPeakInfo)
      for (String field : peakInformationFields)
        line.append(field + fieldSeparator);

    // Data file elements
    length = dataFileElements.length;
    for (int df = 0; df < peakList.getNumberOfRawDataFiles(); df++) {
      for (int i = 0; i < length; i++) {
        name = rawDataFiles[df].getName();
        name = name + " " + dataFileElements[i].toString();
        name = escapeStringForCSV(name);
        line.append(name + fieldSeparator);
      }
    }

    line.append("\n");

    try {
      writer.write(line.toString());
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not write to file " + fileName);
      return;
    }

    // Write data rows
    for (PeakListRow peakListRow : peakList.getRows()) {

      if (!filter.filter(peakListRow)) {
        processedRows++;
        continue;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Reset the buffer
      line.setLength(0);

      // Common elements
      length = commonElements.length;
      for (int i = 0; i < length; i++) {
        switch (commonElements[i]) {
          case ROW_ID:
            // starts with 0 when merged
            if (mergeLists)
              lastID++;
            int id = mergeLists ? lastID : peakListRow.getID();
            line.append(id + fieldSeparator);
            break;
          case ROW_MZ:
            line.append(peakListRow.getAverageMZ() + fieldSeparator);
            break;
          case ROW_RT:
            line.append(peakListRow.getAverageRT() + fieldSeparator);
            break;
          case ROW_IDENTITY:
            // Identity elements
            PeakIdentity peakId = peakListRow.getPreferredPeakIdentity();
            if (peakId == null) {
              line.append(fieldSeparator);
              break;
            }
            String propertyValue = peakId.toString();
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_IDENTITY_ALL:
            // Identity elements
            PeakIdentity[] peakIdentities = peakListRow.getPeakIdentities();
            propertyValue = "";
            for (int x = 0; x < peakIdentities.length; x++) {
              if (x > 0)
                propertyValue += idSeparator;
              propertyValue += peakIdentities[x].toString();
            }
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_IDENTITY_DETAILS:
            peakId = peakListRow.getPreferredPeakIdentity();
            if (peakId == null) {
              line.append(fieldSeparator);
              break;
            }
            propertyValue = peakId.getDescription();
            if (propertyValue != null)
              propertyValue = propertyValue.replaceAll("\\n", ";");
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_COMMENT:
            String comment = escapeStringForCSV(peakListRow.getComment());
            line.append(comment + fieldSeparator);
            break;
          case ROW_PEAK_NUMBER:
            int numDetected = 0;
            for (Feature p : peakListRow.getPeaks()) {
              if (p.getFeatureStatus() == FeatureStatus.DETECTED) {
                numDetected++;
              }
            }
            line.append(numDetected + fieldSeparator);
            break;
          case ROW_CORR_GROUP_ID:
            int gid = peakListRow.getGroupID();
            line.append((gid == -1 ? "" : gid) + fieldSeparator);
            break;
          case ROW_BEST_ANNOTATION:
            IonIdentity adduct = peakListRow.getBestIonIdentity();
            if (adduct == null)
              line.append(fieldSeparator);
            else {
              line.append(adduct.getIonType().toString(false) + fieldSeparator);
            }
            break;
          case ROW_BEST_ANNOTATION_AND_SUPPORT:
            IonIdentity ad = peakListRow.getBestIonIdentity();
            if (ad == null)
              line.append(fieldSeparator + fieldSeparator + fieldSeparator + fieldSeparator);
            else {
              String msms = "";
              if (ad.getMSMSModVerify() > 0)
                msms = "MS/MS verified: nloss";
              if (ad.getMSMSMultimerCount() > 0)
                msms += msms.isEmpty() ? "MS/MS verified: xmer" : (idSeparator + " xmer");
              String partners =
                  StringUtils.join(ArrayUtils.toObject(ad.getPartnerRowsID()), idSeparator);
              line.append(ad.getIonType().toString(false) + fieldSeparator //
                  + msms + fieldSeparator //
                  + ad.getPartnerRowsID().length + fieldSeparator //
                  + partners + fieldSeparator);
            }
            break;
          case ROW_MOL_NETWORK_ID:
            IonIdentity ad2 = peakListRow.getBestIonIdentity();
            if (ad2 == null || ad2.getNetwork() == null)
              line.append(fieldSeparator);
            else
              line.append(ad2.getNetwork().getID() + fieldSeparator);
            break;
          case ROW_NEUTRAL_MASS:
            IonIdentity ad3 = peakListRow.getBestIonIdentity();
            if (ad3 == null || ad3.getNetwork() == null)
              line.append(fieldSeparator);
            else
              line.append(mzForm.format(ad3.getNetwork().calcNeutralMass()) + fieldSeparator);
            break;
        }
      }

      // peak Information
      if (exportAllPeakInfo) {
        if (peakListRow.getPeakInformation() != null) {
          Map<String, String> allPropertiesMap =
              peakListRow.getPeakInformation().getAllProperties();

          for (String key : peakInformationFields) {
            String value = allPropertiesMap.get(key);
            if (value == null)
              value = "";
            line.append(value + fieldSeparator);
          }
        }
      }

      // Data file elements
      length = dataFileElements.length;
      for (RawDataFile dataFile : rawDataFiles) {
        for (int i = 0; i < length; i++) {
          Feature peak = peakListRow.getPeak(dataFile);
          if (peak != null) {
            switch (dataFileElements[i]) {
              case PEAK_STATUS:
                line.append(peak.getFeatureStatus() + fieldSeparator);
                break;
              case PEAK_MZ:
                line.append(peak.getMZ() + fieldSeparator);
                break;
              case PEAK_RT:
                line.append(peak.getRT() + fieldSeparator);
                break;
              case PEAK_RT_START:
                line.append(peak.getRawDataPointsRTRange().lowerEndpoint() + fieldSeparator);
                break;
              case PEAK_RT_END:
                line.append(peak.getRawDataPointsRTRange().upperEndpoint() + fieldSeparator);
                break;
              case PEAK_DURATION:
                line.append(
                    RangeUtils.rangeLength(peak.getRawDataPointsRTRange()) + fieldSeparator);
                break;
              case PEAK_HEIGHT:
                line.append(peak.getHeight() + fieldSeparator);
                break;
              case PEAK_AREA:
                line.append(peak.getArea() + fieldSeparator);
                break;
              case PEAK_CHARGE:
                line.append(peak.getCharge() + fieldSeparator);
                break;
              case PEAK_DATAPOINTS:
                line.append(peak.getScanNumbers().length + fieldSeparator);
                break;
              case PEAK_FWHM:
                line.append(peak.getFWHM() + fieldSeparator);
                break;
              case PEAK_TAILINGFACTOR:
                line.append(peak.getTailingFactor() + fieldSeparator);
                break;
              case PEAK_ASYMMETRYFACTOR:
                line.append(peak.getAsymmetryFactor() + fieldSeparator);
                break;
              case PEAK_MZMIN:
                line.append(peak.getRawDataPointsMZRange().lowerEndpoint() + fieldSeparator);
                break;
              case PEAK_MZMAX:
                line.append(peak.getRawDataPointsMZRange().upperEndpoint() + fieldSeparator);
                break;
            }
          } else {
            switch (dataFileElements[i]) {
              case PEAK_STATUS:
                line.append(FeatureStatus.UNKNOWN + fieldSeparator);
                break;
              default:
                line.append("0" + fieldSeparator);
                break;
            }
          }
        }
      }

      line.append("\n");

      try {
        writer.write(line.toString());
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not write to file " + fileName);
        return;
      }

      processedRows++;
    }
  }


  private void writeMergedHeader(FileWriter writer, File fileName, RawDataFile[] rawDataFiles,
      HashMap<String, Integer> renumbered, Set<String> peakInformationFields) {
    // Buffer for writing
    StringBuffer line = new StringBuffer();
    // Write column headers
    // Common elements
    int length = commonElements.length;
    String name;
    for (int i = 0; i < length; i++) {
      if (commonElements[i].equals(ExportRowCommonElement.ROW_BEST_ANNOTATION_AND_SUPPORT)) {
        line.append("best ion" + fieldSeparator);
        line.append("auto MS2 verify" + fieldSeparator);
        line.append("identified by n=" + fieldSeparator);
        line.append("partners" + fieldSeparator);
      } else if (commonElements[i].equals(ExportRowCommonElement.ROW_BEST_ANNOTATION)) {
        line.append("best ion" + fieldSeparator);
      } else {
        name = commonElements[i].toString();
        name = name.replace("Export ", "");
        name = escapeStringForCSV(name);
        line.append(name + fieldSeparator);
      }
    }

    if (exportAllPeakInfo)
      for (String field : peakInformationFields)
        line.append(field + fieldSeparator);

    // Data file elements
    length = dataFileElements.length;
    for (int df = 0; df < rawDataFiles.length; df++) {
      for (int i = 0; i < length; i++) {
        name = rawDataFiles[df].getName();
        name = name + " " + dataFileElements[i].toString();
        name = escapeStringForCSV(name);
        line.append(name + fieldSeparator);
      }
    }

    line.append("\n");

    try {
      writer.write(line.toString());
    } catch (Exception e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Could not write to file " + fileName);
      return;
    }
  }

  private void exportMergedPeakList(PeakList peakList, FileWriter writer, File fileName,
      RawDataFile[] rawDataFiles, HashMap<String, Integer> renumbered,
      Set<String> peakInformationFields, HashMap<String, Integer> netIDs) {
    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();

    StringBuffer line = new StringBuffer();
    int length = 0;
    // Write data rows
    for (PeakListRow peakListRow : peakList.getRows()) {

      if (!filter.filter(peakListRow)) {
        processedRows++;
        continue;
      }

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Reset the buffer
      line.setLength(0);

      // Common elements
      length = commonElements.length;
      for (int i = 0; i < length; i++) {
        switch (commonElements[i]) {
          case ROW_ID:
            // starts with 0 when merged
            if (mergeLists)
              lastID++;
            int id = mergeLists ? lastID : peakListRow.getID();
            line.append(id + fieldSeparator);
            break;
          case ROW_MZ:
            line.append(peakListRow.getAverageMZ() + fieldSeparator);
            break;
          case ROW_RT:
            line.append(peakListRow.getAverageRT() + fieldSeparator);
            break;
          case ROW_IDENTITY:
            // Identity elements
            PeakIdentity peakId = peakListRow.getPreferredPeakIdentity();
            if (peakId == null) {
              line.append(fieldSeparator);
              break;
            }
            String propertyValue = peakId.toString();
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_IDENTITY_ALL:
            // Identity elements
            PeakIdentity[] peakIdentities = peakListRow.getPeakIdentities();
            propertyValue = "";
            for (int x = 0; x < peakIdentities.length; x++) {
              if (x > 0)
                propertyValue += idSeparator;
              propertyValue += peakIdentities[x].toString();
            }
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_IDENTITY_DETAILS:
            peakId = peakListRow.getPreferredPeakIdentity();
            if (peakId == null) {
              line.append(fieldSeparator);
              break;
            }
            propertyValue = peakId.getDescription();
            if (propertyValue != null)
              propertyValue = propertyValue.replaceAll("\\n", ";");
            propertyValue = escapeStringForCSV(propertyValue);
            line.append(propertyValue + fieldSeparator);
            break;
          case ROW_COMMENT:
            String comment = escapeStringForCSV(peakListRow.getComment());
            line.append(comment + fieldSeparator);
            break;
          case ROW_PEAK_NUMBER:
            int numDetected = 0;
            for (Feature p : peakListRow.getPeaks()) {
              if (p.getFeatureStatus() == FeatureStatus.DETECTED) {
                numDetected++;
              }
            }
            line.append(numDetected + fieldSeparator);
            break;
          case ROW_CORR_GROUP_ID:
            int gid = peakListRow.getGroupID();
            line.append((gid == -1 ? "" : gid) + fieldSeparator);
            break;
          case ROW_BEST_ANNOTATION:
            IonIdentity adduct = peakListRow.getBestIonIdentity();
            if (adduct == null)
              line.append(fieldSeparator);
            else {
              line.append(adduct.getIonType().toString(false) + fieldSeparator);
            }
            break;
          case ROW_BEST_ANNOTATION_AND_SUPPORT:
            IonIdentity ad = peakListRow.getBestIonIdentity();
            if (ad == null)
              line.append(fieldSeparator + fieldSeparator + fieldSeparator + fieldSeparator);
            else {
              String msms = "";
              if (ad.getMSMSModVerify() > 0)
                msms = "MS/MS verified: nloss";
              if (ad.getMSMSMultimerCount() > 0)
                msms += msms.isEmpty() ? "MS/MS verified: xmer" : (idSeparator + " xmer");

              int[] pid = ad.getPartnerRowsID();
              Integer[] mergedpid = new Integer[pid.length];
              for (int y = 0; y < pid.length; y++) {
                mergedpid[y] = renumbered.get(getRowMapKey(peakList.findRowByID(pid[y])));
              }

              String partners = StringUtils.join(mergedpid, idSeparator);
              line.append(ad.getIonType().toString(false) + fieldSeparator //
                  + msms + fieldSeparator //
                  + ad.getPartnerRowsID().length + fieldSeparator //
                  + partners + fieldSeparator);
            }
            break;
          case ROW_MOL_NETWORK_ID:
            IonIdentity ad2 = peakListRow.getBestIonIdentity();
            if (ad2 == null || ad2.getNetwork() == null)
              line.append(fieldSeparator);
            else {
              String key = getNetKey(peakList, peakListRow);
              line.append(netIDs.get(key) + fieldSeparator);
            }
            break;
          case ROW_NEUTRAL_MASS:
            IonIdentity ad3 = peakListRow.getBestIonIdentity();
            if (ad3 == null || ad3.getNetwork() == null)
              line.append(fieldSeparator);
            else
              line.append(mzForm.format(ad3.getNetwork().calcNeutralMass()) + fieldSeparator);
            break;
        }
      }

      // peak Information
      if (exportAllPeakInfo) {
        if (peakListRow.getPeakInformation() != null) {
          Map<String, String> allPropertiesMap =
              peakListRow.getPeakInformation().getAllProperties();

          for (String key : peakInformationFields) {
            String value = allPropertiesMap.get(key);
            if (value == null)
              value = "";
            line.append(value + fieldSeparator);
          }
        }
      }

      // Data file elements
      length = dataFileElements.length;
      for (RawDataFile dataFile : rawDataFiles) {
        for (int i = 0; i < length; i++) {
          Feature peak = peakListRow.getPeak(dataFile);
          if (peak != null) {
            switch (dataFileElements[i]) {
              case PEAK_STATUS:
                line.append(peak.getFeatureStatus() + fieldSeparator);
                break;
              case PEAK_MZ:
                line.append(peak.getMZ() + fieldSeparator);
                break;
              case PEAK_RT:
                line.append(peak.getRT() + fieldSeparator);
                break;
              case PEAK_RT_START:
                line.append(peak.getRawDataPointsRTRange().lowerEndpoint() + fieldSeparator);
                break;
              case PEAK_RT_END:
                line.append(peak.getRawDataPointsRTRange().upperEndpoint() + fieldSeparator);
                break;
              case PEAK_DURATION:
                line.append(
                    RangeUtils.rangeLength(peak.getRawDataPointsRTRange()) + fieldSeparator);
                break;
              case PEAK_HEIGHT:
                line.append(peak.getHeight() + fieldSeparator);
                break;
              case PEAK_AREA:
                line.append(peak.getArea() + fieldSeparator);
                break;
              case PEAK_CHARGE:
                line.append(peak.getCharge() + fieldSeparator);
                break;
              case PEAK_DATAPOINTS:
                line.append(peak.getScanNumbers().length + fieldSeparator);
                break;
              case PEAK_FWHM:
                line.append(peak.getFWHM() + fieldSeparator);
                break;
              case PEAK_TAILINGFACTOR:
                line.append(peak.getTailingFactor() + fieldSeparator);
                break;
              case PEAK_ASYMMETRYFACTOR:
                line.append(peak.getAsymmetryFactor() + fieldSeparator);
                break;
              case PEAK_MZMIN:
                line.append(peak.getRawDataPointsMZRange().lowerEndpoint() + fieldSeparator);
                break;
              case PEAK_MZMAX:
                line.append(peak.getRawDataPointsMZRange().upperEndpoint() + fieldSeparator);
                break;
            }
          } else {
            switch (dataFileElements[i]) {
              case PEAK_STATUS:
                line.append(FeatureStatus.UNKNOWN + fieldSeparator);
                break;
              default:
                line.append("0" + fieldSeparator);
                break;
            }
          }
        }
      }

      line.append("\n");

      try {
        writer.write(line.toString());
      } catch (Exception e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not write to file " + fileName);
        return;
      }

      processedRows++;
    }
  }

  private String escapeStringForCSV(final String inputString) {

    if (inputString == null)
      return "";

    // Remove all special characters (particularly \n would mess up our CSV
    // format).
    String result = inputString.replaceAll("[\\p{Cntrl}]", " ");

    // Skip too long strings (see Excel 2007 specifications)
    if (result.length() >= 32766)
      result = result.substring(0, 32765);

    // If the text contains fieldSeparator, we will add
    // parenthesis
    if (result.contains(fieldSeparator) || result.contains("\"")) {
      result = "\"" + result.replaceAll("\"", "'") + "\"";
    }

    return result;
  }
}
