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

package net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import net.sf.mzmine.modules.peaklistmethods.identification.gnpsresultsimport.GNPSResultsIdentity;
import net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis.GNPSResultsAnalysisTask.EdgeAtt;
import net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis.GNPSResultsAnalysisTask.EdgeType;
import net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis.GNPSResultsAnalysisTask.NodeAtt;

public class GnpsResults {

  private final Graph graph;
  private final Graph reducedGraph;
  private final HashMap<Integer, Integer> msmsData;
  private final HashMap<Integer, GNPSResultsIdentity> matches;

  public GnpsResults(Graph graph, Graph reducedg, HashMap<Integer, Integer> msmsData,
      HashMap<Integer, GNPSResultsIdentity> matches) {
    this.graph = graph;
    this.reducedGraph = reducedg;
    this.msmsData = msmsData;
    this.matches = matches;
  }

  public Graph getGraph() {
    return graph;
  }

  public HashMap<Integer, GNPSResultsIdentity> getMatches() {
    return matches;
  }

  public HashMap<Integer, Integer> getMsmsData() {
    return msmsData;
  }

  /**
   * Reduced by ion identity edges
   * 
   * @return
   */
  public Graph getReducedGraph() {
    return reducedGraph;
  }



  private Graph reduceGraph(Graph g) {
    // reduce all IIN to one neutral M node
    // NAME: M (netID)
    // connect all GNPS edges of all nodes to the neutral M node

    while (hasIINEdges(g)) {
    }
    graph.nodes().filter(n -> getIonIdentity(n) == null).forEach(n -> g.addNode(n));

    return null;
  }


  private Integer getIonNetworkID(Node n) {
    Object o = n.getAttribute(NodeAtt.NET_ID.key);
    if (o == null)
      return null;
    try {
      double d = Double.valueOf(o.toString());
      return Integer.valueOf((int) d);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Cannot convert net id to int", e);
    }
    return null;
  }

  private String getIonIdentity(Node n) {
    Object o = n.getAttribute(NodeAtt.IIN_ADDUCT.key);
    return o == null || o.toString().isEmpty() ? null : o.toString();
  }

  /**
   * Stream cosine edges without selfloops
   * 
   * @param n
   * @return
   */
  private Stream<Edge> streamCosineEdges(Node n) {
    return n.edges()
        .filter(e -> e.getAttribute(EdgeAtt.EDGE_TYPE.getKey()).equals(EdgeType.COSINE.key))
        .filter(e -> !e.getNode0().getId().equals(e.getNode1().getId()));
  }

  /**
   * stream ion identity networking edges
   * 
   * @param n
   * @return
   */
  private Stream<Edge> streamIINEdges(Node n) {
    return n.edges()
        .filter(e -> e.getAttribute(EdgeAtt.EDGE_TYPE.getKey()).equals(EdgeType.MS1_ANNOTATION.key))
        .filter(e -> !e.getNode0().getId().equals(e.getNode1().getId()));
  }

  /**
   * Still contains ion identity networking edges
   * 
   * @param g
   * @return
   */
  private boolean hasIINEdges(Graph g) {
    return g.edges().anyMatch(
        e -> e.getAttribute(EdgeAtt.EDGE_TYPE.getKey()).equals(EdgeType.MS1_ANNOTATION.key));
  }

  /**
   * Stream edges without selfloops
   * 
   * @param n
   * @return
   */
  private Stream<Edge> streamEdges(Node n) {
    return n.edges().filter(e -> !e.getNode0().getId().equals(e.getNode1().getId()));
  }

  private boolean hasMSMS(Node n, HashMap<Integer, Integer> msmsData, int minSignals) {
    Integer signals = msmsData.get(Integer.parseInt(n.getId()));
    return signals != null && signals >= minSignals;
  }

  private boolean isIdentified(Node n, HashMap<Integer, GNPSResultsIdentity> matches) {
    return matches.containsKey(Integer.parseInt(n.getId()));
  }

  private boolean hasEdges(Node n) {
    return streamEdges(n).count() > 0l;
  }

  private boolean hasCosineEdges(Node n) {
    return streamCosineEdges(n).count() > 0l;
  }
}
