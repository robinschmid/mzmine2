package net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graphstream.graph.Node;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsIdentity;
import net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsIdentity.ATT;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats.GnpsJsonGenerator;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibraryMethodeMetaDataParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis.GNPSResultsAnalysisTask.NodeAtt;
import net.sf.mzmine.taskcontrol.AbstractTask;

public class GNPSLibraryBatchExportTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private static final DecimalFormat scoreFormat = new DecimalFormat("0.000");
  private static String del = "\t";
  private static String nl = "\n";

  // batch import header
  // tab separated
  public static final String HEADER =
      "FILENAME,SEQ,COMPOUND_NAME,MOLECULEMASS,INSTRUMENT,IONSOURCE,EXTRACTSCAN,SMILES,INCHI,INCHIAUX,CHARGE,IONMODE,PUBMED,ACQUISITION,EXACTMASS,DATACOLLECTOR,ADDUCT,INTEREST,LIBQUALITY,GENUS,SPECIES,STRAIN,CASNUMBER,PI";


  private LibraryMethodeMetaDataParameters methodParam;
  private File outputLibrary;
  private GnpsResults res;
  private double minMatchScoreGNPS;

  public GNPSLibraryBatchExportTask(LibraryMethodeMetaDataParameters methodParam,
      File outputLibrary, GnpsResults res, double minMatchScoreGNPS) {
    this.methodParam = methodParam;
    this.outputLibrary = outputLibrary;
    this.res = res;
    this.minMatchScoreGNPS = minMatchScoreGNPS;
  }

  @Override
  public String getTaskDescription() {
    return "";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  /**
   * Find all IIN with identity (spectral match) and export conneted nodes as new library entries
   * 
   * @param methodParam
   * 
   * @param outputLibrary
   * @param res
   */
  @Override
  public void run() {

    Map<Integer, GNPSResultsIdentity> matches = res.getMatches();
    Map<Integer, DataPoint[]> msmsData = res.getMsmsData();
    Map<Integer, IonIdentityNetworkResult> nets = res.getNets();
    AtomicInteger totalNew = new AtomicInteger(0);
    // create parameters:
    LibraryMetaDataParameters meta = new LibraryMetaDataParameters(methodParam);
    LibrarySubmitIonParameters param = new LibrarySubmitIonParameters();
    param.getParameter(LibrarySubmitIonParameters.META_PARAM).setValue(meta);

    try {
      if (!outputLibrary.getParentFile().exists())
        outputLibrary.getParentFile().mkdirs();
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Cannot create folder " + outputLibrary.getParent(), e);
    }

    // open file output
    try (BufferedWriter writer = new BufferedWriter((new FileWriter(outputLibrary, false)))) {

      // for all networks
      for (IonIdentityNetworkResult net : nets.values()) {
        // has identity
        GNPSResultsIdentity bestMatch = net.getBestLibraryMatch(matches);
        // >min match score
        if (bestMatch != null && bestMatch.getMatchScore() >= minMatchScoreGNPS) {
          // all possible new library entries of this ion network
          net.stream().filter(node -> !node.equals(bestMatch))
              .filter(node -> hasMSMS(node, msmsData, 3, 0.001)).forEach(node -> {
                // export to library
                int id = toIndex(node);
                DataPoint[] signals = msmsData.get(id);
                totalNew.getAndIncrement();
                logger.log(Level.INFO,
                    "new lib:" + totalNew.get() + "  Exporting node " + id + " with signals="
                        + signals.length + "  for entry: " + bestMatch.getName() + " old->new ("
                        + bestMatch.getResult(ATT.ADDUCT) + "->"
                        + IonIdentityNetworkResult.getIonString(node) + ")");
                //
                String entry = exportLibraryEntry(node, id, signals, bestMatch, net, meta, param);
                try {
                  writer.write(entry);
                  writer.write(System.lineSeparator());
                } catch (IOException e) {
                  logger.log(Level.SEVERE,
                      "Error while writing " + entry + " to " + outputLibrary.getAbsolutePath(), e);
                  e.printStackTrace();
                }
              });
        }
      }

      logger.info(totalNew.get() + " added new entries to " + outputLibrary.getAbsolutePath());
      // close file output automatically
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error while writing to " + outputLibrary.getAbsolutePath(), e);
      e.printStackTrace();
    }
  }

  private String exportLibraryEntry(Node node, int id, DataPoint[] signals,
      GNPSResultsIdentity bestMatch, IonIdentityNetworkResult net, LibraryMetaDataParameters meta,
      LibrarySubmitIonParameters param) {

    String description = meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).getValue();

    String combinedDescription =
        "created by [IIN] (GNPS score=" + scoreFormat.format(bestMatch.getMatchScore()) + ", "
            + bestMatch.getResult(ATT.ADDUCT) + "), " + description + ", original lib entry: "
            + bestMatch.getResult(ATT.GNPS_LIBRARY_URL);
    meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).setValue(combinedDescription);

    // By Library match
    String newName = bestMatch.getResult(ATT.COMPOUND_NAME).toString() + " [IIN based]";
    meta.getParameter(LibraryMetaDataParameters.COMPOUND_NAME).setValue(newName);
    meta.getParameter(LibraryMetaDataParameters.SMILES)
        .setValue(bestMatch.getResult(ATT.SMILES).toString());
    meta.getParameter(LibraryMetaDataParameters.INCHI)
        .setValue(bestMatch.getResult(ATT.INCHI).toString());
    // not given in GNPS output (graphml)
    meta.getParameter(LibraryMetaDataParameters.FORMULA).setValue("");
    meta.getParameter(LibraryMetaDataParameters.INCHI_AUX).setValue("");
    meta.getParameter(LibraryMetaDataParameters.CAS).setValue("");
    meta.getParameter(LibraryMetaDataParameters.PUBMED).setValue("");

    // by IIN
    double neutralMass = (double) bestMatch.getResult(ATT.NEUTRAL_M_MASS);
    meta.getParameter(LibraryMetaDataParameters.EXACT_MASS).setValue(neutralMass);
    param.getParameter(LibrarySubmitIonParameters.ADDUCT)
        .setValue(IonIdentityNetworkResult.getIonString(node));
    param.getParameter(LibrarySubmitIonParameters.MZ)
        .setValue((double) node.getAttribute(NodeAtt.PRECURSOR_MASS.key));
    param.getParameter(LibrarySubmitIonParameters.CHARGE).setValue(0);

    // write
    String json = GnpsJsonGenerator.generateJSON(param, signals);

    // reset
    meta.getParameter(LibraryMetaDataParameters.DESCRIPTION).setValue(description);

    return json;
  }


  /**
   * Node index was peak list row index
   * 
   * @param n
   * @return
   */
  private Integer toIndex(Node n) {
    return Integer.parseInt(n.getId());
  }


  /**
   * minimum signals above cutOffFromMaxIntensity
   * 
   * @param n
   * @param msmsData
   * @param minSignals
   * @param cutOffFromMaxIntensity 0.01 is 1 % of max intensity
   * @return
   */
  private boolean hasMSMS(Node n, Map<Integer, DataPoint[]> msmsData, int minSignals,
      final double cutOffFromMaxIntensity) {
    DataPoint[] signals = msmsData.get(toIndex(n));
    if (signals == null)
      return false;
    final double max = Arrays.stream(signals).mapToDouble(DataPoint::getIntensity).max().orElse(0);
    long dp = Arrays.stream(signals).mapToDouble(DataPoint::getIntensity)
        .filter(intensity -> intensity >= max * cutOffFromMaxIntensity).count();
    return dp >= minSignals;
  }

  private boolean hasMSMS(Node n, Map<Integer, DataPoint[]> msmsData, int minSignals) {
    DataPoint[] signals = msmsData.get(Integer.parseInt(n.getId()));
    return signals != null && signals.length >= minSignals;
  }
}
