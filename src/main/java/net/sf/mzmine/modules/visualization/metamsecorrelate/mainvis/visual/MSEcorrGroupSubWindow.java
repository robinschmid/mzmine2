package net.sf.mzmine.modules.visualization.metamsecorrelate.mainvis.visual;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.GridLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartPanel;

public class MSEcorrGroupSubWindow extends JFrame {

  private JPanel contentPane;
  private JPanel pnBoxPlot;
  private JPanel pnMaxICorr;
  private JPanel pnShapeCorr, pnShape, pnCorrColumns;
  private JPanel pnTotalShapeCorr;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          MSEcorrGroupSubWindow frame = new MSEcorrGroupSubWindow();
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
  public MSEcorrGroupSubWindow() {
    setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    setBounds(100, 100, 853, 586);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    JPanel panel = new JPanel();
    contentPane.add(panel, BorderLayout.CENTER);
    panel.setLayout(new BorderLayout(0, 0));


    JSplitPane splitPane = new JSplitPane();
    splitPane.setResizeWeight(0.2);
    splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    panel.add(splitPane, BorderLayout.CENTER);

    JSplitPane splitPane2 = new JSplitPane();
    splitPane2.setResizeWeight(0.50);
    splitPane2.setOrientation(JSplitPane.VERTICAL_SPLIT);


    JPanel bottom = new JPanel(new GridLayout(1, 2));
    JPanel middle = new JPanel(new GridLayout(1, 3));
    JPanel top = new JPanel(new BorderLayout());
    splitPane.setLeftComponent(top);
    splitPane.setRightComponent(splitPane2);
    splitPane2.setLeftComponent(middle);
    splitPane2.setRightComponent(bottom);

    // top
    pnCorrColumns = new JPanel(new BorderLayout());
    top.add(pnCorrColumns, BorderLayout.CENTER);

    // middle
    pnShape = new JPanel(new BorderLayout());
    pnShapeCorr = new JPanel(new BorderLayout());
    pnTotalShapeCorr = new JPanel(new BorderLayout());
    middle.add(pnShape);
    middle.add(pnShapeCorr);
    middle.add(pnTotalShapeCorr);

    // bottom
    pnBoxPlot = new JPanel(new BorderLayout());
    pnMaxICorr = new JPanel(new BorderLayout());
    bottom.add(pnBoxPlot);
    bottom.add(pnMaxICorr);
  }

  public void setBoxPlot(ChartPanel chart) {
    addChartToPanel(pnBoxPlot, chart);
  }

  public void setShapeCorrPlot(ChartPanel chart) {
    addChartToPanel(pnShapeCorr, chart);
  }

  public void setShapePlot(ChartPanel chart) {
    addChartToPanel(pnShape, chart);
  }

  public void setTotalShapeCorrPlot(ChartPanel chart) {
    addChartToPanel(pnTotalShapeCorr, chart);
  }

  public void setMaxICorrPlot(ChartPanel chart) {
    addChartToPanel(pnMaxICorr, chart);
  }

  public void setCorrColumnsChart(ChartPanel chart) {
    addChartToPanel(pnCorrColumns, chart);
  }

  private void addChartToPanel(JPanel pn, ChartPanel chart) {
    pn.removeAll();
    pn.add(chart, BorderLayout.CENTER);
    pn.getParent().revalidate();
    pn.getParent().repaint();
  }

  public JPanel getPnBoxPlot() {
    return pnBoxPlot;
  }

  public JPanel getPnMaxICorr() {
    return pnMaxICorr;
  }

  public JPanel getPnShapeCorr() {
    return pnShapeCorr;
  }

  public JPanel getPnTotalShapeCorr() {
    return pnTotalShapeCorr;
  }

}
