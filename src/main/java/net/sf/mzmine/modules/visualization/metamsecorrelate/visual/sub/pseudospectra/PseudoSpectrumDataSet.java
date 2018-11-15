package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.sub.pseudospectra;

import java.util.HashMap;
import java.util.Map;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSDataPointIdentity;
import net.sf.mzmine.datamodel.identities.ms2.interf.AbstractMSMSIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class PseudoSpectrumDataSet extends XYSeriesCollection {
  private static final long serialVersionUID = 1L;

  private Map<XYDataItem, String> annotation;

  public PseudoSpectrumDataSet(Comparable key, boolean autoSort) {
    super(new XYSeries(key, autoSort));
  }

  public void addDP(double x, double y, String ann) {
    XYDataItem dp = new XYDataItem(x, y);
    getSeries(0).add(dp);
    if (ann != null) {
      if (annotation == null)
        this.annotation = new HashMap<XYDataItem, String>();
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

  public void addIdentity(MZTolerance mzTolerance, AbstractMSMSIdentity ann) {
    if (ann instanceof AbstractMSMSDataPointIdentity)
      addDPIdentity(mzTolerance, (AbstractMSMSDataPointIdentity) ann);
    // TODO add diff identity
  }

  private void addDPIdentity(MZTolerance mzTolerance, AbstractMSMSDataPointIdentity ann) {
    XYSeries series = getSeries(0);
    for (int i = 0; i < series.getItemCount(); i++) {
      XYDataItem dp = series.getDataItem(i);
      if (mzTolerance.checkWithinTolerance(dp.getXValue(), ann.getMZ())) {
        if (annotation == null)
          this.annotation = new HashMap<XYDataItem, String>();
        annotation.put(dp, ann.getName());
      }
    }
  }
}
