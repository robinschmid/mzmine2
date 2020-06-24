/*
 * Copyright 2006-2019 The MZmine 2 Development Team
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.drjekyll.fontchooser.FontDialog;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.spectraldb.entry.SpectralDBPeakIdentity;

/**
 * Window to show all spectral database matches from selected scan or peaklist match
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class SpectraIdentificationResultsWindow extends JFrame {
  private final Logger logger = Logger.getLogger(this.getClass().getName());

  private Font headerFont = new Font("Dialog", Font.BOLD, 16);
  private JPanel pnGrid;
  private JScrollPane scrollPane;
  private List<SpectralDBPeakIdentity> totalMatches;
  private Map<SpectralDBPeakIdentity, SpectralMatchPanel> matchPanels;
  // couple y zoom (if one is changed - change the other in a mirror plot)
  private boolean isCouplingZoomY;

  private JLabel noMatchesFound;
  private Font chartFont = new Font("Verdana", Font.PLAIN, 11);

  public SpectraIdentificationResultsWindow() {
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setSize(new Dimension(1400, 900));
    getContentPane().setLayout(new BorderLayout());
    setTitle("Processing...");

    pnGrid = new JPanel();
    // any number of rows
    pnGrid.setLayout(new GridLayout(0, 1, 0, 0));

    pnGrid.setBackground(Color.WHITE);
    pnGrid.setAutoscrolls(false);

    noMatchesFound = new JLabel("I'm working on it", SwingConstants.CENTER);
    noMatchesFound.setFont(headerFont);
    // yellow
    noMatchesFound.setForeground(new Color(0xFFCC00));
    pnGrid.add(noMatchesFound, BorderLayout.CENTER);

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());

    final JMenuItem btnToggleSorting = new JMenuItem("Toggle sorting: ");
    // set font size of chart
    JMenuItem btnSetup = new JMenuItem("Setup dialog");
    btnSetup.addActionListener(e -> {
      ParameterSet param = MZmineCore.getConfiguration()
          .getModuleParameters(SpectraIdentificationResultsModule.class);
      MatchSortMode oldSorting =
          param.getParameter(SpectraIdentificationResultsParameters.sorting).getValue();
      double oldWeight =
          param.getParameter(SpectraIdentificationResultsParameters.weightScore).getValue();
      if (param.showSetupDialog(this, true) == ExitCode.OK) {
        showExportButtonsChanged();

        // sorting has changed
        MatchSortMode newSorting =
            param.getParameter(SpectraIdentificationResultsParameters.sorting).getValue();
        double newWeight =
            param.getParameter(SpectraIdentificationResultsParameters.weightScore).getValue();
        if (Double.compare(oldWeight, newWeight) != 0 || !oldSorting.equals(newSorting)) {
          btnToggleSorting.setText("Toggle sorting: " + newSorting.toString());

          sortTotalMatches();
        }
      }
    });
    menuBar.add(btnSetup);


    try {
      ParameterSet param = MZmineCore.getConfiguration()
          .getModuleParameters(SpectraIdentificationResultsModule.class);
      MatchSortMode sorting =
          param.getParameter(SpectraIdentificationResultsParameters.sorting).getValue();
      if (sorting == null) {
        sorting = MatchSortMode.MATCH_SCORE;
        param.getParameter(SpectraIdentificationResultsParameters.sorting).setValue(sorting);
      }
      btnToggleSorting.setText("Toggle sorting: " + sorting.toString());
      btnToggleSorting.addActionListener(e -> {
        ParameterSet param2 = MZmineCore.getConfiguration()
            .getModuleParameters(SpectraIdentificationResultsModule.class);
        MatchSortMode sorting2 =
            param2.getParameter(SpectraIdentificationResultsParameters.sorting).getValue();
        // next sort mode
        sorting2 = MatchSortMode.values()[(sorting2.ordinal() + 1) % MatchSortMode.values().length];
        param2.getParameter(SpectraIdentificationResultsParameters.sorting).setValue(sorting2);

        btnToggleSorting.setText("Toggle sorting: " + sorting2.toString());
        sortTotalMatches();
      });
      menuBar.add(btnToggleSorting);
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Somehow no parameters were available for the lib match window");
    }


    JCheckBoxMenuItem cbCoupleZoomY = new JCheckBoxMenuItem("Couple y-zoom");
    cbCoupleZoomY.setSelected(true);
    cbCoupleZoomY.addItemListener(e -> setCoupleZoomY(cbCoupleZoomY.isSelected()));
    menuBar.add(cbCoupleZoomY);

    JMenuItem btnSetFont = new JMenuItem("Set chart font");
    btnSetFont.addActionListener(e -> setChartFont());
    menuBar.add(btnSetFont);

    setJMenuBar(menuBar);

    scrollPane = new JScrollPane(pnGrid);
    getContentPane().add(scrollPane, BorderLayout.CENTER);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setViewportView(pnGrid);

    totalMatches = new ArrayList<>();
    matchPanels = new HashMap<>();
    setCoupleZoomY(true);

    setVisible(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    validate();
    repaint();
  }

  private void setChartFont() {
    FontDialog dialog = new FontDialog(this, "Font Dialog Example", true);
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
    if (!dialog.isCancelSelected()) {
      setChartFont(dialog.getSelectedFont());
    }
  }

  public void setCoupleZoomY(boolean selected) {
    isCouplingZoomY = selected;

    synchronized (matchPanels) {
      matchPanels.values().stream().filter(Objects::nonNull)
          .forEach(pn -> pn.setCoupleZoomY(selected));
    }
  }

  /**
   * Add a new match and sort the view
   * 
   * @param scan
   * @param match
   */
  public synchronized void addMatches(SpectralDBPeakIdentity match) {
    if (!totalMatches.contains(match)) {
      // add
      totalMatches.add(match);
      SpectralMatchPanel pn = new SpectralMatchPanel(match);
      pn.setCoupleZoomY(isCouplingZoomY);
      matchPanels.put(match, pn);

      // sort and show
      sortTotalMatches();
    }
  }

  /**
   * add all matches and sort the view
   * 
   * @param scan
   * @param matches
   */
  public synchronized void addMatches(List<SpectralDBPeakIdentity> matches) {
    // add all
    for (SpectralDBPeakIdentity match : matches) {
      if (!totalMatches.contains(match)) {
        // add
        totalMatches.add(match);
        SpectralMatchPanel pn = new SpectralMatchPanel(match);
        pn.setCoupleZoomY(isCouplingZoomY);
        matchPanels.put(match, pn);
      }
    }
    // sort and show
    sortTotalMatches();
  }

  /**
   * Sort all matches and renew panels
   * 
   */
  public void sortTotalMatches() {
    if (totalMatches.isEmpty()) {
      return;
    }

    // reversed sorting (highest cosine first
    synchronized (totalMatches) {
      ParameterSet param = MZmineCore.getConfiguration()
          .getModuleParameters(SpectraIdentificationResultsModule.class);
      MatchSortMode sorting =
          param.getParameter(SpectraIdentificationResultsParameters.sorting).getValue();

      switch (sorting) {
        case COMBINED:
          double factorScore =
              param.getParameter(SpectraIdentificationResultsParameters.weightScore).getValue();
          totalMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double
              .compare(calcCombinedScore(b, factorScore), calcCombinedScore(a, factorScore)));
          break;
        case EXPLAINED_LIBRARY_INTENSITY:
          totalMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double.compare(
              b.getSimilarity().getExplainedLibraryIntensityRatio(),
              a.getSimilarity().getExplainedLibraryIntensityRatio()));
          break;
        case MATCH_SCORE:
        default:
          totalMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double
              .compare(b.getSimilarity().getScore(), a.getSimilarity().getScore()));
          break;
      }
    }
    // renew layout and show
    renewLayout();
  }

  public double calcCombinedScore(SpectralDBPeakIdentity id, double factorScore) {
    return (id.getSimilarity().getScore() * factorScore
        + id.getSimilarity().getExplainedLibraryIntensityRatio()) / (factorScore + 1d);
  }

  public void setMatchingFinished() {
    if (totalMatches.isEmpty()) {
      noMatchesFound.setText("Sorry no matches found");
      noMatchesFound.setForeground(Color.RED);
    }
  }

  /**
   * Add a spectral library hit
   * 
   * @param ident
   * @param simScore
   */
  public void renewLayout() {
    SwingUtilities.invokeLater(() -> {
      // any number of rows
      JPanel pnGrid = new JPanel(new GridLayout(0, 1, 0, 5));
      pnGrid.setBackground(Color.WHITE);
      pnGrid.setAutoscrolls(false);
      // add all panel in order
      synchronized (totalMatches) {
        for (SpectralDBPeakIdentity match : totalMatches) {
          JPanel pn = matchPanels.get(match);
          if (pn != null)
            pnGrid.add(pn);
        }
      }
      // show
      scrollPane.setViewportView(pnGrid);
      scrollPane.getVerticalScrollBar().setUnitIncrement(75);
      pnGrid.revalidate();
      scrollPane.revalidate();
      scrollPane.repaint();
      this.pnGrid = pnGrid;
    });
  }

  public Font getChartFont() {
    return chartFont;
  }

  public void setChartFont(Font chartFont) {
    this.chartFont = chartFont;
    if (matchPanels == null)
      return;
    matchPanels.values().stream().forEach(pn -> {
      pn.setChartFont(chartFont);
    });
  }


  private void showExportButtonsChanged() {
    if (matchPanels == null)
      return;
    matchPanels.values().stream().forEach(pn -> {
      pn.applySettings(MZmineCore.getConfiguration()
          .getModuleParameters(SpectraIdentificationResultsModule.class));
    });
  }
}
