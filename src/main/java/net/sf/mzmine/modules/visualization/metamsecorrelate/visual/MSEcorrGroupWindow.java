package net.sf.mzmine.modules.visualization.metamsecorrelate.visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.chartthemes.ChartThemeFactory;
import net.sf.mzmine.chartbasics.chartthemes.EStandardChartTheme;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.identities.iontype.AnnotationNetwork;
import net.sf.mzmine.datamodel.impl.PKLRowGroupList;
import net.sf.mzmine.datamodel.impl.RowGroup;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.CorrelationData.SimilarityMeasure;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.MS2SimilarityProviderGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.PKLRowGroup;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2GroupCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.R2RFullCorrelationData;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationNetworkLogic;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.annotationnetwork.visual.AnnotationNetworkPanel;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.corrnetwork.visual.CorrNetworkPanel;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.msmsnetwork.visual.MSMSSimilarityNetworkPanel;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.networks.rtnetwork.visual.RTNetworkPanel;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra.PseudoSpectrum;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.table.GroupedPeakListTable;
import net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.table.GroupedPeakListTableModel;
import net.sf.mzmine.modules.visualization.multimsms.MultiMSMSWindow;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;

public class MSEcorrGroupWindow extends JFrame {
  // Logger.
  private final Logger LOG = Logger.getLogger(getClass().getName());
  //
  private final Paint colors[];
  // sub window for more charts
  private MSEcorrGroupSubWindow subWindow;
  private MultiMSMSWindow msmsWindow;

  private EStandardChartTheme theme;

  // data
  private PeakList peakList;
  private PKLRowGroupList groups;
  private int currentGroupIndex;

  private MZmineProject project;
  // visual
  private JPanel contentPane;
  private GroupedPeakListTable tableGroupMembers;
  private JTextField txtGroup;
  private JTextField txtRow;
  private JButton btnPreviousRow;
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
  private JPanel panel_7;
  private JCheckBox cbSumPseudoSpectrum;
  private JSplitPane splitPane;
  private JSplitPane splitChart;
  private JSplitPane splitNetwork;
  private JSplitPane splitNetworkSub;
  private JPanel pnSpectrum;
  private AnnotationNetworkPanel pnAnnNetwork;
  private CorrNetworkPanel pnCorrNetwork;
  private RTNetworkPanel pnRTNetwork;
  private MSMSSimilarityNetworkPanel pnMSMSNetwork;
  private JButton btnSubWindow;
  private JButton btnMsmsWindow;
  private JLabel lblNet;
  private JTextField txtSkipSmallNetwork;
  private JPanel panel_8;
  private JComboBox<SimilarityMeasure> comboSimilarity;
  private JCheckBox cbMS2SimilarityNetwork;
  // user parameter that is used to group data in plots
  private UserParameter<?, ?> sampleGroupingUserParam;

