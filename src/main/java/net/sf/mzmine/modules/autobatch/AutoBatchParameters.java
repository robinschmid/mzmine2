
package net.sf.mzmine.modules.autobatch;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;

public class AutoBatchParameters extends SimpleParameterSet {

  public static final FileNameParameter FILENAME = new FileNameParameter("List of files",
      "Text file with a list of raw data files (path+file)", "txt");
  public static final FileNameParameter BATCH =
      new FileNameParameter("Batch file", "Batch file", "xml");

  public AutoBatchParameters() {
    super(new Parameter[] {FILENAME, BATCH});
  }
}
