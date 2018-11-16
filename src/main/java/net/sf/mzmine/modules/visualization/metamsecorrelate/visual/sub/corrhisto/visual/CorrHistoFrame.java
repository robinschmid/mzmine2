package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.corrhisto.visual;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RCorrelationData;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.HistogramData;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.MultiHistogramPanel;

public class CorrHistoFrame extends JFrame {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private JPanel contentPane;
  private MultiHistogramPanel pnMultiHisto;

  // data
  private PeakList pkl;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          CorrHistoFrame frame = new CorrHistoFrame();
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the frame.
   */
  public CorrHistoFrame() {
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 1017, 619);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    JPanel panel = new JPanel();
    contentPane.add(panel, BorderLayout.CENTER);
    panel.setLayout(new BorderLayout(0, 0));

    pnMultiHisto = new MultiHistogramPanel("r");
    panel.add(pnMultiHisto);

    JPanel pnSett = new JPanel();
    panel.add(pnSett, BorderLayout.SOUTH);
    pnSett.setLayout(new BorderLayout(0, 0));

    JPanel panel_1 = new JPanel();
    pnSett.add(panel_1, BorderLayout.NORTH);
    panel_1.setLayout(new MigLayout("", "[]", "[]"));
  }

  public PeakList getPeakList() {
    return pkl;
  }

  /**
   * Set data and update
   * 
   * @param pkl
   */
  public void setPeakList(PeakList pkl) {
    this.pkl = pkl;
    setTitle("Group correlations of pkl " + pkl.getName());
    fireDataChanged();
  }

  public void fireDataChanged() {
    Thread t = new Thread(() -> {
      logger.info("Starting to fetch data for correlation histogram dialog");
      // all single feature to feature correlations
      DoubleArrayList single = new DoubleArrayList();
      // average row2row (over feature to feature) correlation
      DoubleArrayList avg = new DoubleArrayList();
      // total correlation of all data points of two rows
      DoubleArrayList total = new DoubleArrayList();

      R2RCorrelationData.streamFrom(pkl).filter(R2RCorrelationData::hasFeatureShapeCorrelation)
          .forEach(r2r -> {
            avg.add(r2r.getAvgShapeR());
            total.add(r2r.getTotalCorr().getR());
            // all single features
            for (CorrelationData f2f : r2r.getCorrPeakShape().values())
              if (f2f != null)
                single.add(f2f.getR());
          });

      // update data in histograms
      HistogramData[] data = new HistogramData[3];
      data[0] = new HistogramData(total.toDoubleArray());
      data[1] = new HistogramData(avg.toDoubleArray());
      data[2] = new HistogramData(single.toDoubleArray());
      String[] title = new String[] {"Total r2r", "Average r2r", "Single f2f"};
      pnMultiHisto.setTitle(title);
      pnMultiHisto.setData(data);
      logger.info("Data of correlation histogram dialog was fetched");
    });
    t.setDaemon(true);
    t.start();
  }
}
