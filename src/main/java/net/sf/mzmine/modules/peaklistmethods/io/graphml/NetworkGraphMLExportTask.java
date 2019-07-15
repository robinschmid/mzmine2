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

package net.sf.mzmine.modules.peaklistmethods.io.graphml;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.stream.file.FileSinkGraphML;
import com.google.common.util.concurrent.AtomicDouble;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual.AnnotationNetworkGenerator;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.files.FileAndPathUtil;

/**
 * Export a network (graphstream graph) to graphml, which is a widely used format
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class NetworkGraphMLExportTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private MZmineProject project;
  private PeakList peakList;
  private File fileCollapsed;
  private File fileFull;
  private AtomicDouble progress = new AtomicDouble(0);

  private Graph graph;
  private File graphFile;

  NetworkGraphMLExportTask(MZmineProject project, ParameterSet parameters) {

    this.project = project;
    peakList = parameters.getParameter(NetworkGraphMLExportParameters.PEAKLIST).getValue()
        .getMatchingPeakLists()[0];
    fileFull = parameters.getParameter(NetworkGraphMLExportParameters.FILE).getValue();
    fileFull = FileAndPathUtil.getRealFilePath(fileFull, "graphml");
    String name = FileAndPathUtil.eraseFormat(fileFull.getName());
    fileCollapsed =
        FileAndPathUtil.getRealFilePath(fileFull.getParentFile(), name + "_collapsed", "graphml");
  }

  public NetworkGraphMLExportTask(Graph graph, File file) {
    this.graph = graph;
    this.graphFile = file;
  }

  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Generating graphml for " + peakList.getName();
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    // direct graphml export
    if (graph != null) {
      logger.info("Exporting graphml to " + graphFile.getAbsolutePath());

      FileSinkGraphML saveGraphML = new FileSinkGraphML();
      try {
        if (graph.getNodeCount() > 0)
          saveGraphML.writeAll(graph, graphFile.getAbsolutePath());

        logger.info("Finished exporting graphml to " + graphFile.getAbsolutePath());
        setStatus(TaskStatus.FINISHED);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(e.getMessage());
      }
    } else {
      logger.info("Generating graphml for  " + peakList.getName());

      FileSinkGraphML saveGraphML = new FileSinkGraphML();
      try {
        AnnotationNetworkGenerator generator = new AnnotationNetworkGenerator();
        Graph graph = new MultiGraph("IIN networking");
        generator.createNewGraph(peakList.getRows(), graph, true, peakList.getR2RSimilarityMap());

        if (graph.getNodeCount() > 0)
          saveGraphML.writeAll(graph, fileFull.getAbsolutePath());

        generator.deleteAllCollapsedNodes();
        if (graph.getNodeCount() > 0)
          saveGraphML.writeAll(graph, fileCollapsed.getAbsolutePath());

        logger.info("Finished generating graphml for  " + peakList.getName());
        setStatus(TaskStatus.FINISHED);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage(e.getMessage());
      }
    }
  }

}
