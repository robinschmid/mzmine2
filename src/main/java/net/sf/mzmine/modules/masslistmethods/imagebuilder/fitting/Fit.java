package net.sf.mzmine.modules.masslistmethods.imagebuilder.fitting;

public class Fit {


  // pick masses by gaussian fit
  private double start = -1;
  private double end = 0;
  private int dpCount = 0;
  // double[] {normFactor, mean, sigma}
  private double[] fit = null;



  public Fit(double start, double end, int dpCount, double[] fit) {
    super();
    this.start = start;
    this.end = end;
    this.dpCount = dpCount;
    this.fit = fit;
  }

  public double getStart() {
    return start;
  }

  public void setStart(double start) {
    this.start = start;
  }

  public double getEnd() {
    return end;
  }

  public void setEnd(double end) {
    this.end = end;
  }

  public int getDpCount() {
    return dpCount;
  }

  public void setDpCount(int dpCount) {
    this.dpCount = dpCount;
  }

  public double[] getFit() {
    return fit;
  }

  public void setFit(double[] fit) {
    this.fit = fit;
  }

  @Override
  public String toString() {
    return "Gaussian fit from " + start + " to " + end + "(" + fitToString() + ")";
  }

  private String fitToString() {
    return fit[1] + "+-" + fit[2] + "(norm=" + fit[0] + ")";
  }

  public double getSigma() {
    return fit[2];
  }

  public double getMean() {
    return fit[1];
  }

  public double getNorm() {
    return fit[0];
  }
}
