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

import java.util.Collection;
import javax.annotation.Nonnull;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.modules.MZmineModuleCategory;
import net.sf.mzmine.modules.MZmineProcessingModule;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.util.ExitCode;

public class LibraryMatchResultsExportModule implements MZmineProcessingModule {
  private static final String MODULE_NAME = "Export spectral library matching results";
  private static final String MODULE_DESCRIPTION = "Export spectral library matching results";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {
    LibraryMatchResultsExportTask task = new LibraryMatchResultsExportTask(parameters);
    tasks.add(task);
    return ExitCode.OK;
  }


  // public static void exportSinglePeakList(PeakListRow row) {
  //
  // try {
  // ParameterSet parameters =
  // MZmineCore.getConfiguration().getModuleParameters(LibraryMatchResultsExportModule.class);
  //
  // ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true);
  // if (exitCode != ExitCode.OK)
  // return;
  // // Open file
  // final LibraryMatchResultsExportTask task = new LibraryMatchResultsExportTask(parameters);
  // task.runSingleRow(row);
  // } catch (Exception e) {
  // e.printStackTrace();
  // MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
  // "Error while exporting feature to SIRIUS: " + ExceptionUtils.exceptionToString(e));
  // }
  //
  // }
  //
  //
  // public static void exportSingleRows(PeakListRow[] row) {
  // try {
  // ParameterSet parameters =
  // MZmineCore.getConfiguration().getModuleParameters(LibraryMatchResultsExportModule.class);
  //
  // ExitCode exitCode = parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(), true);
  // if (exitCode != ExitCode.OK)
  // return;
  // // Open file
  // final LibraryMatchResultsExportTask task = new LibraryMatchResultsExportTask(parameters);
  // task.runSingleRows(row);
  // } catch (Exception e) {
  // e.printStackTrace();
  // MZmineCore.getDesktop().displayErrorMessage(MZmineCore.getDesktop().getMainWindow(),
  // "Error while exporting feature to SIRIUS: " + ExceptionUtils.exceptionToString(e));
  // }
  // }

  @Override
  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.PEAKLISTEXPORT;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return LibraryMatchResultsExportParameters.class;
  }

}