  /**
   * Create the frame.
   * 
   * @param index
   * @param groups
   * @param peakList
   */
  public MSEcorrGroupWindow(MZmineProject project, final PeakList peakList, PKLRowGroupList groups,
      int index) {
    // sub window for more charts
    subWindow = new MSEcorrGroupSubWindow(this);
    msmsWindow = new MultiMSMSWindow();
    theme = ChartThemeFactory.getStandardTheme();
    theme.setPlotBackgroundPaint(new Color(235, 235, 235));
    theme.setShowSubtitles(true);
    // do not change colors of series
    theme.setApplyColors(false);

    // data
    this.peakList = peakList;
    this.groups = groups;
    this.currentGroupIndex = index;
    this.project = project;
    // theme
    colors = PKLRowGroup.colors;

    // peak list table parameters
    ParameterSet peakListTableParameters =
        MZmineCore.getConfiguration().getModuleParameters(PeakListTableModule.class);
    setBounds(100, 100, 1369, 667);
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    setContentPane(contentPane);

    splitPane = new JSplitPane();
    splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
    splitPane.setResizeWeight(0.4);
    contentPane.add(splitPane, BorderLayout.CENTER);

    splitChart = new JSplitPane();
    splitChart.setResizeWeight(0.5);
    splitPane.setLeftComponent(splitChart);

    pnSpectrum = new JPanel();
    pnSpectrum.setLayout(new BorderLayout(0, 0));
    splitChart.setRightComponent(pnSpectrum);


    splitNetwork = new JSplitPane();
    splitNetwork.setResizeWeight(0.666);
    splitChart.setLeftComponent(splitNetwork);

    pnAnnNetwork = new AnnotationNetworkPanel(true);
    splitNetwork.setRightComponent(pnAnnNetwork);

    // sub split for rt and corr net
    splitNetworkSub = new JSplitPane();
    splitNetworkSub.setResizeWeight(0.5);
    splitNetwork.setLeftComponent(splitNetworkSub);

    pnCorrNetwork = new CorrNetworkPanel(true);
    splitNetworkSub.setRightComponent(pnCorrNetwork);

    pnMSMSNetwork = new MSMSSimilarityNetworkPanel(true);
    pnMSMSNetwork.setMin(0.4, 3);
    splitNetworkSub.setLeftComponent(pnMSMSNetwork);

    pnRTNetwork = new RTNetworkPanel(true);
    pnRTNetwork.setTitle("Average retention time network");

    // scroll table
    mainScroll = new JScrollPane();
    splitPane.setRightComponent(mainScroll);

    JPanel pnTable = new JPanel();
    pnTable.setLayout(new BorderLayout(0, 0));
    mainScroll.setViewportView(pnTable);


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
    btnPreviousRow.addActionListener(e -> prevRow());
    panel_1.add(btnPreviousRow, "cell 0 1");

    txtRow = new JTextField();
    panel_1.add(txtRow, "cell 1 1");
    txtRow.setText("0");
    txtRow.setHorizontalAlignment(SwingConstants.RIGHT);
    txtRow.setColumns(4);

    JButton btnNextRow = new JButton("Next");
    btnNextRow.setToolTipText("UP key");
    btnNextRow.addActionListener(e -> nextRow());
    panel_1.add(btnNextRow, "cell 2 1");

    JButton btnNextGroup = new JButton("Next");
    btnNextGroup.setToolTipText("RIGHT key");
    btnNextGroup.addActionListener(e -> nextGroup());
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

    lblNet = new JLabel(" net< ");
    panel_4.add(lblNet);

    txtSkipSmallNetwork = new JTextField();
    txtSkipSmallNetwork.setToolTipText("Auto skip groups with size smaller than x.");
    txtSkipSmallNetwork.setText("2");
    txtSkipSmallNetwork.setColumns(2);
    panel_4.add(txtSkipSmallNetwork);

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
      @Override
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
      @Override
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

    cbSumPseudoSpectrum = new JCheckBox("Sum pseudo spectrum");
    cbSumPseudoSpectrum.setToolTipText("Take max height of each row");
    cbSumPseudoSpectrum.setSelected(true);
    panel_6.add(cbSumPseudoSpectrum);


    cbSampleSummary = new JCheckBox("Sample summary");
    panel_6.add(cbSampleSummary);
    cbSampleSummary.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        createCorrColumnsPlot();
      }
    });
    cbSampleSummary.setToolTipText("Average peak shape correlation column chart.");

    panel_7 = new JPanel();
    pnMenu.add(panel_7);
    panel_7.setLayout(new BoxLayout(panel_7, BoxLayout.Y_AXIS));

    btnSubWindow = new JButton("Sub window");
    btnSubWindow.addActionListener(e -> subWindow.setVisible(true));
    panel_7.add(btnSubWindow);

    btnMsmsWindow = new JButton("MSMS window");
    btnMsmsWindow.addActionListener(e -> msmsWindow.setVisible(true));
    panel_7.add(btnMsmsWindow);

    panel_8 = new JPanel();
    pnMenu.add(panel_8);
    panel_8.setLayout(new MigLayout("", "[][grow]", "[][]"));

    cbMS2SimilarityNetwork = new JCheckBox("MS/MS similarity network");
    cbMS2SimilarityNetwork.setSelected(true);
    cbMS2SimilarityNetwork.addItemListener(e -> setActiveFirstNetwork(
        cbMS2SimilarityNetwork.isSelected() ? pnMSMSNetwork : pnRTNetwork));
    panel_8.add(cbMS2SimilarityNetwork, "cell 0 1");

    comboSimilarity = new JComboBox<SimilarityMeasure>(SimilarityMeasure.values());
    comboSimilarity.setSelectedItem(SimilarityMeasure.COSINE_SIM);
    panel_8.add(comboSimilarity, "cell 0 0,growx");
    comboSimilarity.addItemListener(e -> {
      GroupedPeakListTableModel model = getTableModel();
      model.setSimilarityMeasure((SimilarityMeasure) comboSimilarity.getSelectedItem());
      model.fireTableDataChanged();
    });

    btnPreviousGroup.addActionListener(e -> prevGroup());

    JScrollPane scrollPane = new JScrollPane();
    pnTable.add(scrollPane, BorderLayout.CENTER);

    tableGroupMembers = new GroupedPeakListTable(this, peakListTableParameters, peakList);
    scrollPane.setViewportView(tableGroupMembers);

    // close also sub window
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    this.addWindowListener(new java.awt.event.WindowAdapter() {
      @Override
      public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        subWindow.setVisible(false);
        msmsWindow.setVisible(false);
      }
    });

    // key bindings
    setVisible(true);
    subWindow.setVisible(true);
    addKeyBindings();
    setCurrentGroupView(index);
  }

  /**
   * Change from RT network to
   * 
   * @param pn
   */
  private void setActiveFirstNetwork(JPanel pn) {
    splitNetworkSub.setLeftComponent(pn);
  }

  private GroupedPeakListTableModel getTableModel() {
    return (GroupedPeakListTableModel) tableGroupMembers.getModel();
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
        prevRow();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[1]), commands[1]);
    pn.getActionMap().put(commands[1], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        nextRow();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[2]), commands[2]);
    pn.getActionMap().put(commands[2], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        prevGroup();
      }
    });
    pn.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        .put(KeyStroke.getKeyStroke(commands[3]), commands[3]);
    pn.getActionMap().put(commands[3], new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent ae) {
        nextGroup();
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
        nextRaw();
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
        prevRaw();
      }
    });
  }

  public void nextRaw() {
    RowGroup g = getTableModel().getGroup();
    if (g.getLastViewedRawFileI() + 1 < peakList.getRawDataFiles().length)
      setCurrentRawView(g.getLastViewedRawFileI() + 1);
  }

  public void prevRaw() {
    RowGroup g = getTableModel().getGroup();
    if (g.getLastViewedRawFileI() > 0)
      setCurrentRawView(g.getLastViewedRawFileI() - 1);
  }

  public void prevGroup() {
    RowGroup g = getTableModel().getGroup();
    if (currentGroupIndex > 0) {
      setCurrentGroupView(currentGroupIndex - 1);
    }
  }

  public void nextGroup() {
    RowGroup g = getTableModel().getGroup();
    if (currentGroupIndex < groups.size() - 1) {
      setCurrentGroupView(currentGroupIndex + 1);
    }
  }

  public void prevRow() {
    RowGroup g = getTableModel().getGroup();
    if (g.getLastViewedRowI() > 0)
      setCurrentRowView(g.getLastViewedRowI() - 1);
  }

  public void nextRow() {
    RowGroup g = getTableModel().getGroup();
    if (g.getLastViewedRowI() + 1 < g.size())
      setCurrentRowView(g.getLastViewedRowI() + 1);
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
        int lastIndex = currentGroupIndex;
        int min = Integer.valueOf(getTxtMinGroupSize().getText());
        int minNet = Integer.valueOf(getTxtSkipSmallNetwork().getText());
        while (!(groups.get(index).size() >= min
            && checkNetSize(MSAnnotationNetworkLogic.getBestNetwork(groups.get(index)), minNet))
            && index + 1 < groups.size() && index - 1 >= 0)
          index += index < lastIndex ? -1 : +1;
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, "While excluding groups", ex);
      }
    }
    // set index to peaklist
    currentGroupIndex = index;
    RowGroup g = groups.get(index);
    getTableModel().setGroup(g);

    // set text
    getTxtGroup().setText(String.valueOf(index));
    getTxtRow().setText(String.valueOf(g.getLastViewedRowI()));

    // auto show raw file containing lastViewed row
    checkAutoShowRawFile();
    // show the group in the table
    GroupedPeakListTableModel model = (GroupedPeakListTableModel) tableGroupMembers.getModel();
    model.fireTableDataChanged();
    //
    renewAllPlots(true, true, true, true);
  }

  private boolean checkNetSize(AnnotationNetwork bestNetwork, int minNet) {
    return bestNetwork != null && bestNetwork.size() >= minNet;
  }

  /**
   * check for auto show raw file that contains the lastViewed row of the last viewed group
   */
  private boolean checkAutoShowRawFile() {
    if (getCbAutoRawFile().isSelected()) {
      RowGroup g = getTableModel().getGroup();
      PeakListRow row = g.getLastViewedRow();

      int maxI = 0;
      double maxHeight = Double.NEGATIVE_INFINITY;
      for (int i = 0; i < g.getRaw().length; i++) {
        RawDataFile raw = g.getRaw()[i];
        if (row.getPeak(raw) != null) {
          double h = row.getPeak(raw).getHeight();
          if (h > maxHeight) {
            maxI = i;
            maxHeight = h;
          }
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
    RowGroup g = getTableModel().getGroup();
    g.setLastViewedRawFileI(i);
    renewAllPlots(true, true, false, false);
  }

  /**
   * sets the row of interest and renews all plots
   * 
   * @param i
   */
  protected void setCurrentRowView(int i) {
    getTxtRow().setText(String.valueOf(i));

    RowGroup g = getTableModel().getGroup();
    g.setLastViewedRowI(i);
    // update table
    getTableModel().fireTableDataChanged();

    // set selected in networks
    PeakListRow row = g.get(i);
    pnAnnNetwork.setSelectedRow(row);
    pnCorrNetwork.setSelectedRow(row);
    pnMSMSNetwork.setSelectedRow(row);
    pnRTNetwork.setSelectedRow(row);
    // auto show raw file containing lastViewed row
    boolean renewRaw = checkAutoShowRawFile();
    renewAllPlots(renewRaw, true, true, false);
  }

  /**
   * renews all plots: IProfile, peak shapes, peak shape correlation
   */
  public void renewAllPlots(boolean peakShapes, boolean peakShapeCorr, boolean iProfile,
      boolean networkSpectrum) {
    // plot peak shapes
    if (peakShapes)
      plotPeakShapes();
    // show correlation of f1 to all other features
    if (peakShapeCorr) {
      // correlation of total and single raw file
      plotPeakShapeCorrelation(false);
      plotPeakShapeCorrelation(true);
      // intensity profile correlation
      plotIMaxCorrelation();

      // all f2f correlations in columns
      createCorrColumnsPlot();
    }
    if (networkSpectrum) {
      // plot a pseudo spectrum
      plotPseudoSpectrum();
      // network of annotations
      createAnnotationNetwork();
      // create correlation network
      createCorrelationNetwork();
      // create RT network
      createRTNetwork();
      // create msms network
      createMSMSNetwork();

      // MSMS window
      if (msmsWindow.isVisible()) {
        RowGroup g = getTableModel().getGroup();
        msmsWindow.setData(g.toArray(new PeakListRow[0]), peakList.getRawDataFiles(), null, false,
            SortingProperty.MZ, SortingDirection.Ascending);
      }
    }
    // plotIProfile
    if (iProfile)
      plotIProfile();
  }

  /**
   * plots the peak shapes of each row of one selected raw file
   */
  private void plotPeakShapes() {
    ChartPanel cp = null;
    // get group
    RowGroup g = getTableModel().getGroup();
    if (g != null) {
      // PeakListRow row = g.getLastViewedRow();
      RawDataFile raw = g.getLastViewedRawFile();

      // data set
      XYSeriesCollection data = new XYSeriesCollection();
      // add plot
      String sg = "";
      if (getSampleGroupsParameter() != null)
        sg = String.valueOf(project.getParameterValue(getSampleGroupsParameter(), raw));

      String title = sg + "(" + raw.getName() + ")";
      JFreeChart chart = ChartFactory.createXYLineChart(title, "retention time | min", "Intensity",
          data, PlotOrientation.VERTICAL, true, true, false);
      XYItemRenderer renderer = chart.getXYPlot().getRenderer();
      setRendererKeepColors(renderer);

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
      cp = createChartPanel(chart);
    }
    // show
    subWindow.setShapePlot(cp);
  }

  private UserParameter<?, ?> getSampleGroupsParameter() {
    return sampleGroupingUserParam;
  }

  /**
   * peak shape correlation of selected row to all other rows in selected raw file
   */
  private void plotPeakShapeCorrelation(boolean totalCorrelation) {
    // result
    EChartPanel cp = null;
    try {
      // get group
      RowGroup cg = getTableModel().getGroup();
      if (cg instanceof PKLRowGroup) {
        PKLRowGroup g = (PKLRowGroup) cg;
        //
        PeakListRow row = g.getLastViewedRow();
        int rowI = g.getLastViewedRowI();
        R2GroupCorrelationData corr = g.getCorr(rowI);
        int rawI = g.getLastViewedRawFileI();
        RawDataFile raw = g.getLastViewedRawFile();

        // first chart: correlation of row to all other rows in rawI
        // data set
        XYSeriesCollection data = new XYSeriesCollection();
        // titles
        String sg = "";
        if (getSampleGroupsParameter() != null)
          sg = String.valueOf(project.getParameterValue(getSampleGroupsParameter(), raw));

        String title = totalCorrelation ? "Total correlation of row " + row.getID()
            : MessageFormat.format("Row {0} corr in: {1} ({2})", row.getID(), sg, raw.getName());
        // create chart
        JFreeChart chart =
            ChartFactory.createScatterPlot(title, "Intensity (row: " + row.getID() + ")",
                "Intensity (other rows)", data, PlotOrientation.VERTICAL, true, true, false);
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        setRendererKeepColors(renderer);
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
            R2RFullCorrelationData corrRows = corr.getCorrelationToRowI(trow.getID());
            if (corrRows != null) {
              // get correlation of feature-feature in selected raw file
              CorrelationData fCorr = null;
              if (totalCorrelation)
                fCorr = corrRows.getTotalCorr();
              else
                fCorr = corrRows.getCorrPeakShape(raw);
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
        }

        // add chart to panel
        chart.setNotify(true);
        chart.fireChartChanged();
        cp = createChartPanel(chart);
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
    }
    // show
    if (totalCorrelation)
      subWindow.setTotalShapeCorrPlot(cp);
    else
      subWindow.setShapeCorrPlot(cp);
  }


  /**
   * Changes the renderer to not change color at theme.apply
   * 
   * @param renderer
   */
  private void setRendererKeepColors(XYItemRenderer renderer) {
    if (renderer instanceof AbstractRenderer) {
      setRendererKeepColors(((AbstractRenderer) renderer));
    }
  }

  /**
   * Changes the renderer to not change color at theme.apply
   * 
   * @param renderer
   */
  private void setRendererKeepColors(AbstractRenderer renderer) {
    renderer.setAutoPopulateSeriesPaint(false);
    renderer.setAutoPopulateSeriesFillPaint(false);
    renderer.setAutoPopulateSeriesOutlinePaint(false);
  }

  /**
   * Correlation of the maximum intensitites. One series per f2f correlation over all raw data
   * files.
   */
  private void plotIMaxCorrelation() {
    EChartPanel cp = null;
    try {
      // get group
      RowGroup cg = getTableModel().getGroup();
      if (cg instanceof PKLRowGroup) {
        PKLRowGroup g = (PKLRowGroup) cg;
        //
        PeakListRow row = g.getLastViewedRow();
        int rowI = g.getLastViewedRowI();
        R2GroupCorrelationData corr = g.getCorr(rowI);
        int rawI = g.getLastViewedRawFileI();
        RawDataFile raw = g.getLastViewedRawFile();

        // first chart: correlation of row to all other rows in rawI
        // data set
        XYSeriesCollection data = new XYSeriesCollection();
        // title
        String sg = getSampleGroupsParameter() == null ? ""
            : String.valueOf(project.getParameterValue(getSampleGroupsParameter(), raw));
        String title = MessageFormat.format("Row {0} Imax corr across all samples", row.getID());
        // create chart
        JFreeChart chart =
            ChartFactory.createScatterPlot(title, "Intensity (row: " + row.getID() + ")",
                "Intensity (other rows)", data, PlotOrientation.VERTICAL, true, true, false);
        XYItemRenderer renderer = chart.getXYPlot().getRenderer();
        setRendererKeepColors(renderer);
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
            R2RFullCorrelationData corrRows = corr.getCorrelationToRowI(trow.getID());
            if (corrRows != null) {
              // get correlation of feature-feature in selected raw file
              CorrelationData fCorr = null;
              fCorr = corrRows.getHeightCorr();
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
        }

        // add chart to panel
        chart.setNotify(true);
        chart.fireChartChanged();
        cp = createChartPanel(chart);
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
    }
    // show
    subWindow.setMaxICorrPlot(cp);
  }

  /**
   * All single feature to feature correlations of one row to all other in a plot in raw
   */
  private void createCorrColumnsPlot() {
    ChartPanel cp = null;
    try {
      // get group
      RowGroup cg = getTableModel().getGroup();
      if (cg instanceof PKLRowGroup) {
        PKLRowGroup g = (PKLRowGroup) cg;
        //
        PeakListRow row = g.getLastViewedRow();
        int rowI = g.getLastViewedRowI();
        R2GroupCorrelationData corr = g.getCorr(rowI);

        // spectrum or column chart of all correlations
        // add plot of all correlations (this row to all other rows in all raw files)
        // data set
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        // go through all raw files

        // for each row: correlation of row to row in all raw files
        for (int i = 0; i < g.size(); i++) {
          PeakListRow trow = g.get(i);
          if (rowI != i) {
            // get correlation data (row to row)
            R2RFullCorrelationData corrRows = corr.getCorrelationToRowI(trow.getID());
            if (corrRows != null) {
              // for all raw data files
              for (int r = 0; r < peakList.getRawDataFiles().length; r++) {
                RawDataFile raw = peakList.getRawDataFile(r);
                String rawSG = getSampleGroupsParameter() == null ? ""
                    : String.valueOf(project.getParameterValue(getSampleGroupsParameter(), raw));

                // get correlation of feature-feature in selected raw file
                CorrelationData fCorr = corrRows.getCorrPeakShape(raw);
                // regression
                if (fCorr != null && fCorr.getReg() != null && fCorr.getData() != null) {
                  SimpleRegression reg = fCorr.getReg();
                  // for summary of samples
                  if (getCbSampleSummary().isSelected())
                    dataset.addValue(reg.getR(), rawSG, raw.getName());
                  else
                    dataset.addValue(reg.getR(), rawSG, raw.getName() + "(" + trow.getID() + ")");
                }
              }
            }
          }
        }
        // add plot
        String title = "Row " + row.getID() + " f2f corr";
        JFreeChart chart = ChartFactory.createBarChart(title, "Sample group",
            "Pearson correlation (r)", dataset, PlotOrientation.VERTICAL, false, true, false);
        BarRenderer catRen = (BarRenderer) chart.getCategoryPlot().getRenderer();
        setRendererKeepColors(catRen);
        catRen.setItemMargin(0.0);
        CategoryAxis axis = chart.getCategoryPlot().getDomainAxis();
        axis.setCategoryMargin(0.000);
        axis.setLowerMargin(0.01);
        axis.setUpperMargin(0.01);

        chart.getCategoryPlot().getRangeAxis().setRange(0.85, 1.0);

        cp = createChartPanel(chart);

        // formating
        NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
        NumberAxis yAxis = (NumberAxis) chart.getCategoryPlot().getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);
        //
      }
    } catch (Exception e) {
      LOG.log(Level.SEVERE, e.getMessage(), e);
    }
    // show
    subWindow.setCorrColumnsChart(cp);
  }

  /**
   * plots the pseudo spectrum of the current group
   */
  private void plotPseudoSpectrum() {
    pnSpectrum.removeAll();
    RowGroup g = getTableModel().getGroup();
    if (g != null) {
      EChartPanel chart = PseudoSpectrum.createChartPanel(g, g.getLastViewedRawFile(),
          getCbSumPseudoSpectrum().isSelected());
      // theme.apply(chart.getChart());
      pnSpectrum.add(chart, BorderLayout.CENTER);
    }
    pnSpectrum.revalidate();
    pnSpectrum.repaint();
  }

  /**
   * Annotation network
   */
  private void createAnnotationNetwork() {
    RowGroup g = getTableModel().getGroup();
    if (g != null) {
      PeakListRow[] rows = g.toArray(new PeakListRow[g.size()]);
      pnAnnNetwork.setPeakListRows(rows);
      pnAnnNetwork.resetZoom();
      pnAnnNetwork.revalidate();
      pnAnnNetwork.repaint();
    } else {
      pnAnnNetwork.setPeakListRows(null);
    }
  }

  /**
   * RT network
   */
  private void createRTNetwork() {
    RowGroup g = getTableModel().getGroup();
    if (g != null) {
      PeakListRow[] rows = g.toArray(new PeakListRow[g.size()]);
      pnRTNetwork.setPeakListRows(rows);
      pnRTNetwork.resetZoom();
      pnRTNetwork.revalidate();
      pnRTNetwork.repaint();
    } else {
      pnRTNetwork.setPeakListRows(null);
    }
  }

  /**
   * Correlation network
   */
  private void createCorrelationNetwork() {
    RowGroup g = getTableModel().getGroup();
    if (g instanceof PKLRowGroup) {
      PeakListRow[] rows = g.toArray(new PeakListRow[g.size()]);
      pnCorrNetwork.setPeakListRows(rows);
      pnCorrNetwork.resetZoom();
      pnCorrNetwork.revalidate();
      pnCorrNetwork.repaint();
    } else {
      pnAnnNetwork.setPeakListRows(null);
    }
  }

  /**
   * MSMS network
   */
  private void createMSMSNetwork() {
    RowGroup g = getTableModel().getGroup();
    if (g instanceof MS2SimilarityProviderGroup) {
      pnMSMSNetwork.setMap(((MS2SimilarityProviderGroup) g).getMS2SimilarityMap());
      pnMSMSNetwork.resetZoom();
      pnMSMSNetwork.revalidate();
      pnMSMSNetwork.repaint();
    } else {
      pnAnnNetwork.setPeakListRows(null);
    }
  }

  /**
   * adds a new export menu to this chart panel
   * 
   * @param chart
   * @return
   */
  private EChartPanel createChartPanel(JFreeChart chart) {
    EChartPanel cp = new EChartPanel(chart);
    try {
      theme.apply(chart);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Cannot apply theme to chart " + chart.getID(), e);
    }
    return cp;
  }

  /**
   * converts a regression to a XYSeries
   * 
   * @param reg
   * @param name
   * @return
   */
  private XYSeries regressionToSeries(CorrelationData fCorr, String name) {
    // add all data points to series
    XYSeries series = new XYSeries(name, true, true);
    double[][] dat = fCorr.getData();
    for (double[] d : dat)
      series.add(d[0], d[1]);

    return series;
  }

  /**
   * 
   * @param fCorr
   * @param name
   * @return line for correlation
   */
  private XYLineAnnotation regressionToAnnotation(CorrelationData fCorr) {
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
    RowGroup g = getTableModel().getGroup();
    if (g != null) {
      PeakListRow row = g.getLastViewedRow();
      RawDataFile[] raw = row.getRawDataFiles();
      // one category for each sample group
      boolean noGroups = true;
      String sgName[] = null;
      int sgCount = 0;
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
      List[][] sg = new List[g.size()][noGroups ? 1 : sgCount];
      for (int i = 0; i < sg.length; i++)
        for (int k = 0; k < sg[0].length; k++)
          sg[i][k] = new ArrayList<>();


      final DefaultBoxAndWhiskerCategoryDataset dataset = new DefaultBoxAndWhiskerCategoryDataset();
      final DefaultBoxAndWhiskerCategoryDataset datasetAll =
          new DefaultBoxAndWhiskerCategoryDataset();

      // go through all raw files
      for (int r = 0; r < raw.length; r++) {
        // find sample group
        c = 0;
        // add
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
      // k: rows
      // i: sample groups
      for (int i = 0; i < sg[0].length; i++)
        // data, series key, sample group
        if (!noGroups && getCbUseSampleGroups().isSelected())
          dataset.add(sg[0][i], "" + rowID[0], sgName[i]);
        else
          dataset.add(sg[0][i], "data", "" + rowID[0]);

      for (int k = 0; k < sg.length; k++) {
        for (int i = 0; i < sg[k].length; i++) {
          if (!noGroups && getCbUseSampleGroups().isSelected())
            datasetAll.add(sg[k][i], "" + rowID[k], sgName[i]);
          else
            datasetAll.add(sg[k][i], "data", "" + rowID[k]);
        }
      }

      // display
      ChartPanel chart = createCombinedBoxAndWhiskerPlot(dataset, datasetAll,
          colors[g.getLastViewedRowI() % colors.length], g.getLastViewedRowI());
      // add to sub window
      subWindow.setBoxPlot(chart);
    }
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
    setRendererKeepColors(renderer);
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
    setRendererKeepColors(renderer);
    renderer.setFillBox(true);
    CategoryPlot plot = new CategoryPlot(dataset, xAxis, yAxis, renderer);
    renderer.setDefaultToolTipGenerator(new BoxAndWhiskerToolTipGenerator());

    JFreeChart chart =
        new JFreeChart("Height profile of row", new Font("Arial", Font.BOLD, 14), plot, false);
    ChartPanel chartPanel = createChartPanel(chart);
    return chartPanel;
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

  public JCheckBox getCbSumPseudoSpectrum() {
    return cbSumPseudoSpectrum;
  }

  public JTextField getTxtSkipSmallNetwork() {
    return txtSkipSmallNetwork;
  }

  public JComboBox getComboSimilarity() {
    return comboSimilarity;
  }
}
