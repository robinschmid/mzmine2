package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure;

import org.apache.commons.math.stat.regression.SimpleRegression;

import net.sf.mzmine.datamodel.DataPoint;

/**
 * correlation of two peak shapes
 * @author RibRob
 *
 */
public class FeatureShapeCorrelationData {
	
	// data points
	// [I1 ; I2][data point]
	private Double[][] data;
	private SimpleRegression reg;
	private double minX, maxX;
	
	public SimpleRegression getReg() { 
		return reg;
	}
	public void setReg(SimpleRegression reg) {
		this.reg = reg;
	} 
	public FeatureShapeCorrelationData(SimpleRegression reg, Double[][] data, double minX, double maxX) {
		super();
		this.reg = reg;
		this.minX = minX;
		this.maxX = maxX;
		this.data = data;
	}
	public int getDPCount() {
		return reg==null? 0 : (int)reg.getN();
	}  
	public double getR() {
		return reg==null? 0 : reg.getR();
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
	public Double[][] getData() {
		return data;
	} 
	/**
	 * 
	 * @return X (intensity of row)
	 */
	public Double[] getX() {
		return data==null? new Double[0] : data[0];
	}
	/**
	 * 
	 * @return Y (intensity of compared row)
	 */
	public Double[] getY() {
		return data==null? new Double[0] : data[1];
	}
}
