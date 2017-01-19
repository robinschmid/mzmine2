package net.sf.mzmine.util.chartthemes;

import java.text.DecimalFormat;
import java.util.Iterator;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.CombinedDomainCategoryPlot;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.CombinedRangeCategoryPlot;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.ui.RectangleInsets;

public class FontChartTheme extends StandardChartTheme{

	protected boolean showXGrid = false, showYGrid = false, noaxisinsets = true;
	protected DecimalFormat format;
	/**
	 * smallFont
	 */
	private static final long serialVersionUID = 1L;


	public FontChartTheme(String name, boolean shadow) {
		super(name, shadow);
		format = new DecimalFormat("#.#");
	}
    
	@Override
	public void apply(JFreeChart chart) { 
		super.apply(chart); 
		if(XYPlot.class.isInstance(chart.getPlot())) {
			XYPlot plot = chart.getXYPlot();
			plot.setDomainGridlinesVisible(showXGrid);
			plot.getDomainAxis().setTickLabelInsets(new RectangleInsets(4, 0, 0, 0));
			plot.getDomainAxis().setTickMarkOutsideLength(3);
			((NumberAxis) plot.getDomainAxis()).setNumberFormatOverride(format);
			plot.setRangeGridlinesVisible(showYGrid);
			plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		}
		else if(CategoryPlot.class.isInstance(chart.getPlot())) {
			// spacing
			CategoryPlot plot = chart.getCategoryPlot();
			plot.setDomainGridlinesVisible(showXGrid);
			plot.setRangeGridlinesVisible(showYGrid);
			plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
		}
	}
	
    /**
     * Applies the attributes of this theme to a {@link CategoryPlot}.
     *
     * @param plot  the plot (<code>null</code> not permitted).
     */
    protected void applyToCategoryPlot(CategoryPlot plot) { 

        // process all domain axes
        int domainAxisCount = plot.getDomainAxisCount();
        for (int i = 0; i < domainAxisCount; i++) {
            CategoryAxis axis = plot.getDomainAxis(i);
            if (axis != null) {
                applyToCategoryAxis(axis);
            }
        }

        // process all range axes
        int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            ValueAxis axis = plot.getRangeAxis(i);
            if (axis != null) {
                applyToValueAxis(axis);
            }
        } 

        if (plot instanceof CombinedDomainCategoryPlot) {
            CombinedDomainCategoryPlot cp = (CombinedDomainCategoryPlot) plot;
            Iterator iterator = cp.getSubplots().iterator();
            while (iterator.hasNext()) {
                CategoryPlot subplot = (CategoryPlot) iterator.next();
                if (subplot != null) {
                    applyToPlot(subplot);
                }
            }
        }
        if (plot instanceof CombinedRangeCategoryPlot) {
            CombinedRangeCategoryPlot cp = (CombinedRangeCategoryPlot) plot;
            Iterator iterator = cp.getSubplots().iterator();
            while (iterator.hasNext()) {
                CategoryPlot subplot = (CategoryPlot) iterator.next();
                if (subplot != null) {
                    applyToPlot(subplot);
                }
            }
        }
    }
	
	@Override
	protected void applyToXYPlot(XYPlot plot) {
		// process all domain axes
        int domainAxisCount = plot.getDomainAxisCount();
        for (int i = 0; i < domainAxisCount; i++) {
            ValueAxis axis = plot.getDomainAxis(i);
            if (axis != null) {
                applyToValueAxis(axis);
            }
        }

        // process all range axes
        int rangeAxisCount = plot.getRangeAxisCount();
        for (int i = 0; i < rangeAxisCount; i++) {
            ValueAxis axis = plot.getRangeAxis(i);
            if (axis != null) {
                applyToValueAxis(axis);
            }
        } 

        // process all annotations
        Iterator iter = plot.getAnnotations().iterator();
        while (iter.hasNext()) {
            XYAnnotation a = (XYAnnotation) iter.next();
            applyToXYAnnotation(a);
        }

        if (plot instanceof CombinedDomainXYPlot) {
            CombinedDomainXYPlot cp = (CombinedDomainXYPlot) plot;
            Iterator iterator = cp.getSubplots().iterator();
            while (iterator.hasNext()) {
                XYPlot subplot = (XYPlot) iterator.next();
                if (subplot != null) {
                    applyToPlot(subplot);
                }
            }
        }
        if (plot instanceof CombinedRangeXYPlot) {
            CombinedRangeXYPlot cp = (CombinedRangeXYPlot) plot;
            Iterator iterator = cp.getSubplots().iterator();
            while (iterator.hasNext()) {
                XYPlot subplot = (XYPlot) iterator.next();
                if (subplot != null) {
                    applyToPlot(subplot);
                }
            }
        }
	}
}
