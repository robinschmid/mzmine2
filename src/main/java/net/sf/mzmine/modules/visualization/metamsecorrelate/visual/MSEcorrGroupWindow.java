package net.sf.mzmine.modules.visualization.metamsecorrelate.visual;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import org.apache.commons.math.stat.regression.SimpleRegression;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedRangeCategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.FeatureShapeCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.GroupCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MSEGroupedPeakList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroupList;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.RowCorrelationData;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.pseudospectra.PseudoSpectrum;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.table.GroupedPeakListTable;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.table.GroupedPeakListTableModel;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.chartexport.ChartExportUtil;
import net.sf.mzmine.util.chartthemes.ChartThemeFactory;
import net.sf.mzmine.util.chartthemes.MyStandardChartTheme;

public class MSEcorrGroupWindow extends JFrame implements ComponentListener {
  // Theme for charts
  private final MyStandardChartTheme theme;
  //
  private final Paint colors[];
  // data
  private MSEGroupedPeakList peakList;
  private PKLRowGroupList groups;
  private int currentIndex;
  private MZmineProject project;
  // visual
  private JPanel contentPane;
  private JPanel pnPeakShapeCorr;
  private JPanel pnIntensityCorr;
  private GroupedPeakListTable tableGroupMembers;
  private JTextField txtGroup;
  private JTextField txtRow;
  private JButton btnPreviousRow;
  private JPanel pnPeakShapeCorrView;
  private JPanel pnPeakShapeView;
  private JPanel pnPeakShapeCorrAllView;
  private JSplitPane split;
  private JCheckBox cbUseSampleGroups;
  private JCheckBox cbSampleSummary;
  private JScrollPane mainScroll;
  private JPanel panel_3;
  private JCheckBox cbAutoSkipGSize;
  private JTextField txtMinGroupSize;
  private JPanel panel_4;
  private JPanel panel_5;
  private JButton btnJumpToRT;
  private JTextField txtJumpToRT;
  private JPanel panel_6;
  private JCheckBox cbAutoRawFile;
  private JCheckBox cbShowPseudoSpectrum;
  private JPanel panel_7;
  private JCheckBox cbSumPseudoSpectrum;

  /**
   * Create the frame.
   * 
   * @param index
   * @param groups
   * @param peakList
   */
  public MSEcorrGroupWindow(MZmineProject project, final MSEGroupedPeakList peakList,
      PKLRowGroupList groups, int index) {
    // data
    this.peakList = peakList;
    this.groups = groups;
    this.currentIndex = index;
    this.project = project;
    // theme
    theme = ChartThemeFactory.createBlackNWhiteTheme();
    colors = PKLRowGroup.colors;

    //
    this.addComponentListener(this);
    // peak list table parameters
    ParameterSet peakListTableParameters =
        MZmineCore.getConfiguration().getModuleParameters(PeakListTableModule.class);
    //
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 911, 480);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    mainScroll = new JScrollPane();
    contentPane.add(mainScroll, BorderLayout.CENTER);

    JPanel pnContent = new JPanel();
    mainScroll.setViewportView(pnContent);
    pnContent.setLayout(new BorderLayout(0, 0));

    split = new JSplitPane();
    split.setResizeWeight(0.5);
    split.setOrientation(JSplitPane.VERTICAL_SPLIT);
    pnContent.add(split, BorderLayout.CENTER);

    pnPeakShapeCorr = new JPanel();
    split.setLeftComponent(pnPeakShapeCorr);
    pnPeakShapeCorr.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

    pnPeakShapeView = new JPanel();
    pnPeakShapeCorr.add(pnPeakShapeView);
    pnPeakShapeView.setLayout(new BorderLayout(0, 0));

    pnPeakShapeCorrView = new JPanel();
    pnPeakShapeCorr.add(pnPeakShapeCorrView);
    pnPeakShapeCorrView.setLayout(new BorderLayout(0, 0));

    pnPeakShapeCorrAllView = new JPanel();
    pnPeakShapeCorr.add(pnPeakShapeCorrAllView);
    pnPeakShapeCorrAllView.setLayout(new BorderLayout(0, 0));

    pnIntensityCorr = new JPanel();
    split.setRightComponent(pnIntensityCorr);
    pnIntensityCorr.setLayout(new BorderLayout(0, 0));

    JPanel pnTable = new JPanel();
    pnContent.add(pnTable, BorderLayout.SOUTH);
    pnTable.setLayout(new BorderLayout(0, 0));

