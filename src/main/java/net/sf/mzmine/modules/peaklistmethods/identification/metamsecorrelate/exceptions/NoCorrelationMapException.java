package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.exceptions;

/**
 * No correlation map. set correlation map before setting groups
 * 
 * @author Robin Schmid
 *
 */
public class NoCorrelationMapException extends Exception {
  private static final long serialVersionUID = 1L;

  public NoCorrelationMapException(String message) {
    super(message);
  }
}
