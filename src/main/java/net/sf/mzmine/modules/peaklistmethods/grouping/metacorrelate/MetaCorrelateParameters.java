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

package net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate;

import java.awt.Window;
import javax.swing.JComponent;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.correlation.FeatureShapeCorrelationParameters;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.correlation.InterSampleHeightCorrParameters;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.minfeaturefilter.MinimumFeaturesFilterParameters;
import net.sf.mzmine.modules.peaklistmethods.grouping.metacorrelate.msms.similarity.MS2SimilarityParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.IonNetworkingParameters;
import net.sf.mzmine.modules.peaklistmethods.identification.ionidentity.ionidnetworking.IonNetworkingParameters.Setup;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class MetaCorrelateParameters extends SimpleParameterSet {

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
          new Object[0]), false);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter MIN_HEIGHT = new DoubleParameter("Min height",
      "Used by min samples filter and MS annotations. Minimum height to recognize a feature (important to destinguis between real peaks and minor gap-filled).",
      MZmineCore.getConfiguration().getIntensityFormat(), 1E5);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter NOISE_LEVEL =
      new DoubleParameter("Noise level", "Noise level of MS1, used by feature shape correlation",
          MZmineCore.getConfiguration().getIntensityFormat(), 1E4);

  /**
   * Filter out by minimum number of features in all samples and/or in at least one sample group
   * features with height>=minHeight
   */
  public static final SubModuleParameter MIN_SAMPLES_FILTER =
      new SubModuleParameter("Min samples filter",
          "Filter out by min number of features in all samples and in sample groups",
          new MinimumFeaturesFilterParameters(true));


  // Sub parameters of correlation grouping
  public static final OptionalModuleParameter<FeatureShapeCorrelationParameters> FSHAPE_CORRELATION =
      new OptionalModuleParameter<>("Correlation grouping",
          "Grouping based on Pearson correlation of the feature shapes.",
          new FeatureShapeCorrelationParameters(true), true);

  public static final OptionalModuleParameter<InterSampleHeightCorrParameters> IMAX_CORRELATION =
      new OptionalModuleParameter<>("Feature height correlation",
          "Feature to feature correlation of the maximum intensities across all samples.",
          new InterSampleHeightCorrParameters(true), true);



  // #####################################################################################
  // Intensity profile correlation
  // intra group comparison

  // adduct finder parameter - taken from the adduct finder
  // search for adducts? Bonus for correlation?
  public static final OptionalModuleParameter<IonNetworkingParameters> ADDUCT_LIBRARY =
      new OptionalModuleParameter<>("MS annotations",
          "Build adduct, in-source fragment, cluster,.. library and match all features",
          new IonNetworkingParameters(Setup.SUB), true);

  public static final BooleanParameter ANNOTATE_ONLY_GROUPED =
      new BooleanParameter("Annotate only corr grouped",
          "Only rows in a correlation group are checked for annotations", true);

  public static final OptionalParameter<StringParameter> SUFFIX = new OptionalParameter<>(
      new StringParameter("Suffix (or auto)", "Select suffix or deselect for auto suffix"), false);

  public static final OptionalModuleParameter<MS2SimilarityParameters> MS2_SIMILARITY =
      new OptionalModuleParameter<>("MS2 similarity check",
          "MS2 similarity check on all rows of the same group", new MS2SimilarityParameters(true),
          false);

  // Constructor
  public MetaCorrelateParameters() {
    super(new Parameter[] {PEAK_LISTS, RT_TOLERANCE,
        // Group and minimum samples filter
        GROUPSPARAMETER,
        // feature filter
        MIN_HEIGHT, NOISE_LEVEL, MIN_SAMPLES_FILTER,
        // feature shape correlation
        FSHAPE_CORRELATION,
        // intensity max correlation
        IMAX_CORRELATION,
        // MS2 similarity
        MS2_SIMILARITY,
        // adducts
        ADDUCT_LIBRARY, ANNOTATE_ONLY_GROUPED,
        // suffix or auto suffix
        SUFFIX});
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
    getParameter(MetaCorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
        .setChoices(choices);
    if (choices.length > 1)
      getParameter(MetaCorrelateParameters.GROUPSPARAMETER).getEmbeddedParameter()
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