    JPanel pnMenu = new JPanel();
    FlowLayout flowLayout = (FlowLayout) pnMenu.getLayout();
    flowLayout.setVgap(0);
    flowLayout.setAlignment(FlowLayout.LEFT);
    flowLayout.setHgap(0);
    pnTable.add(pnMenu, BorderLayout.NORTH);

    JPanel panel = new JPanel();
    pnMenu.add(panel);
    panel.setLayout(new MigLayout("", "[]", "[][]"));

    JLabel lblGroup = new JLabel("Group");
    lblGroup.setFont(new Font("Tahoma", Font.BOLD, 11));
    panel.add(lblGroup, "cell 0 0");

    JButton btnPreviousGroup = new JButton("Previous");
    btnPreviousGroup.setToolTipText("LEFT key");
    panel.add(btnPreviousGroup, "flowx,cell 0 1");

    txtGroup = new JTextField();
    txtGroup.setHorizontalAlignment(SwingConstants.RIGHT);
    txtGroup.setText("0");
    panel.add(txtGroup, "cell 0 1");
    txtGroup.setColumns(4);

    JPanel panel_1 = new JPanel();
    pnMenu.add(panel_1);
    panel_1.setLayout(new MigLayout("", "[][][]", "[][]"));

    JLabel lblRow = new JLabel("Row");
    lblRow.setFont(new Font("Tahoma", Font.BOLD, 11));
    panel_1.add(lblRow, "flowy,cell 0 0");

