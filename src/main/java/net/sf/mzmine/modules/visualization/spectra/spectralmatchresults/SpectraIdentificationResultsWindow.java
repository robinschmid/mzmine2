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
import java.util.logging.Logger;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.sf.mzmine.desktop.impl.WindowsMenu;
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

    // add label (is replaced later
    JLabel noMatchesFound = new JLabel("Sorry, no matches found!", SwingConstants.CENTER);
    noMatchesFound.setFont(headerFont);
    noMatchesFound.setForeground(Color.RED);
    pnGrid.add(noMatchesFound, BorderLayout.CENTER);

    // Add the Windows menu
    JMenuBar menuBar = new JMenuBar();
    menuBar.add(new WindowsMenu());
    JCheckBoxMenuItem cbCoupleZoomY = new JCheckBoxMenuItem("Couple y-zoom");
    cbCoupleZoomY.setSelected(true);
    cbCoupleZoomY.addItemListener(e -> setCoupleZoomY(cbCoupleZoomY.isSelected()));
    menuBar.add(cbCoupleZoomY);
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
      totalMatches.sort((SpectralDBPeakIdentity a, SpectralDBPeakIdentity b) -> Double
          .compare(b.getSimilarity().getScore(), a.getSimilarity().getScore()));
    }
    // renew layout and show
    renewLayout();
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
}
