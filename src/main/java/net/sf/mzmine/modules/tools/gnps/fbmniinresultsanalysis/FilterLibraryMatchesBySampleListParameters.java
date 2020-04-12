package net.sf.mzmine.modules.tools.gnps.fbmniinresultsanalysis;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;

/**
 * Fitler the library matches by a sample list.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class FilterLibraryMatchesBySampleListParameters extends SimpleParameterSet {


  public static final FileNameParameter SAMPLE_LIST = new FileNameParameter("Sample list",
      "First column the sample name or plate number. Filter the library matches by a sample list.",
      "csv");

  public static final FileNameParameter QUANT_LIST =
      new FileNameParameter("Quant table", "Quantification table of FBMN IIN job", "csv");

  public static final OptionalParameter<StringParameter> SAMPLE_HEADER = new OptionalParameter<>(
      new StringParameter("Sample header", "Header of the samples column", "FILENAME"));

  public static final OptionalParameter<StringParameter> PLATE_NUMBER_HEADER =
      new OptionalParameter<>(
          new StringParameter("Plate number header", "Plate number header", "PlateID"));

  public static final StringParameter COMPOUND_NAME_HEADER =
      new StringParameter("Compound name header",
          "Compound name header is used by a starts with comparison to the library match", "Name");
  public static final StringParameter SEPARATOR = new StringParameter("Column separator", "", ",");

  public FilterLibraryMatchesBySampleListParameters() {
    super(new Parameter[] {SAMPLE_LIST, QUANT_LIST, SEPARATOR, SAMPLE_HEADER, PLATE_NUMBER_HEADER,
        COMPOUND_NAME_HEADER});
  }
}
