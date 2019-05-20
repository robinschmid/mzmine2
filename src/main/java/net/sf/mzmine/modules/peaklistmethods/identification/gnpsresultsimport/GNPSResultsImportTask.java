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

package net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceGraphML;
import com.google.common.util.concurrent.AtomicDouble;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.HeadLessDesktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.datastructure.R2RMap;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.MS2Similarity;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.R2RMS2Similarity;
import net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsIdentity.ATT;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

public class GNPSResultsImportTask extends AbstractTask {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  private PeakList peakList;
  private File file;

  private AtomicDouble progress = new AtomicDouble(0);
  private ParameterSet parameters;
  private String step = "Importing GNPS results for";

  public enum EdgeAtt {
    EDGE_TYPE("EdgeType", String.class), // edgetype
    EDGE_SCORE("EdgeScore", Double.class), EDGE_ANNOTATION("EdgeAnnotation", String.class);

    private String key;
    private Class c;

    private EdgeAtt(String key, Class c) {
      this.c = c;
      this.key = key;
    }

    public Class getValueClass() {
      return c;
    }

    public String getKey() {
      return key;
    }
  }

  public enum EdgeType {
    MS1_ANNOTATION("MS1 annotation"), COSINE("Cosine");
    private String key;

    private EdgeType(String key) {
      this.key = key;
    }
  }

