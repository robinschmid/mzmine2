package net.sf.mzmine.util.files;

import java.io.File;

public class PathLengthException extends RuntimeException {

  public PathLengthException(File f) {
    super("Path is too long: " + f.getAbsolutePath());
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

}
