package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math.stat.regression.SimpleRegression;
import net.sf.mzmine.util.maths.similarity.Similarity;

/**
 * correlation of two peak shapes
 * 
 * @author RibRob
 *
 */
public class CorrelationData {

  public enum SimilarityMeasure {
    PEARSON, COSINE_SIM;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }

  }

  // data points
  // [I1 ; I2][data point]
  private double[][] data;
  private SimpleRegression reg;
  private double minX, maxX;
  // cosineSimilarity
  private double cosineSim = 0;

  /**
   * Extracts all data from all correlations
   * 
   * @param corr
   */
  public static CorrelationData create(Collection<CorrelationData> corr) {
    List<double[]> dat = new ArrayList<>();
    for (CorrelationData c : corr) {
      for (double[] d : c.getData())
        dat.add(d);
    }
    return create(dat);
  }

  public static CorrelationData create(List<double[]> dat) {
    CorrelationData c = new CorrelationData();
    c.reg = new SimpleRegression();
    c.data = new double[dat.size()][2];
    c.minX = Double.NEGATIVE_INFINITY;
    c.maxX = Double.POSITIVE_INFINITY;
    for (int i = 0; i < dat.size(); i++) {
      c.data[i][0] = dat.get(i)[0];
      c.data[i][1] = dat.get(i)[1];
      c.minX = Math.min(c.minX, c.data[i][0]);
      c.maxX = Math.max(c.maxX, c.data[i][0]);
    }
    c.reg.addData(c.data);
    // calc cosineSim
    c.cosineSim = Similarity.COSINE.calc(c.data);
    return c;
  }

  public SimpleRegression getReg() {
    return reg;
  }

  public void setReg(SimpleRegression reg) {
    this.reg = reg;
  }

  public int getDPCount() {
    return reg == null ? 0 : (int) reg.getN();
  }

  /**
   * Pearson correlation
   * 
   * @return
   */
  public double getR() {
    return reg == null ? 0 : reg.getR();
  }

  /**
   * Cosine similarity
   * 
   * @return
   */
  public double getCosineSimilarity() {
    return cosineSim;
  }

  public double getSimilarity(SimilarityMeasure type) {
    switch (type) {
      case COSINE_SIM:
        return getCosineSimilarity();
      case PEARSON:
        return getR();
    }
    return 0;
  }

  public double getMinX() {
    return minX;
  }

  public void setMinX(double minX) {
    this.minX = minX;
  }

  public double getMaxX() {
    return maxX;
  }

  public void setMaxX(double maxX) {
    this.maxX = maxX;
  }

  public double[][] getData() {
    return data;
  }

  public boolean isValid() {
    return getDPCount() > 0;
  }

  /**
   *
   * @return X (intensity of row)
   */
  public double getX(int i) {
    return data == null ? 0 : data[i][0];
  }

  /**
   *
   * @return Y (intensity of compared row)
   */
  public double getY(int i) {
    return data == null ? 0 : data[i][1];
  }
}