  /**
   * @param parameters
   * @param peakList
   */
  public GNPSResultsImportTask(ParameterSet parameters, PeakList peakList) {
    this.parameters = parameters;
    this.peakList = peakList;
    file = parameters.getParameter(GNPSResultsImportParameters.FILE).getValue();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  /**
   * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return step + " " + peakList + " in file " + file;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    logger.info("Importing GNPS results for " + peakList);

    // remove zeros from edge ids (GNPS export error)
    removeZeroIDFromEdge(file);

    Graph graph = new DefaultGraph("GNPS");
    if (importGraphData(graph, file)) {

      progress.set(0d);
      step = "Importing library matches";
      logger.info("Starting to import library matches");
      // import library matches from nodes
      importLibraryMatches(graph);

      // import MS2 similarity from GNPS edges
      progress.set(0d);
      step = "Importing MS2 similarity edges";
      logger.info("Starting to import MS2 similarity edges");
      importMS2SimilarityEdges(graph);

      // Add task description to peakList
      ((SimplePeakList) peakList).addDescriptionOfAppliedTask(
          new SimplePeakListAppliedMethod("GNPS results import", parameters));

      // Repaint the window to reflect the change in the peak list
      Desktop desktop = MZmineCore.getDesktop();
      if (!(desktop instanceof HeadLessDesktop))
        desktop.getMainWindow().repaint();

      logger.info("Finished import of GNPS results for " + peakList);
      setStatus(TaskStatus.FINISHED);
    } else {
      setErrorMessage("Error while importing graphml file: " + file.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
    }
  }

  /**
   * All edges have id=0 - this causes an exception. Replace all zero ids and save the file
   * 
   * @param file2
   */
  private void removeZeroIDFromEdge(File file) {
    try {
      logger.info("replacing zero ids in graphml");
      Path path = Paths.get(file.getAbsolutePath());
      Stream<String> lines = Files.lines(path);
      List<String> replaced =
          lines.map(line -> line.replaceAll("edge id=\"0\"", "edge")).collect(Collectors.toList());
      Files.write(path, replaced);
      lines.close();
      logger.info("zero ids in graphml replaces");
    } catch (IOException e) {
      logger.log(Level.SEVERE, "graphml NOT LOADED: " + file.getAbsolutePath(), e);
      setErrorMessage("Cannot load graphml file: " + file.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      cancel();
    }
  }

  private void importMS2SimilarityEdges(Graph graph) {
    AtomicInteger missingRows = new AtomicInteger(0);
    AtomicInteger r2rSimilarities = new AtomicInteger(0);

    R2RMap<R2RMS2Similarity> map = peakList.getR2RSimilarityMap();

    final int size = graph.getEdgeCount();
    AtomicInteger done = new AtomicInteger(0);
    graph.edges().forEach(edge -> {
      // edge type is gnps? (Cosine)
      String type = (String) edge.getAttribute(EdgeAtt.EDGE_TYPE.getKey());
      if (type.equals(EdgeType.COSINE.key)) {
        // get rows
        int ida = Integer.parseInt(edge.getSourceNode().getId());
        int idb = Integer.parseInt(edge.getTargetNode().getId());
        PeakListRow a = peakList.findRowByID(ida);
        PeakListRow b = peakList.findRowByID(idb);
        if (a != null && b != null) {
          r2rSimilarities.getAndIncrement();
          // get existing similarity
          R2RMS2Similarity similarity = map != null ? map.get(a, b) : null;
          // create new if empty
          if (similarity == null) {
            similarity = new R2RMS2Similarity(a, b);
          }
          // add gnps results
          double cosine = (double) edge.getAttribute(EdgeAtt.EDGE_SCORE.getKey());
          MS2Similarity sim = new MS2Similarity(cosine, 1);
          similarity.addGNPSSim(sim);

          // add r2r similarity
          peakList.addR2RSimilarity(a, b, similarity);
        } else
          missingRows.getAndIncrement();
      }
      // increment
      done.getAndIncrement();
      progress.set(done.get() / (double) size);
    });


    if (missingRows.get() > 0)
      logger.info(missingRows.get()
          + " rows (features) that were present in the GNPS results were not found in the peakList. Check if you selected the correct peak list, did some filtering or applied renumbering (IDs have to match).");

    logger.info(r2rSimilarities.get() + " cosine score edges");
  }

  private void importLibraryMatches(Graph graph) {
    AtomicInteger missingRows = new AtomicInteger(0);
    AtomicInteger libraryMatches = new AtomicInteger(0);
    // go through all nodes and add info
    final int size = graph.getNodeCount();
    AtomicInteger done = new AtomicInteger(0);
    graph.nodes().forEach(node -> {
      int id = Integer.parseInt(node.getId());
      // has library match?
      String compoundName = (String) node.getAttribute(ATT.COMPOUND_NAME.getKey());
      PeakListRow row = peakList.findRowByID(id);
      if (row != null) {
        if (compoundName != null && !compoundName.isEmpty()) {
          libraryMatches.getAndIncrement();
          // add identity
          String adduct = (String) node.getAttribute(ATT.ADDUCT.getKey());

          // find all results
          HashMap<String, Object> results = new HashMap<>();
          for (ATT att : ATT.values()) {
            Object result = node.getAttribute(att.getKey());
            if (result != null) {
              results.put(att.getKey(), result);
            }
          }

          // add identity
          GNPSResultsIdentity identity = new GNPSResultsIdentity(results, compoundName, adduct);
          row.addPeakIdentity(identity, true);
          // Notify the GUI about the change in the project
          MZmineCore.getProjectManager().getCurrentProject().notifyObjectChanged(row, false);
        }
      } else
        missingRows.getAndIncrement();

      // increment
      done.getAndIncrement();
      progress.set(done.get() / (double) size);
    });

    if (missingRows.get() > 0)
      logger.info(missingRows.get()
          + " rows (features) that were present in the GNPS results were not found in the peakList. Check if you selected the correct peak list, did some filtering or applied renumbering (IDs have to match).");

    logger.info(libraryMatches.get() + " rows found with library matches");
  }

  private boolean importGraphData(Graph graph, File file) {
    boolean result = true;
    FileSource fs = null;
    logger.info("Importing graphml data");
    try {
      fs = new FileSourceGraphML();
      fs.addSink(graph);
      fs.readAll(file.getAbsolutePath());
      logger.info(() -> MessageFormat.format("GNPS results: nodes={0} edges={1}",
          graph.getNodeCount(), graph.getEdgeCount()));
    } catch (IOException e) {
      logger.log(Level.SEVERE, "NOT LOADED", e);
      setErrorMessage("Cannot load graphml file: " + file.getAbsolutePath());
      setStatus(TaskStatus.ERROR);
      cancel();
      result = false;
    } finally {
      if (fs != null)
        fs.removeSink(graph);
    }
    return result;
  }

}
