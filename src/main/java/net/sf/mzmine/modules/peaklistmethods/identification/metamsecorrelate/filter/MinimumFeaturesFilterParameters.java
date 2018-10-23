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

package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.filter;

import java.awt.Window;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.absrel.AbsoluteNRelativeInt;
import net.sf.mzmine.parameters.parametertypes.absrel.AbsoluteNRelativeInt.Mode;
import net.sf.mzmine.parameters.parametertypes.absrel.AbsoluteNRelativeIntParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import net.sf.mzmine.util.ExitCode;

public class MinimumFeaturesFilterParameters extends SimpleParameterSet {


  private final boolean isSub;
  // sample sets
  public static final OptionalParameter<ComboParameter<Object>> GROUPSPARAMETER =
      new OptionalParameter<ComboParameter<Object>>(new ComboParameter<Object>("Sample set",
          "Paremeter defining the sample set of each sample. (Set them in Project/Set sample parameters)",
          new Object[0]));

  //
  public static final RTToleranceParameter RT_TOLERANCE = new RTToleranceParameter("RT tolerance",
      "Maximum allowed difference of retention time to set a relationship between peaks");


  // minimum of samples per group (with the feature detected or filled in (min height?)
  // ... showing RT<=tolerance and height>=minHeight
  public static final AbsoluteNRelativeIntParameter MIN_SAMPLES_GROUP =
      new AbsoluteNRelativeIntParameter("Min samples in group",
          "Minimum of samples per group (with the feature detected or filled in) matching the conditions (in RT-range).",
          0, 0, Mode.ROUND_DOWN);

  // minimum of samples per all (with the feature detected or filled in (min height?)
  // ... showing RT<=tolerance and height>=minHeight
  public static final AbsoluteNRelativeIntParameter MIN_SAMPLES_ALL =
      new AbsoluteNRelativeIntParameter("Min samples in all",
          "Minimum of samples per group (with the feature detected or filled in) matching the conditions (in RT-range).",
          1, 0, Mode.ROUND_DOWN, 1);

  /**
   * Filter by minimum height
   */
  public static final DoubleParameter MIN_HEIGHT =
      new DoubleParameter("Min height", "Minimum height to recognize a feature");

  public static final PercentParameter MIN_INTENSITY_OVERLAP = new PercentParameter(
      "Min %-intensity overlap",
      "The smaller feature has to overlap with at least X% of its intensity with the other feature",
      0.6);

  /**
   * do not accept one feature out of RTTolerance or minPercOverlap
   */
  public static final BooleanParameter STRICT_RULES = new BooleanParameter("Strict rules",
      "Do not allow one feature to be out of RTTolerance or minimum intensity overlap, when testing overlap of features.",
      true);

  /**
   * 
   */
  public MinimumFeaturesFilterParameters() {
    this(false);
  }

  /**
   * Sub has no grouping parameter and no RTTolerance
   * 
   * @param isSub
   */
  public MinimumFeaturesFilterParameters(boolean isSub) {
    super(isSub
        ? new Parameter[] {MIN_HEIGHT, MIN_SAMPLES_ALL, MIN_SAMPLES_GROUP, MIN_INTENSITY_OVERLAP,
            STRICT_RULES}
        : new Parameter[] {GROUPSPARAMETER, RT_TOLERANCE, MIN_HEIGHT, MIN_SAMPLES_ALL,
            MIN_SAMPLES_GROUP, MIN_INTENSITY_OVERLAP, STRICT_RULES});
    this.isSub = isSub;
  }

  /**
   * Creates the filter with groups
   * 
   * @param groupingParameter
   * @param rawDataFiles
   * @param project
   * 
   * @return
   */
  public MinimumFeatureFilter createFilterWithGroups(MZmineProject project,
      RawDataFile[] rawDataFiles, String groupingParameter) {
    AbsoluteNRelativeInt minFInSamples = this.getParameter(MIN_SAMPLES_ALL).getValue();
    AbsoluteNRelativeInt minFInGroups = this.getParameter(MIN_SAMPLES_GROUP).getValue();
    double minFeatureHeight = this.getParameter(MIN_HEIGHT).getValue();
    double minIPercOverlap = this.getParameter(MIN_INTENSITY_OVERLAP).getValue();
    boolean strict = this.getParameter(STRICT_RULES).getValue();
    return new MinimumFeatureFilter(project, rawDataFiles, groupingParameter, minFInSamples,
        minFInGroups, minFeatureHeight, minIPercOverlap, strict);
  }

  /**
   * Creates the filter without groups
   * 
   * @return
   */
  public MinimumFeatureFilter createFilter() {
    AbsoluteNRelativeInt minFInSamples = this.getParameter(MIN_SAMPLES_ALL).getValue();
    AbsoluteNRelativeInt minFInGroups = this.getParameter(MIN_SAMPLES_GROUP).getValue();
    double minFeatureHeight = this.getParameter(MIN_HEIGHT).getValue();
    double minIPercOverlap = this.getParameter(MIN_INTENSITY_OVERLAP).getValue();
    boolean strict = this.getParameter(STRICT_RULES).getValue();
    return new MinimumFeatureFilter(minFInSamples, minFInGroups, minFeatureHeight, minIPercOverlap,
        strict);
  }


  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {
    // Update the parameter choices
    if (isSub)
      return super.showSetupDialog(parent, valueCheckRequired);
    else {
      OptionalParameter<ComboParameter<Object>> gParam = getParameter(GROUPSPARAMETER);
      if (gParam != null) {
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
        gParam.getEmbeddedParameter().setChoices(choices);
        if (choices.length > 1)
          gParam.getEmbeddedParameter().setValue(choices[1]);
      }

      ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this);
      dialog.setVisible(true);
      return dialog.getExitCode();
    }
  }
}
