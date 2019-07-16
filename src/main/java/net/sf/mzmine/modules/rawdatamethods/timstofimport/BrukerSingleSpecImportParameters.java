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

package net.sf.mzmine.modules.rawdatamethods.timstofimport;

import java.awt.Window;
import java.io.File;
import javax.swing.JFileChooser;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.util.ExitCode;

public class BrukerSingleSpecImportParameters extends SimpleParameterSet {

  public static final FileNameParameter fileName = new FileNameParameter("Folder", "Folder");

  public BrukerSingleSpecImportParameters() {
    super(new Parameter[] {fileName});
  }

  @Override
  public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) {

    JFileChooser chooser = new JFileChooser();

    // We need to allow directories, because Waters raw data come in
    // directories, not files
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

    File lastFiles = getParameter(fileName).getValue();
    if ((lastFiles != null)) {
      File currentDir = lastFiles.getParentFile();
      if ((currentDir != null) && (currentDir.exists()))
        chooser.setCurrentDirectory(currentDir);
      chooser.setSelectedFile(lastFiles);
    }

    chooser.setMultiSelectionEnabled(false);

    int returnVal = chooser.showOpenDialog(parent);

    if (returnVal != JFileChooser.APPROVE_OPTION)
      return ExitCode.CANCEL;

    File selectedFiles = chooser.getSelectedFile();

    getParameter(fileName).setValue(selectedFiles);

    return ExitCode.OK;
  }

}
