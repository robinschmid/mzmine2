
package net.sf.mzmine.modules.autobatch;

import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.swing.JFileChooser;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ButtonParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.chartexport.FileTypeFilter;
import net.sf.mzmine.util.files.FileAndPathUtil;

public class AutoBatchParameters extends SimpleParameterSet {

  public static final FileNameParameter FILENAME = new FileNameParameter("List of files",
      "Text file with a list of raw data files (path+file)", "txt");
  public static final FileNameParameter BATCH =
      new FileNameParameter("Batch file", "Batch file", "xml");

  public static final OptionalParameter<FileNamesParameter> listenToFolder =
      new OptionalParameter<>(new FileNamesParameter("Listen for new files",
          "Automatically adds new files in the selected folder to the todo list", true), false);

  public AutoBatchParameters() {
    super(new Parameter[] {FILENAME, BATCH, listenToFolder, //
        new ButtonParameter("", "Create files list", new String[] {"Create files list"},
            new ActionListener[] {e -> {
              JFileChooser chooser = new JFileChooser();
              chooser.setMultiSelectionEnabled(true);
              chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
              if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                List<String> found = new ArrayList<>();

                File[] files = chooser.getSelectedFiles();
                for (File f : files) {
                  if (!f.isDirectory()) {
                    String s = f.getAbsolutePath();
                    if (s.toLowerCase().endsWith("imzml") || s.toLowerCase().endsWith("mzxml")
                        || s.toLowerCase().endsWith("mzml"))
                      found.add(s);
                  } else {
                    try (Stream<Path> walk = Files.walk(Paths.get(f.getAbsolutePath()))) {
                      walk.filter(Files::isRegularFile).map(x -> x.toString())
                          .filter(s -> s.toLowerCase().endsWith("imzml")
                              || s.toLowerCase().endsWith("mzxml")
                              || s.toLowerCase().endsWith("mzml"))
                          .forEach(s -> {
                            found.add(s);
                          });
                    } catch (IOException ex) {
                      ex.printStackTrace();
                    }
                  }
                }
                //
                if (found.size() > 0) {
                  String[] list = found.toArray(String[]::new);
                  MultiChoiceParameter<String> filesParam =
                      new MultiChoiceParameter<String>("Files", "Files to add to list", list, list);
                  ParameterSet param = new SimpleParameterSet(new Parameter[] {filesParam});
                  if (param.showSetupDialog(null, false).equals(ExitCode.OK)) {
                    chooser.setMultiSelectionEnabled(false);
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.setFileFilter(new FileTypeFilter("txt", "Text-file"));
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                      writeFile(chooser.getSelectedFile(),
                          param.getParameter(filesParam).getValue());
                    }
                  }
                }
              }
            }})});
  }

  private static void writeFile(File file, String[] strings) {
    file = FileAndPathUtil.getRealFilePath(file, ".txt");
    BufferedWriter writer = null;
    try {
      // Success
      try {
        if (!file.getParentFile().exists())
          file.getParentFile().mkdirs();
      } catch (Exception e) {
      }
      writer = new BufferedWriter(new FileWriter(file, true));
      for (String s : strings)
        writer.append(s + "\n");

    } catch (Throwable t) {
    } finally {
      if (writer != null)
        try {
          writer.close();
        } catch (IOException e) {
        }
    }
  }

}
