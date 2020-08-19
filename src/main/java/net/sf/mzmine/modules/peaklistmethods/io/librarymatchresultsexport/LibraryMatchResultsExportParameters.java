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

package net.sf.mzmine.modules.peaklistmethods.io.librarymatchresultsexport;

import java.text.DecimalFormat;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.MatchSortMode;
import net.sf.mzmine.modules.visualization.spectra.spectralmatchresults.SpectralMatchCompareParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;


public class LibraryMatchResultsExportParameters extends SimpleParameterSet {

  public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

  public static final FileNameParameter FILENAME = new FileNameParameter("Export file",
      "Name of the output csv (comma-separated values) file. "
          + "Use pattern \"{}\" in the file name to substitute with feature list name. "
          + "(i.e. \"blah{}blah.csv\" would become \"blahSourcePeakListNameblah.csv\"). "
          + "If the file already exists, it will be overwritten.",
      "csv");

  public static final MassListParameter MASS_LIST = new MassListParameter();



  public static final DoubleParameter weightScore = new DoubleParameter("Weight for combined score",
      "Weight is applied to match score to calculate combined score: (WEIGHT*score + explainedIntensity)/2",
      new DecimalFormat("0.0"), 2.0, 0.00001, 1000000d);

  public static final ComboParameter<MatchSortMode> sorting = new ComboParameter<>(
      "Sort matches by", "Sort matches by score or explained library intensity",
      MatchSortMode.values(), MatchSortMode.MATCH_SCORE);

  public static final MZToleranceParameter mzTol =
      new MZToleranceParameter("m/z tolerance", "Annotation m/z tolerance", 0.003, 10);

  public static final OptionalModuleParameter<SpectralMatchCompareParameters> collapse =
      new OptionalModuleParameter<>("Collapse duplicates",
          "Collapse duplicate matches to the same spectral entry",
          new SpectralMatchCompareParameters(), true);
  

  public LibraryMatchResultsExportParameters() {
    super(new Parameter[] {PEAK_LISTS, FILENAME, MASS_LIST, collapse, // collapse results
        sorting, weightScore, // sorting of matches
        mzTol // annotation of ions
        });
    }
  }

}
