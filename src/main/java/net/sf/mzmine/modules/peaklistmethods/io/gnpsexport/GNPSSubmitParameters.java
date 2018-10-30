/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;


public class GNPSSubmitParameters extends SimpleParameterSet {

  public enum Preset {
    HIGHRES, LOWRES;
  }

  public static final OptionalParameter<FileNameParameter> META_FILE =
      new OptionalParameter<FileNameParameter>(
          new FileNameParameter("Meta data file", "Optional meta file for GNPS"), false);

  public static final ComboParameter<Preset> PRESETS = new ComboParameter<>("Presets",
      "GNPS parameter presets for high or low resolution mass spectrometry data", Preset.values(),
      Preset.HIGHRES);

  public static final StringParameter EMAIL =
      new StringParameter("Email", "Email adresse for notifications about the job");
  public static final BooleanParameter ANN_EDGES =
      new BooleanParameter("Annotation edges", "Add annotation edges to GNPS job", true);
  public static final BooleanParameter CORR_EDGES =
      new BooleanParameter("Correlation edges", "Add correlation edges to GNPS job", false);

  public static final BooleanParameter OPEN_WEBSITE =
      new BooleanParameter("Open website", "Website of GNPS job", true);

  public GNPSSubmitParameters() {
    super(new Parameter[] {META_FILE, PRESETS, EMAIL, ANN_EDGES, CORR_EDGES, OPEN_WEBSITE});
  }
}
