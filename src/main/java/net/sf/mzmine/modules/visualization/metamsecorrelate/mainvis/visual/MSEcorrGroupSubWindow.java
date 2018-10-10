package net.sf.mzmine.modules.visualization.metamsecorrelate.mainvis.visual;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import org.jfree.chart.ChartPanel;

/**
 * Holds more charts for data reviewing
 * 
 * @author Robin Schmid
 *
 */
public class MSEcorrGroupSubWindow extends JFrame {

  private JPanel contentPane;
  private JPanel pnBoxPlot;
  private JPanel pnMaxICorr;
  private JPanel pnShapeCorr, pnShape, pnCorrColumns;
  private JPanel pnTotalShapeCorr;
  private MSEcorrGroupWindow mainWnd;

  /**
   * Create the frame.
   */
  public MSEcorrGroupSubWindow(MSEcorrGroupWindow mainWnd) {
    this.mainWnd = mainWnd;
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

    addKeyBindings();
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


  private void addKeyBindings() {
    JPanel pn = (JPanel) this.getContentPane();

    // group and row controls
    final String[] commands = {"released UP", "released DOWN", "released LEFT", "released RIGHT",
        "released PLUS", "released MINUS"};
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[0]), commands[0]);
    pn.getActionMap().put(commands[0], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        mainWnd.prevRow();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[1]), commands[1]);
    pn.getActionMap().put(commands[1], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        mainWnd.nextRow();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[2]), commands[2]);
    pn.getActionMap().put(commands[2], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        mainWnd.prevGroup();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[3]), commands[3]);
    pn.getActionMap().put(commands[3], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        mainWnd.nextGroup();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), commands[4]);
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0), commands[4]);
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK), commands[4]);
    pn.getActionMap().put(commands[4], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        mainWnd.nextRaw();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), commands[5]);
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), commands[5]);
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.SHIFT_DOWN_MASK), commands[5]);
    pn.getActionMap().put(commands[5], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        mainWnd.prevRaw();
      }
    });
  }
}
