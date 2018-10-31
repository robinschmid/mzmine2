package net.sf.mzmine.util.maths.similarity;

public abstract class Similarity {

  // Measures
  public static final Similarity COSINE = new Similarity() {
    @Override
    public double calc(double[][] data) {
      return dot(data) / (Math.sqrt(norm(data, 0)) * Math.sqrt(norm(data, 1)));
    }
  };


  // #############################################
  // abstract methods
  /**
   * 
   * @param data data[dp][0,1]
   * @return
   */
  public abstract double calc(double[][] data);

  // ############################################
  // COMMON METHODS
  /**
   * sum(x*y)
   * 
   * @param data data[dp][x,y]
   * @return
   */
  public double dot(double[][] data) {
    double sum = 0;
    for (double[] val : data)
      sum += val[0] * val[1];
    return sum;
  }

  /**
   * Euclidean norm (self dot product). sum(x*x)
   * 
   * @param data data[dp][indexOfX]
   * @param index
   * @return
   */
  public double norm(double[][] data, int index) {
    double sum = 0;
    for (double[] val : data)
      sum += val[index] * val[index];
    return sum;
  }
}
