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

package net.sf.mzmine.modules.visualization.spectra.spectralmatchresults;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.text.DecimalFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.Range;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.smiles.SmilesParser;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.chartbasics.gui.swing.EChartPanel;
import net.sf.mzmine.chartbasics.gui.wrapper.ChartViewWrapper;
import net.sf.mzmine.chartbasics.listener.AxisRangeChangedListener;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.molstructure.Structure2DComponent;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.util.ColorScaleUtil;
import net.sf.mzmine.util.components.MultiLineLabel;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBEntry;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;
import net.sf.mzmine.util.swing.IconUtil;
import net.sf.mzmine.util.swing.SwingExportUtil;

public class SpectralMatchPanel extends JPanel {
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private static final int ICON_WIDTH = 50;
  static final ImageIcon iconAll = new ImageIcon("icons/exp_graph_all.png");
  static final ImageIcon iconPdf = new ImageIcon("icons/exp_graph_pdf.png");
  static final ImageIcon iconEps = new ImageIcon("icons/exp_graph_eps.png");
  static final ImageIcon iconEmf = new ImageIcon("icons/exp_graph_emf.png");
  static final ImageIcon iconSvg = new ImageIcon("icons/exp_graph_svg.png");

  public static final Font FONT = new Font("Verdana", Font.PLAIN, 24);

  private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");
  private static final long serialVersionUID = 1L;
  public static final int META_WIDTH = 500;
  public static final int ENTRY_HEIGHT = 500;

  // colors
  public static final double MIN_COS_COLOR_VALUE = 0.5;
  public static final double MAX_COS_COLOR_VALUE = 1.0;
  // min color is a darker red
  // max color is a darker green
  public static final Color MAX_COS_COLOR = new Color(0x388E3C);
  public static final Color MIN_COS_COLOR = new Color(0xE30B0B);

  private Font headerFont = new Font("Dialog", Font.BOLD, 16);
  private Font titleFont = new Font("Dialog", Font.BOLD, 18);
  private Font scoreFont = new Font("Dialog", Font.BOLD, 30);

  private EChartPanel mirrorChart;

  private boolean setCoupleZoomY;

  private XYPlot queryPlot;

  private XYPlot libraryPlot;
  private Font chartFont;
  private JPanel pnExport;

