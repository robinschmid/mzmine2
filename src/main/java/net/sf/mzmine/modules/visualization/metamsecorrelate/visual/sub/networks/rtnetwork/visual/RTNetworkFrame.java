package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.rtnetwork.visual;

import javax.swing.JFrame;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeatureFilter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;

public class RTNetworkFrame extends JFrame {

  private RTNetworkPanel contentPane;

  /**
   * Create the frame.
   */
  public RTNetworkFrame() {
    setTitle("Correlation networks");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    contentPane = new RTNetworkPanel();
    setContentPane(contentPane);
  }

  public void setUp(MZmineProject project, PeakList peakList, RTTolerance rtTolerance,
      boolean useMinFFilter, MinimumFeatureFilter minFFilter) {
    contentPane.setAll(project, peakList, rtTolerance, useMinFFilter, minFFilter, true);
  }



}
