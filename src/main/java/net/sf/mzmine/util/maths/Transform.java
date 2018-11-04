package net.sf.mzmine.util.maths;

@FunctionalInterface
public interface Transform {

  public static final Transform LOG = v -> Math.log(v);

  public double transform(double v);
}
