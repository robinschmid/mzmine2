package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.externalsource;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceGraphML;
import net.sf.mzmine.framework.networks.NetworkPanel;

public class ExternalSourceNetworkPanel extends NetworkPanel {
  private static final Logger LOG = Logger.getLogger(ExternalSourceNetworkPanel.class.getName());

  /**
   * Create the panel.
   */
  public ExternalSourceNetworkPanel() {
    this(false);
  }

  public ExternalSourceNetworkPanel(boolean showTitle) {
    super("Retention time networks", showTitle);

    createNewGraph(
        "D:\\Daten\\UCSD\\Zdenek bile acids\\mzmine networking\\ProteoSAFe-FEATURE-BASED-MOLECULAR-NETWORKING-3fa85680-download_cytoscape_data\\FEATURE-BASED-MOLECULAR-NETWORKING-3fa85680-download_cytoscape_data-main.graphml");
  }


  public void createNewGraph(String string) {
    LOG.info("Loading file " + string);
    clear();


    FileSource fs = null;
    try {
      File file = new File(string);
      LOG.info("EXIST" + file.exists());
      fs = new FileSourceGraphML();
      fs.addSink(graph);
      fs.readAll(file.getAbsolutePath());
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "NOT LOADED", e);
    } finally {
      if (fs != null)
        fs.removeSink(graph);
    }
    // add id name
    showNodeLabels(true);

    LOG.info("Nodes " + graph.getNodeCount() + " edges=" + graph.getEdgeCount());
  }

}