  public SpectralMatchPanel(SpectralDBPeakIdentity hit) {
    JPanel panel = this;
    panel.setLayout(new BorderLayout());

    JPanel spectrumPanel = new JPanel(new BorderLayout());

    // set meta data from identity
    JPanel metaDataPanel = new JPanel();
    metaDataPanel.setLayout(new BoxLayout(metaDataPanel, BoxLayout.Y_AXIS));

    metaDataPanel.setBackground(Color.WHITE);

    // add title
    MigLayout l = new MigLayout("aligny center, wrap, insets 0 10 0 30", "[grow][]", "[grow]");
    JPanel boxTitlePanel = new JPanel();
    boxTitlePanel.setLayout(l);


    double simScore = hit.getSimilarity().getScore();
    Color gradientCol = ColorScaleUtil.getColor(MIN_COS_COLOR, MAX_COS_COLOR, MIN_COS_COLOR_VALUE,
        MAX_COS_COLOR_VALUE, simScore);
    boxTitlePanel.setBackground(gradientCol);
    Box boxTitle = Box.createHorizontalBox();
    // boxTitle.add(Box.createHorizontalGlue());

    JPanel panelTitle = new JPanel(new GridLayout(1, 1));
    panelTitle.setBackground(gradientCol);
    String name = hit.getEntry().getField(DBEntryField.NAME).orElse("N/A").toString();
    CustomTextPane title = new CustomTextPane(true);
    title.setEditable(false);
    title.setFont(headerFont);
    title.setText(name);
    title.setBackground(gradientCol);
    title.setForeground(Color.WHITE);
    panelTitle.add(title);

    // score result
    JPanel panelScore = new JPanel();
    panelScore.setLayout(new BoxLayout(panelScore, BoxLayout.Y_AXIS));
    JLabel score = new JLabel(COS_FORM.format(simScore));
    score.setToolTipText("Cosine similarity of raw data scan (top, blue) and database scan: "
        + COS_FORM.format(simScore));
    score.setFont(scoreFont);
    score.setForeground(Color.WHITE);
    panelScore.setBackground(gradientCol);
    panelScore.add(score);
    boxTitlePanel.add(panelScore, "cell 1 0");
    boxTitlePanel.add(panelTitle, "cell 0 0, growx, center");
    boxTitle.add(boxTitlePanel);

    // structure preview
    IAtomContainer molecule;
    JPanel preview2DPanel = new JPanel(new BorderLayout());
    preview2DPanel.setPreferredSize(new Dimension(META_WIDTH, 150));
    preview2DPanel.setMinimumSize(new Dimension(META_WIDTH, 150));
    preview2DPanel.setMaximumSize(new Dimension(META_WIDTH, 150));

    JPanel pn = new JPanel(new BorderLayout());
    pn.setBackground(Color.WHITE);
    pnExport = new JPanel(new MigLayout("gapy 0 ", "[]", "[][][][][]"));
    pnExport.setBackground(Color.WHITE);
    preview2DPanel.add(pn, BorderLayout.EAST);
    pn.add(pnExport, BorderLayout.CENTER);

    addExportButtons(MZmineCore.getConfiguration()
        .getModuleParameters(SpectraIdentificationResultsModule.class));

    JComponent newComponent = null;

    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A").toString();
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A").toString();

    // check for INCHI
    if (inchiString != "N/A") {
      molecule = parseInChi(hit);
    }
    // check for smiles
    else if (smilesString != "N/A") {
      molecule = parseSmiles(hit);
    } else
      molecule = null;

    // try to draw the component
    if (molecule != null) {
      try {
        newComponent = new Structure2DComponent(molecule, FONT);
      } catch (Exception e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        newComponent = new MultiLineLabel(errorMessage);
      }
      preview2DPanel.add(newComponent, BorderLayout.CENTER);
      preview2DPanel.revalidate();

      metaDataPanel.add(preview2DPanel);
    }

    // information on compound
    JPanel panelCompounds =
        extractMetaData("Compound information", hit.getEntry(), DBEntryField.COMPOUND_FIELDS);

    // instrument info
    JPanel panelInstrument =
        extractMetaData("Instrument information", hit.getEntry(), DBEntryField.INSTRUMENT_FIELDS);

    JPanel g1 = new JPanel(new GridLayout(1, 2, 4, 0));
    g1.setBackground(Color.WHITE);
    g1.add(panelCompounds);
    g1.add(panelInstrument);
    metaDataPanel.add(g1);

    // database links
    JPanel panelDB =
        extractMetaData("Database links", hit.getEntry(), DBEntryField.DATABASE_FIELDS);

    // // Other info
    JPanel panelOther =
        extractMetaData("Other information", hit.getEntry(), DBEntryField.OTHER_FIELDS);

    JPanel g2 = new JPanel(new GridLayout(1, 2, 4, 0));
    g2.setBackground(Color.WHITE);
    g2.add(panelDB);
    g2.add(panelOther);
    metaDataPanel.add(g2);

    // get mirror spectra window
    MirrorScanWindow mirrorWindow = new MirrorScanWindow();
    mirrorWindow.setScans(hit);

    // fixed width panel
    ScrollablePanel scrollpn = new ScrollablePanel(new BorderLayout());
    scrollpn.setScrollableWidth(ScrollablePanel.ScrollableSizeHint.FIT);
    scrollpn.setScrollableHeight(ScrollablePanel.ScrollableSizeHint.STRETCH);
    scrollpn.add(metaDataPanel);


    JScrollPane metaDataPanelScrollPane =
        new JScrollPane(scrollpn, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    mirrorChart = mirrorWindow.getMirrorSpecrumPlot();
    // use no buffer for later pdf export
    mirrorChart = new EChartPanel(mirrorChart.getChart(), false);
    spectrumPanel.add(mirrorChart);

    coupleZoomYListener();

    metaDataPanelScrollPane.setPreferredSize(new Dimension(META_WIDTH + 20, ENTRY_HEIGHT));
    panel.setPreferredSize(new Dimension(0, ENTRY_HEIGHT));

    panel.add(boxTitle, BorderLayout.NORTH);
    panel.add(spectrumPanel, BorderLayout.CENTER);
    panel.add(metaDataPanelScrollPane, BorderLayout.EAST);
    panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

    metaDataPanelScrollPane.revalidate();
    scrollpn.revalidate();
    panel.revalidate();
  }


  /**
   * 
   * @param param {@link SpectraIdentificationResultsParameters}
   */
  private void addExportButtons(ParameterSet param) {
    JButton btnExport = null;

    if (param.getParameter(SpectraIdentificationResultsParameters.all).getValue()) {
      btnExport = new JButton(IconUtil.scaled(iconAll, ICON_WIDTH));
      btnExport.setMaximumSize(new Dimension(btnExport.getIcon().getIconWidth() + 6,
          btnExport.getIcon().getIconHeight() + 6));
      btnExport.addActionListener(e -> exportToGraphics("all"));
      pnExport.add(btnExport, "cell 0 0, growx, center");
    }

    if (param.getParameter(SpectraIdentificationResultsParameters.pdf).getValue()) {
      btnExport = new JButton(IconUtil.scaled(iconPdf, ICON_WIDTH));
      btnExport.setMaximumSize(new Dimension(btnExport.getIcon().getIconWidth() + 6,
          btnExport.getIcon().getIconHeight() + 6));
      btnExport.addActionListener(e -> exportToGraphics("pdf"));
      pnExport.add(btnExport, "cell 0 1, growx, center");
    }

    if (param.getParameter(SpectraIdentificationResultsParameters.emf).getValue()) {
      btnExport = new JButton(IconUtil.scaled(iconEmf, ICON_WIDTH));
      btnExport.setMaximumSize(new Dimension(btnExport.getIcon().getIconWidth() + 6,
          btnExport.getIcon().getIconHeight() + 6));
      btnExport.addActionListener(e -> exportToGraphics("emf"));
      pnExport.add(btnExport, "cell 0 2, growx, center");
    }

    if (param.getParameter(SpectraIdentificationResultsParameters.eps).getValue()) {
      btnExport = new JButton(IconUtil.scaled(iconEps, ICON_WIDTH));
      btnExport.setMaximumSize(new Dimension(btnExport.getIcon().getIconWidth() + 6,
          btnExport.getIcon().getIconHeight() + 6));
      btnExport.addActionListener(e -> exportToGraphics("eps"));
      pnExport.add(btnExport, "cell 0 3, growx, center");
    }

    if (param.getParameter(SpectraIdentificationResultsParameters.svg).getValue()) {
      btnExport = new JButton(IconUtil.scaled(iconSvg, ICON_WIDTH));
      btnExport.setMaximumSize(new Dimension(btnExport.getIcon().getIconWidth() + 6,
          btnExport.getIcon().getIconHeight() + 6));
      btnExport.addActionListener(e -> exportToGraphics("svg"));
      pnExport.add(btnExport, "cell 0 4, growx, center");
    }
  }


  public void exportToAllGraphics() {
    exportToGraphics("all");
  }

  /**
   * Export the whole panel to pdf, emf, eps or all
   * 
   * @param format
   */
  public void exportToGraphics(String format) {
    // old path
    FileNameParameter param =
        MZmineCore.getConfiguration().getModuleParameters(SpectraIdentificationResultsModule.class)
            .getParameter(SpectraIdentificationResultsParameters.file);
    final JFileChooser chooser;
    if (param.getValue() != null) {
      chooser = new JFileChooser();
      chooser.setSelectedFile(param.getValue());
    } else
      chooser = new JFileChooser();
    // get file
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      try {
        File f = chooser.getSelectedFile();
        pnExport.setVisible(false);
        pnExport.revalidate();
        pnExport.getParent().revalidate();
        pnExport.getParent().repaint();

        SwingExportUtil.writeToGraphics(this, f.getParentFile(), f.getName(), format);
        // save path
        param.setValue(FileAndPathUtil.eraseFormat(f));
      } catch (Exception ex) {
        logger.log(Level.WARNING, "Cannot export graphics of spectra match panel", ex);
      } finally {
        pnExport.setVisible(true);
        pnExport.getParent().revalidate();
        pnExport.getParent().repaint();
      }
    }
  }

