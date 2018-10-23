/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate;

import java.awt.Window;
import javax.swing.JComponent;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation.FeatureShapeCorrelationParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.correlation.InterSampleIntCorrParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.MSAnnotationParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msannotation.refinement.AnnotationRefinementParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class MetaMSEcorrelateParameters extends SimpleParameterSet {

  // General parameters
  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
  // RT-tolerance: Grouping
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");

  // GROUPING
  // sample sets
  public static final OptionalParameter<ComboParameter<Object>> GROUPSPARAMETER =
      new OptionalParameter<ComboParameter<Object>>(new ComboParameter<Object>("Sample set",
          "Paremeter defining the sample set of each sample. (Set them in Project/Set sample parameters)",
          new Object[0]));


  /**
   * Filter out by minimum number of features in all samples and/or in at least one sample group
   * features with height>=minHeight
   */
  public static final OptionalModuleParameter MIN_SAMPLES_FILTER =
      new OptionalModuleParameter("Min samples filter",
          "Filter out by min number of features in all samples and in sample groups",
          new MinimumFeaturesFilterParameters(true));


  // Sub parameters of correlation grouping
  public static final SubModuleParameter FSHAPE_CORRELATION = new SubModuleParameter(
      "Correlation grouping", "Grouping based on Pearson correlation of the feature shapes.",
      new FeatureShapeCorrelationParameters(true));

  public static final OptionalModuleParameter IMAX_CORRELATION =
      new OptionalModuleParameter("Feature height correlation",
          "Feature to feature correlation of the maximum intensities across all samples.",
          new InterSampleIntCorrParameters());



  // #####################################################################################
  // Intensity profile correlation
  // intra group comparison

  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final OptionalModuleParameter ADDUCT_LIBRARY =
      new OptionalModuleParameter("MS annotations",
          "Build adduct, in-source fragment, cluster,.. library and match all features",
          new MSAnnotationParameters(true));

  public static final BooleanParameter ANNOTATE_ONLY_GROUPED = new BooleanParameter(
      "Annotate only corr grouped", "Only rows in a correlation group are checked for annotations");

  public static final OptionalModuleParameter<AnnotationRefinementParameters> ANNOTATION_REFINEMENTS =
      new OptionalModuleParameter<AnnotationRefinementParameters>("Annotation refinement", "",
          new AnnotationRefinementParameters(true));


  // Constructor
  public MetaMSEcorrelateParameters() {
    super(new Parameter[] {PEAK_LISTS, RT_TOLERANCE,
        // Group and minimum samples filter
        GROUPSPARAMETER, MIN_SAMPLES_FILTER,
        // feature shape correlation
        FSHAPE_CORRELATION,
        // intensity max correlation
        IMAX_CORRELATION,
        // adducts
        ADDUCT_LIBRARY, ANNOTATE_ONLY_GROUPED, ANNOTATION_REFINEMENTS});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    // Update the parameter choices
    UserParameter<?, ?> newChoices[] =
        MZmineCore.getProjectManager().getCurrentProject().getParameters();
    String[] choices;
    if (newChoices == null || newChoices.length == 0) {
      choices = new String[1];
      choices[0] = "No groups";
    } else {
      choices = new String[newChoices.length + 1];
      choices[0] = "No groups";
      for (int i = 0; i < newChoices.length; i++) {
        choices[i + 1] = newChoices[i].getName();
      }
    }
    getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
        .setChoices(choices);
    if (choices.length > 1)
      getParameter(MetaMSEcorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
          .setValue(choices[1]);

    ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);

    // enable
    JComponent com = dialog.getComponentForParameter(ANNOTATE_ONLY_GROUPED);
    OptionalModuleComponent adducts =
        (OptionalModuleComponent) dialog.getComponentForParameter(ADDUCT_LIBRARY);
    adducts.addItemListener(e -> com.setEnabled(adducts.isSelected()));

    dialog.setVisible(true);
    return dialog.getExitCode();
  }
}