    btnPreviousRow = new JButton("Previous");
    btnPreviousRow.setToolTipText("DOWN key");
    btnPreviousRow.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        PKLRowGroup g = peakList.getLastViewedGroup();
        if (g.getLastViewedRowI() > 0)
          setCurrentRowView(g.getLastViewedRowI() - 1);
      }
    });
    panel_1.add(btnPreviousRow, "cell 0 1");

    txtRow = new JTextField();
    panel_1.add(txtRow, "cell 1 1");
    txtRow.setText("0");
    txtRow.setHorizontalAlignment(SwingConstants.RIGHT);
    txtRow.setColumns(4);

    JButton btnNextRow = new JButton("Next");
    btnNextRow.setToolTipText("UP key");
    btnNextRow.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        PKLRowGroup g = peakList.getLastViewedGroup();
        if (g.getLastViewedRowI() + 1 < g.size())
          setCurrentRowView(g.getLastViewedRowI() + 1);
      }
    });
    panel_1.add(btnNextRow, "cell 2 1");

    JButton btnNextGroup = new JButton("Next");
    btnNextGroup.setToolTipText("RIGHT key");
    btnNextGroup.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        if (peakList.getLastViewedIndex() + 1 < peakList.getGroups().size()) {
          setCurrentGroupView(peakList.getLastViewedIndex() + 1);
        }
      }
    });
    panel.add(btnNextGroup, "cell 0 1");

    panel_3 = new JPanel();
    pnMenu.add(panel_3);
    panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.Y_AXIS));

    panel_4 = new JPanel();
    FlowLayout flowLayout_1 = (FlowLayout) panel_4.getLayout();
    flowLayout_1.setAlignment(FlowLayout.LEFT);
    flowLayout_1.setVgap(0);
    flowLayout_1.setHgap(0);
    panel_3.add(panel_4);

    cbAutoSkipGSize = new JCheckBox("Auto skip size<");
    cbAutoSkipGSize.setToolTipText("Auto skip groups with size smaller than x.");
    cbAutoSkipGSize.setSelected(true);
    panel_4.add(cbAutoSkipGSize);

    txtMinGroupSize = new JTextField();
    txtMinGroupSize.setToolTipText("Auto skip groups with size smaller than x.");
    panel_4.add(txtMinGroupSize);
    txtMinGroupSize.setText("2");
    txtMinGroupSize.setColumns(2);

    panel_5 = new JPanel();
    FlowLayout flowLayout_2 = (FlowLayout) panel_5.getLayout();
    flowLayout_2.setAlignment(FlowLayout.LEFT);
    flowLayout_2.setHgap(0);
    flowLayout_2.setVgap(0);
    panel_3.add(panel_5);

    txtJumpToRT = new JTextField();
    txtJumpToRT.setToolTipText("Jump to next group after RT=x min.");
    txtJumpToRT.setHorizontalAlignment(SwingConstants.RIGHT);
    txtJumpToRT.setText("0");
    panel_5.add(txtJumpToRT);
    txtJumpToRT.setColumns(4);

    btnJumpToRT = new JButton("Jump to (min)");
    btnJumpToRT.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        try {
          jumpToGroupAtRT(Double.valueOf(txtJumpToRT.getText()));
        } catch (Exception ex) {
        }
      }
    });
    btnJumpToRT.setToolTipText("Jump to next group after RT=x min.");
    panel_5.add(btnJumpToRT);

    JPanel panel_2 = new JPanel();
    pnMenu.add(panel_2);
    panel_2.setLayout(new BoxLayout(panel_2, BoxLayout.Y_AXIS));

    cbUseSampleGroups = new JCheckBox("Use sample groups");
    cbUseSampleGroups.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        plotIProfile();
      }
    });
    cbUseSampleGroups
        .setToolTipText("Use sample groups or rows as categories for intensity boxplots.");
    cbUseSampleGroups.setSelected(true);
    panel_2.add(cbUseSampleGroups);

    cbAutoRawFile = new JCheckBox("Auto raw file");
    panel_2.add(cbAutoRawFile);
    cbAutoRawFile.setToolTipText(
        "(Switch raw files with + and - keys) Show peak shapes and correlation data of the raw file with the highest number of peaks.");
    cbAutoRawFile.setSelected(true);

    panel_6 = new JPanel();
    pnMenu.add(panel_6);
    panel_6.setLayout(new BoxLayout(panel_6, BoxLayout.Y_AXIS));

    cbShowPseudoSpectrum = new JCheckBox("Show pseudo spectrum");
    cbShowPseudoSpectrum.setSelected(true);
    cbShowPseudoSpectrum.setToolTipText("Show a speudo spectrum (of one raw file; or summed)");
    panel_6.add(cbShowPseudoSpectrum);

    cbSumPseudoSpectrum = new JCheckBox("Sum pseudo spectrum");
    cbSumPseudoSpectrum.setToolTipText("Take max height of each row");
    cbSumPseudoSpectrum.setSelected(true);
    panel_6.add(cbSumPseudoSpectrum);

    panel_7 = new JPanel();
    pnMenu.add(panel_7);
    panel_7.setLayout(new BoxLayout(panel_7, BoxLayout.Y_AXIS));

    cbSampleSummary = new JCheckBox("Sample summary");
    panel_7.add(cbSampleSummary);
    cbSampleSummary.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        plotPeakShapeCorrelation();
      }
    });
    cbSampleSummary.setToolTipText("Average peak shape correlation column chart.");

    btnPreviousGroup.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        if (peakList.getLastViewedIndex() > 0) {
          setCurrentGroupView(peakList.getLastViewedIndex() - 1);
        }
      }
    });

    JScrollPane scrollPane = new JScrollPane();
    pnTable.add(scrollPane, BorderLayout.CENTER);

    tableGroupMembers = new GroupedPeakListTable(this, peakListTableParameters, peakList);
    scrollPane.setViewportView(tableGroupMembers);

    /*
     * JFreeChart chart = createCombinedChart(); ChartPanel panel = new ChartPanel(chart, true,
     * true, true, false, true); contentPane.add(panel, BorderLayout.CENTER);
     * 
     * 
     * chart = createCombinedChart(); panel = new ChartPanel(chart, true, true, true, false, true);
     * contentPane.add(panel, BorderLayout.SOUTH);
     */

    // key bindings
    setVisible(true);
    addKeyBindings();
    setCurrentGroupView(index);
  }

  /**
   * Jump to the next group with averageRT >= rt (or last group)
   * 
   * @param rt
   */
  protected void jumpToGroupAtRT(double rt) {
    for (int i = 0; i < groups.size(); i++) {
      if (groups.get(i).getCenterRT() >= rt) {
        setCurrentGroupView(i);
        return;
      }
    }
    // else set last group
    setCurrentGroupView(groups.size() - 1);
  }

  private void addKeyBindings() {
    JPanel pn = (JPanel) this.getContentPane();
    //
    InputMap im = getMainScroll().getInputMap();
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "scrollDown");
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "scrollUp");

    ActionMap am = getMainScroll().getActionMap();
    am.put("scrollDown", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {}
    });
    am.put("scrollUp", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {}
    });

    // group and row controls
    final String[] commands = {"released UP", "released DOWN", "released LEFT", "released RIGHT",
        "released PLUS", "released MINUS"};
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[0]), commands[0]);
    pn.getActionMap().put(commands[0], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        PKLRowGroup g = peakList.getLastViewedGroup();
        if (g.getLastViewedRowI() > 0)
          setCurrentRowView(g.getLastViewedRowI() - 1);
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[1]), commands[1]);
    pn.getActionMap().put(commands[1], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        PKLRowGroup g = peakList.getLastViewedGroup();
        if (g.getLastViewedRowI() + 1 < g.size())
          setCurrentRowView(g.getLastViewedRowI() + 1);
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[2]), commands[2]);
    pn.getActionMap().put(commands[2], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        if (peakList.getLastViewedIndex() > 0)
          setCurrentGroupView(peakList.getLastViewedIndex() - 1);
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[3]), commands[3]);
    pn.getActionMap().put(commands[3], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        if (peakList.getLastViewedIndex() + 1 < peakList.getGroups().size())
          setCurrentGroupView(peakList.getLastViewedIndex() + 1);
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
        PKLRowGroup g = peakList.getLastViewedGroup();
        if (g.getLastViewedRawFileI() + 1 < g.size())
          setCurrentRawView(g.getLastViewedRawFileI() + 1);
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
        PKLRowGroup g = peakList.getLastViewedGroup();
        if (g.getLastViewedRawFileI() > 0)
          setCurrentRawView(g.getLastViewedRawFileI() - 1);
      }
    });
  }

  /**
   * create all plots and show members in table
   * 
   * @param index
   */
  private void setCurrentGroupView(int index) {
    // skip groups with size smaller than X
    if (getCbAutoSkipGSize().isSelected()) {
      try {
        int lastIndex = peakList.getLastViewedIndex();
        int min = Integer.valueOf(getTxtMinGroupSize().getText());
        while (groups.get(index).size() < min && index + 1 < groups.size() && index - 1 > 0)
          index += index < lastIndex ? -1 : +1;
      } catch (Exception ex) {
      }
    }
    // set index to peaklist
    peakList.setLastViewedIndex(index);

    // set text
    getTxtGroup().setText(String.valueOf(index));
    getTxtRow().setText(String.valueOf(peakList.getLastViewedGroup().getLastViewedRowI()));

    // auto show raw file containing lastViewed row
    checkAutoShowRawFile();
    // show the group in the table
    GroupedPeakListTableModel model = (GroupedPeakListTableModel) tableGroupMembers.getModel();
    model.fireTableDataChanged();
    //
    renewAllPlots(true, true, true);
  }

  /**
   * check for auto show raw file that contains the lastViewed row of the last viewed group
   */
  private boolean checkAutoShowRawFile() {
    if (getCbAutoRawFile().isSelected()) {
      PKLRowGroup g = peakList.getLastViewedGroup();
      PeakListRow row = g.getLastViewedRow();

      int maxI = 0;
      int maxSize = 0;
      for (int i = 0; i < g.getRaw().length; i++) {
        RawDataFile raw = g.getRaw()[i];
        if (row.getPeak(raw) != null && row.getPeaks().length > maxSize) {
          maxI = i;
          maxSize = row.getPeaks().length;
        }
      }
      // set index
      g.setLastViewedRawFileI(maxI);
      return true;
    }
    return false;
  }

  /**
   * sets the current raw view and calls to renew all plots
   * 
   * @param i
   */
  protected void setCurrentRawView(int i) {
    PKLRowGroup g = peakList.getLastViewedGroup();
    g.setLastViewedRawFileI(i);
    renewAllPlots(true, true, false);
  }

  /**
   * sets the row of interest and renews all plots
   * 
   * @param i
   */
  protected void setCurrentRowView(int i) {
    getTxtRow().setText(String.valueOf(i));

    PKLRowGroup g = peakList.getLastViewedGroup();
    g.setLastViewedRowI(i);
    // auto show raw file containing lastViewed row
    boolean renewRaw = checkAutoShowRawFile();
    renewAllPlots(renewRaw, true, true);
  }

  /**
   * renews all plots: IProfile, peak shapes, peak shape correlation
   */
  public void renewAllPlots(boolean peakShapes, boolean peakShapeCorr, boolean iProfile) {
    // plot peak shapes
    if (peakShapes)
      plotPeakShapes();
    // show correlation of f1 to all other features
    if (peakShapeCorr) {
      plotPeakShapeCorrelation();
      // plot a pseudo spectrum
      if (getCbShowPseudoSpectrum().isSelected())
        plotPseudoSpectrum();
    }
    // plotIProfile
    if (iProfile)
      plotIProfile();
  }

  /**
   * plots the peak shapes of each row of one selected raw file
   */
  private void plotPeakShapes() {
    // get group
    PKLRowGroup g = peakList.getLastViewedGroup();
    getPnPeakShapeView().removeAll();
    if (g != null) {
      // PeakListRow row = g.getLastViewedRow();
      RawDataFile raw = g.getLastViewedRawFile();

      // data set
      XYSeriesCollection data = new XYSeriesCollection();
      // add plot
      String sg =
          String.valueOf(project.getParameterValue(peakList.getSampleGroupsParameter(), raw));
      String title = sg + "(" + raw.getName() + ")";
      JFreeChart chart = ChartFactory.createXYLineChart(title, "retention time | min", "Intensity",
          data, PlotOrientation.VERTICAL, true, true, false);
      XYItemRenderer renderer = chart.getXYPlot().getRenderer();
      theme.apply(chart);
      // formating
      NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
      NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
      NumberAxis xAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
      NumberAxis yAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
      xAxis.setNumberFormatOverride(rtFormat);
      yAxis.setNumberFormatOverride(intensityFormat);
      //
      chart.setNotify(false);
      // for each row (r=color)
      for (int r = 0; r < g.size(); r++) {
        PeakListRow row = g.get(r);
        // peak
        Feature f1 = row.getPeak(raw);
        if (f1 != null) {
          // series
          XYSeries series = new XYSeries(row.getID());
          // peak data
          int[] dp = f1.getScanNumbers();
          for (int i : dp) {
            double x = raw.getScan(i).getRetentionTime();
            DataPoint cdp = f1.getDataPoint(i);
            if (cdp != null) {
              double y = cdp.getIntensity();
              series.add(x, y);
            }
          }
          data.addSeries(series);
          renderer.setSeriesPaint(data.getSeriesCount() - 1, colors[r % colors.length]);
        }
      }
      // apply theme
      chart.setNotify(true);
      chart.fireChartChanged();
      // add to panel
      ChartPanel cp = createChartPanel(chart);

      getPnPeakShapeView().add(cp, BorderLayout.CENTER);

      getPnPeakShapeCorr().validate();
    }
  }

  /**
   * peak shape correlation of selected row to all other rows in selected raw file
   */
  private void plotPeakShapeCorrelation() {
    // get group
    PKLRowGroup g = peakList.getLastViewedGroup();
    getPnPeakShapeCorrView().removeAll();
    getPnPeakShapeCorrAllView().removeAll();
    if (g != null) {
      //
      PeakListRow row = g.getLastViewedRow();
      int rowI = g.getLastViewedRowI();
      GroupCorrelationData corr = g.getCorr(rowI);
      int rawI = g.getLastViewedRawFileI();
      RawDataFile raw = g.getLastViewedRawFile();

      // first chart: correlation of row to all other rows in rawI
      // data set
      XYSeriesCollection data = new XYSeriesCollection();
      // title
      String sg =
          String.valueOf(project.getParameterValue(peakList.getSampleGroupsParameter(), raw));
      String title = "Row " + row.getID() + " corr in: " + sg + "(" + raw.getName() + ")";
      // create chart
      JFreeChart chart =
          ChartFactory.createScatterPlot(title, "Intensity (row: " + row.getID() + ")",
              "Intensity (other rows)", data, PlotOrientation.VERTICAL, true, true, false);
      XYItemRenderer renderer = chart.getXYPlot().getRenderer();
      theme.apply(chart);
      // formating
      NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
      NumberAxis xAxis = (NumberAxis) chart.getXYPlot().getDomainAxis();
      NumberAxis yAxis = (NumberAxis) chart.getXYPlot().getRangeAxis();
      xAxis.setNumberFormatOverride(intensityFormat);
      yAxis.setNumberFormatOverride(intensityFormat);
      //
      chart.setNotify(false);
      // for each row: correlation of row to
      for (int i = 0; i < g.size(); i++) {
        PeakListRow trow = g.get(i);
        if (rowI != i) {
          // get correlation data (row to row)
          RowCorrelationData corrRows = corr.getCorrelationToRowI(i);
          // get correlation of feature-feature in selected raw file
          FeatureShapeCorrelationData fCorr = corrRows.getCorrPeakShape()[rawI];
          // add series
          if (fCorr != null && fCorr.getReg() != null && fCorr.getData() != null) {
            data.addSeries(regressionToSeries(fCorr, String.valueOf(trow.getID())));
            // add regression line
            XYLineAnnotation line = regressionToAnnotation(fCorr);
            renderer.addAnnotation(line);
            // set colors
            renderer.setSeriesPaint(data.getSeriesCount() - 1, colors[i % colors.length]);
          }
        }
      }

      // add chart to panel
      chart.setNotify(true);
      chart.fireChartChanged();
      ChartPanel cp = createChartPanel(chart);
      getPnPeakShapeCorrView().add(cp, BorderLayout.CENTER);

      // spectrum or column chart of all correlations
      if (!getCbShowPseudoSpectrum().isSelected()) {
        // add plot of all correlations (this row to all other rows in all raw files)
        // data set
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // go through all raw files
        for (int r = 0; r < peakList.getRawDataFiles().length; r++) {
          RawDataFile rfile = peakList.getRawDataFile(r);
          String rawSG =
              String.valueOf(project.getParameterValue(peakList.getSampleGroupsParameter(), rfile));

          // for each row: correlation of row to row in all raw files
          for (int i = 0; i < g.size(); i++) {
            PeakListRow trow = g.get(i);
            if (rowI != i) {
              // get correlation data (row to row)
              RowCorrelationData corrRows = corr.getCorrelationToRowI(i);
              // get correlation of feature-feature in selected raw file
              FeatureShapeCorrelationData fCorr = corrRows.getCorrPeakShape()[r];
              // regression
              if (fCorr != null && fCorr.getReg() != null && fCorr.getData() != null) {
                SimpleRegression reg = fCorr.getReg();
                // for summary of samples
                if (getCbSampleSummary().isSelected())
                  dataset.addValue(reg.getR(), rawSG, rfile.getName());
                else
                  dataset.addValue(reg.getR(), rawSG, rfile.getName() + "(" + trow.getID() + ")");
              }
            }
          }
        }
        // add plot
        title = "Row " + row.getID() + " corr";
        chart = ChartFactory.createBarChart(title, "Sample group", "Pearson correlation (r)",
            dataset, PlotOrientation.VERTICAL, false, true, false);
        BarRenderer catRen = (BarRenderer) chart.getCategoryPlot().getRenderer();
        catRen.setItemMargin(0.0);
        CategoryAxis axis = chart.getCategoryPlot().getDomainAxis();
        axis.setCategoryMargin(0.000);
        axis.setLowerMargin(0.01);
        axis.setUpperMargin(0.01);
        theme.apply(chart);
        cp = createChartPanel(chart);
        // formating
        intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
        yAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);
        //
        getPnPeakShapeCorrAllView().add(cp, BorderLayout.CENTER);
      }

      getPnPeakShapeCorr().validate();
    }
  }

  /**
   * plots the pseudo spectrum of the current group
   */
  private void plotPseudoSpectrum() {
    PKLRowGroup g = peakList.getLastViewedGroup();
    ChartPanel chart = PseudoSpectrum.createChart(g, g.getLastViewedRawFile(),
        getCbSumPseudoSpectrum().isSelected());
    ChartExportUtil.addExportMenu(chart);
    // theme.apply(chart.getChart());
    getPnPeakShapeCorrAllView().removeAll();
    getPnPeakShapeCorrAllView().add(chart, BorderLayout.CENTER);
    getPnPeakShapeCorrAllView().validate();
  }


  /**
   * adds a new export menu to this chart panel
   * 
   * @param chart
   * @return
   */
  private ChartPanel createChartPanel(JFreeChart chart) {
    ChartPanel cp = new ChartPanel(chart);
    ChartExportUtil.addExportMenu(cp);
    return cp;
  }

  /**
   * converts a regression to a XYSeries
   * 
   * @param reg
   * @param name
   * @return
   */
  private XYSeries regressionToSeries(FeatureShapeCorrelationData fCorr, String name) {
    // add all data points to series
    XYSeries series = new XYSeries(name, true, true);
    Double[] x = fCorr.getX();
    Double[] y = fCorr.getY();
    for (int i = 0; i < x.length; i++)
      series.add(x[i], y[i]);

    return series;
  }

  /**
   * 
   * @param fCorr
   * @param name
   * @return line for correlation
   */
  private XYLineAnnotation regressionToAnnotation(FeatureShapeCorrelationData fCorr) {
    SimpleRegression reg = fCorr.getReg();

    double minY = fCorr.getMinX() * reg.getSlope() + reg.getIntercept();
    double maxY = fCorr.getMaxX() * reg.getSlope() + reg.getIntercept();

    XYLineAnnotation line = new XYLineAnnotation(fCorr.getMinX(), minY, fCorr.getMaxX(), maxY);
    line.setToolTipText("r=" + reg.getR());

    return line;
  }

  /**
   * plot the height profile of this row and of all rows in a combined chart
   */
  private void plotIProfile() {
    // get group
    PKLRowGroup g = peakList.getLastViewedGroup();
    if (g != null) {
      PeakListRow row = g.getLastViewedRow();
      RawDataFile[] raw = row.getRawDataFiles();
      // one category for each sample group
      int sgCount = peakList.getSampleGroups().size();
      String sgName[] = new String[sgCount];
      // row IDs for series name
      int[] rowID = new int[g.size()];
      rowID[0] = row.getID();
      int c = 1;
      for (PeakListRow trow : g) {
        if (trow.getID() != row.getID()) {
          rowID[c] = trow.getID();
          c++;
        }
      }
      // one list for each row in the group and each sample group
      List[][] sg = new List[g.size()][sgCount];
      for (int i = 0; i < sg.length; i++)
        for (int k = 0; k < sg[0].length; k++)
          sg[i][k] = new ArrayList();

      c = 0;
      for (Object o : peakList.getSampleGroups().keySet()) {
        sgName[c] = (String) o;
        c++;
      }

      final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
      final DefaultBoxAndWhiskerCategoryDataset datasetAll =
          new DefaultBoxAndWhiskerCategoryDataset();

      // go through all raw files
      for (int r = 0; r < raw.length; r++) {
        // find group
        c = 0;
        for (Object o : peakList.getSampleGroups().keySet()) {
          Object rawSG = project.getParameterValue(peakList.getSampleGroupsParameter(), raw[r]);
          if (o.equals(rawSG)) {
            // height: add to lists
            Feature f1 = row.getPeak(raw[r]);
            double I1 = f1 != null ? f1.getHeight() : 0;
            sg[0][c].add(I1);
            // all other rows than the selected one
            int k = 1;
            for (PeakListRow trow : g) {
              if (trow.getID() != row.getID()) {
                Feature f2 = trow.getPeak(raw[r]);
                double I = f2 != null ? f2.getHeight() : 0;
                sg[k][c].add(I);
                k++;
              }
            }
          }
          c++;
        }
      }
      // k: rows
      // i: sample groups
      for (int i = 0; i < sg[0].length; i++)
        // data, series key, sample group
        if (getCbUseSampleGroups().isSelected())
          dataset.add(sg[0][i], "" + rowID[0], sgName[i]);
        else
          dataset.add(sg[0][i], sgName[i], "" + rowID[0]);

      for (int k = 0; k < sg.length; k++) {
        for (int i = 0; i < sg[k].length; i++) {
          if (getCbUseSampleGroups().isSelected())
            datasetAll.add(sg[k][i], "" + rowID[k], sgName[i]);
          else
            datasetAll.add(sg[k][i], sgName[i], "" + rowID[k]);
        }
      }

      // display
      ChartPanel chart = createCombinedBoxAndWhiskerPlot(dataset, datasetAll,
          colors[g.getLastViewedRowI() % colors.length], g.getLastViewedRowI());
      getPnIntensityCorr().removeAll();
      getPnIntensityCorr().add(chart, BorderLayout.CENTER);
      getPnIntensityCorr().validate();
    }
  }

  public JPanel getPnPeakShapeCorr() {
    return pnPeakShapeCorr;
  }

  public JPanel getPnIntensityCorr() {
    return pnIntensityCorr;
  }

  public JButton getBtnPreviousRow() {
    return btnPreviousRow;
  }

  /**
   * creates a double chart
   * 
   * @param dataset
   * @param datasetAll
   * @param rowAsSeries row or sample group as series?
   * @return
   */
  private ChartPanel createCombinedBoxAndWhiskerPlot(BoxAndWhiskerCategoryDataset dataset,
      BoxAndWhiskerCategoryDataset datasetAll, Paint colorRowI, int rowI) {
    CategoryAxis xAxis = new CategoryAxis("Sample set");
    xAxis.setLowerMargin(0.02);
    xAxis.setUpperMargin(0.02);
    xAxis.setCategoryMargin(0.15);
    NumberAxis yAxis = new NumberAxis("Height");
    // formating
    NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
    yAxis.setNumberFormatOverride(intensityFormat);
    //
    yAxis.setAutoRangeIncludesZero(true);
    BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
    renderer.setSeriesPaint(0, colorRowI);
    renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
    renderer.setFillBox(true);
    renderer.setMeanVisible(false);
    renderer.setDefaultSeriesVisibleInLegend(false);
    CategoryPlot plot = new CategoryPlot(dataset, xAxis, null, renderer);
    // renderer.setSeriesToolTipGenerator(1, new BoxAndWhiskerToolTipGenerator());

    CategoryAxis xAxis2 = new CategoryAxis("Sample set");
    xAxis2.setLowerMargin(0.01);
    xAxis2.setUpperMargin(0.01);
    xAxis2.setCategoryMargin(0.15);
    BoxAndWhiskerRenderer renderer2 = new BoxAndWhiskerRenderer();
    renderer2.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
    renderer2.setFillBox(true);
    renderer2.setMeanVisible(false);
    // set colors
    renderer2.setSeriesPaint(0, colorRowI);
    for (int i = 1; i < datasetAll.getRowCount(); i++) {
      int c = rowI < i ? i : i + 1;
      if (i <= rowI)
        c = i - 1;

      renderer2.setSeriesPaint(i, colors[c % colors.length]);
    }
    CategoryPlot plot2 = new CategoryPlot(datasetAll, xAxis2, null, renderer2);

    // parent plot...
    final CombinedRangeCategoryPlot mainPlot = new CombinedRangeCategoryPlot(yAxis);
    mainPlot.setGap(10.0);

    // add the subplots...
    mainPlot.add(plot, 1);
    mainPlot.add(plot2, 2);
    mainPlot.setOrientation(PlotOrientation.VERTICAL);

    JFreeChart chart =
        new JFreeChart("Height profile of row", new Font("Arial", Font.BOLD, 14), mainPlot, true);
    ChartPanel chartPanel = createChartPanel(chart);
    return chartPanel;
  }

  /**
   * used for plotting the intensity profile across sample groups
   * 
   * @param dataset
   * @return
   */
  private ChartPanel createBoxAndWhiskerPlot(BoxAndWhiskerCategoryDataset dataset) {

    CategoryAxis xAxis = new CategoryAxis("Sample set");
    NumberAxis yAxis = new NumberAxis("Height");
    yAxis.setAutoRangeIncludesZero(true);
    BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
    renderer.setFillBox(true);
    CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
    renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());

    JFreeChart chart =
        new JFreeChart("Height profile of row", new Font("Arial", Font.BOLD, 14), plot, false);
    ChartPanel chartPanel = createChartPanel(chart);
    return chartPanel;
  }

  public JPanel getPnPeakShapeCorrView() {
    return pnPeakShapeCorrView;
  }

  public JPanel getPnPeakShapeView() {
    return pnPeakShapeView;
  }

  public JPanel getPnPeakShapeCorrAllView() {
    return pnPeakShapeCorrAllView;
  }


  @Override
  public void componentResized(ComponentEvent e) {
    Dimension s = e.getComponent().getBounds().getSize();
    int insets = getInsets().left + getInsets().right;
    Dimension newSize = new Dimension((int) s.getWidth() - insets - 35, (int) s.getHeight() / 3);
    int w = (int) newSize.getWidth() / 3;
    int h = (int) newSize.getHeight();
    getPnPeakShapeCorrAllView().setPreferredSize(new Dimension(w, h));
    getPnPeakShapeCorrView().setPreferredSize(new Dimension(w, h));
    getPnPeakShapeView().setPreferredSize(new Dimension(w, h));
    //
    getPnIntensityCorr().setPreferredSize(newSize);
    getPnPeakShapeCorr().setPreferredSize(newSize);
    revalidate();
  }

  @Override
  public void componentHidden(ComponentEvent arg0) {}

  @Override
  public void componentMoved(ComponentEvent arg0) {}

  @Override
  public void componentShown(ComponentEvent arg0) {}

  public JSplitPane getSplit() {
    return split;
  }

  public JCheckBox getCbUseSampleGroups() {
    return cbUseSampleGroups;
  }

  public JCheckBox getCbSampleSummary() {
    return cbSampleSummary;
  }

  public JScrollPane getMainScroll() {
    return mainScroll;
  }

  public JCheckBox getCbAutoSkipGSize() {
    return cbAutoSkipGSize;
  }

  public JTextField getTxtMinGroupSize() {
    return txtMinGroupSize;
  }

  public JTextField getTxtJumpToRT() {
    return txtJumpToRT;
  }

  public JCheckBox getCbAutoRawFile() {
    return cbAutoRawFile;
  }

  public JTextField getTxtGroup() {
    return txtGroup;
  }

  public JTextField getTxtRow() {
    return txtRow;
  }

  public JCheckBox getCbShowPseudoSpectrum() {
    return cbShowPseudoSpectrum;
  }

  public JCheckBox getCbSumPseudoSpectrum() {
    return cbSumPseudoSpectrum;
  }
}