  private void coupleZoomYListener() {
    CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) mirrorChart.getChart().getXYPlot();
    NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
    axis.setLabel("m/z");
    queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
    libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);
    queryPlot.getRangeAxis().addChangeListener(new AxisRangeChangedListener(null) {
      @Override
      public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
          Range newR) {
        rangeHasChanged(newR);
      }
    });
    libraryPlot.getRangeAxis().addChangeListener(new AxisRangeChangedListener(null) {
      @Override
      public void axisRangeChanged(ChartViewWrapper chart, ValueAxis axis, Range lastR,
          Range newR) {
        rangeHasChanged(newR);
      }
    });
  }

  /**
   * Apply changes to all other charts
   * 
   * @param range
   */
  private void rangeHasChanged(Range range) {
    if (setCoupleZoomY) {
      ValueAxis axis = libraryPlot.getRangeAxis();
      if (!axis.getRange().equals(range))
        axis.setRange(range);
      ValueAxis axisQuery = queryPlot.getRangeAxis();
      if (!axisQuery.getRange().equals(range))
        axisQuery.setRange(range);
    }
  }

  private JPanel extractMetaData(String title, SpectralDBEntry entry, DBEntryField[] other) {
    JPanel panelOther = new JPanel();
    panelOther.setLayout(new BoxLayout(panelOther, BoxLayout.Y_AXIS));
    panelOther.setBackground(Color.WHITE);
    panelOther.setAlignmentY(Component.TOP_ALIGNMENT);
    panelOther.setAlignmentX(Component.TOP_ALIGNMENT);

    for (DBEntryField db : other) {
      Object o = entry.getField(db).orElse("N/A");
      if (!o.equals("N/A")) {
        CustomTextPane textPane = new CustomTextPane(true);
        textPane.setText(db.toString() + ": " + o.toString());
        panelOther.add(textPane);
      }
    }

    JLabel otherInfo = new JLabel(title);
    otherInfo.setFont(headerFont);
    JPanel pn = new JPanel(new BorderLayout());
    pn.setBackground(Color.WHITE);
    pn.add(otherInfo, BorderLayout.NORTH);
    pn.add(panelOther, BorderLayout.CENTER);
    JPanel pn1 = new JPanel(new BorderLayout());
    pn1.add(pn, BorderLayout.NORTH);
    pn1.setBackground(Color.WHITE);
    return pn1;
  }


  private IAtomContainer parseInChi(SpectralDBPeakIdentity hit) {
    String inchiString = hit.getEntry().getField(DBEntryField.INCHI).orElse("N/A").toString();
    InChIGeneratorFactory factory;
    IAtomContainer molecule;
    if (inchiString != "N/A") {
      try {
        factory = InChIGeneratorFactory.getInstance();
        // Get InChIToStructure
        InChIToStructure inchiToStructure =
            factory.getInChIToStructure(inchiString, DefaultChemObjectBuilder.getInstance());
        molecule = inchiToStructure.getAtomContainer();
        return molecule;
      } catch (CDKException e) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e);
        return null;
      }
    } else
      return null;
  }

  private IAtomContainer parseSmiles(SpectralDBPeakIdentity hit) {
    SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
    String smilesString = hit.getEntry().getField(DBEntryField.SMILES).orElse("N/A").toString();
    IAtomContainer molecule;
    if (smilesString != "N/A") {
      try {
        molecule = smilesParser.parseSmiles(smilesString);
        return molecule;
      } catch (InvalidSmilesException e1) {
        String errorMessage = "Could not load 2D structure\n" + "Exception: ";
        logger.log(Level.WARNING, errorMessage, e1);
        return null;
      }
    } else
      return null;
  }

  /**
   * The mirror chart panel
   * 
   * @return
   */
  public EChartPanel getMirrorChart() {
    return mirrorChart;
  }

  /**
   * Couple y zoom of both XYPlots
   * 
   * @param selected
   */
  public void setCoupleZoomY(boolean selected) {
    setCoupleZoomY = selected;
  }


  public void setChartFont(Font chartFont) {
    this.chartFont = chartFont;
    if (mirrorChart != null) {

      // add datasets and renderer
      // set up renderer
      CombinedDomainXYPlot domainPlot = (CombinedDomainXYPlot) mirrorChart.getChart().getXYPlot();
      NumberAxis axis = (NumberAxis) domainPlot.getDomainAxis();
      axis.setLabel("m/z");
      XYPlot queryPlot = (XYPlot) domainPlot.getSubplots().get(0);
      XYPlot libraryPlot = (XYPlot) domainPlot.getSubplots().get(1);
      domainPlot.getDomainAxis().setLabelFont(chartFont);
      domainPlot.getDomainAxis().setTickLabelFont(chartFont);
      queryPlot.getRangeAxis().setLabelFont(chartFont);
      queryPlot.getRangeAxis().setTickLabelFont(chartFont);
      libraryPlot.getRangeAxis().setLabelFont(chartFont);
      libraryPlot.getRangeAxis().setTickLabelFont(chartFont);
    }
  }


  public void applySettings(ParameterSet param) {
    pnExport.removeAll();
    addExportButtons(param);
    pnExport.revalidate();
    pnExport.repaint();
  }

}
