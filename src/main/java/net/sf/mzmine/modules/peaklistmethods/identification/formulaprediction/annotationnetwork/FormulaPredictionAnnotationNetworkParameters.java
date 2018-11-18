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
package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.annotationnetwork;

import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.singlerow.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.singlerow.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.singlerow.restrictions.rdbe.RDBERestrictionParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.sort.FormulaSortParameters;
import net.sf.mzmine.modules.peaklistmethods.isotopes.isotopepatternscore.IsotopePatternScoreParameters;
import net.sf.mzmine.modules.peaklistmethods.msms.msmsscore.MSMSScoreParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

public class FormulaPredictionAnnotationNetworkParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final DoubleParameter ppmOffset = new DoubleParameter("Center by ppm offset",
      "Linear correction to mass difference offset. If all correct results are shifted by +4 ppm use -4 ppm to shift these molecular formulae to the center");

  public static final OptionalModuleParameter<FormulaSortParameters> sorting =
      new OptionalModuleParameter<FormulaSortParameters>("Sorting",
          "Apply sorting to all resulting lists", new FormulaSortParameters(true));

  public static final ElementsParameter elements =
      new ElementsParameter("Elements", "Elements and ranges");

  public static final OptionalModuleParameter elementalRatios =
      new OptionalModuleParameter("Element count heuristics",
          "Restrict formulas by heuristic restrictions of elemental counts and ratios",
          new ElementalHeuristicParameters());

  public static final OptionalModuleParameter rdbeRestrictions =
      new OptionalModuleParameter("RDBE restrictions",
          "Search only for formulas which correspond to the given RDBE restrictions",
          new RDBERestrictionParameters());

  public static final OptionalModuleParameter isotopeFilter = new OptionalModuleParameter(
      "Isotope pattern filter", "Search only for formulas with a isotope pattern similar",
      new IsotopePatternScoreParameters());

  public static final OptionalModuleParameter msmsFilter =
      new OptionalModuleParameter("MS/MS filter", "Check MS/MS data", new MSMSScoreParameters());

  public FormulaPredictionAnnotationNetworkParameters() {
    this(false);
  }

  public FormulaPredictionAnnotationNetworkParameters(boolean isSub) {
    super(isSub ? //
        new Parameter[] {ppmOffset, sorting, elements, elementalRatios, rdbeRestrictions,
            isotopeFilter, msmsFilter}
        : new Parameter[] {PEAK_LISTS, mzTolerance, ppmOffset, sorting, elements, elementalRatios,
            rdbeRestrictions, isotopeFilter, msmsFilter});
  }
}
