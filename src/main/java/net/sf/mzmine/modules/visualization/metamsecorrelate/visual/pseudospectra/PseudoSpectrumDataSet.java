package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.pseudospectra;

import java.util.HashMap;
import java.util.Map;

import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class PseudoSpectrumDataSet extends XYSeriesCollection {
	private static final long serialVersionUID = 1L;

	private Map<XYDataItem, String> annotation;

	public PseudoSpectrumDataSet(Comparable key, boolean autoSort) {
		super(new XYSeries(key, autoSort));
	}
	public void addDP(double x, double y, String ann) {
		XYDataItem dp = new XYDataItem(x, y);
		getSeries(0).add(dp); 
		if(ann!=null) {
			if(annotation==null) this.annotation = new HashMap<XYDataItem, String>();
			annotation.put(dp, ann);
		}
	}
	
	public String getAnnotation(int item) {
		if (annotation == null)
			return null;
		XYDataItem itemDataPoint = getSeries(0).getDataItem(item);
		for (XYDataItem key : annotation.keySet()) {
			if (Math.abs(key.getXValue() - itemDataPoint.getXValue()) < 0.0001)
				return annotation.get(key);
		}
		return null;
	}
}
