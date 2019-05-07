package net.sf.mzmine.util.chartthemes;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.title.PaintScaleLegend;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.mzmine.util.chartthemes.ChartThemeFactory.THEME;
 

public class MyStandardChartTheme extends StandardChartTheme {
	
// master font
	protected Font masterFont;
	protected Color masterFontColor;
	
	// ShortTitle
	protected Font fontShortTitle;
	protected Color cShortTitle, cBGShortTitle;
	
	// Chart appearance 
	protected boolean isAntiAliased = true;
	// orientation : 0 - 2 (90� CW)
	protected int chartOrientation;
	
	protected boolean isShowTitle = false;
	
	
	
	public static final String XML_DESC = "ChartTheme";
	protected Paint axisLinePaint = Color.black;
	protected THEME themeID;

	protected boolean showXGrid = false, showYGrid = false;
	protected boolean showXAxis= true, showYAxis = true;
	

	public MyStandardChartTheme(THEME themeID, String name) {
		super(name);
		this.themeID = themeID;

		// in theme
		setAntiAliased(false);
		setNoBackground(false);
		// general
		
		// Short title in plot
		fontShortTitle = new Font("Arial", Font.BOLD, 14);
		cShortTitle = Color.WHITE;
		cBGShortTitle = new Color(60,120,155, 120);
		
		isAntiAliased = true;
		chartOrientation = 0;
		
		masterFont = new Font("Arial", Font.PLAIN, 11);
		masterFontColor = Color.black;
	}
	
	public void setAll(boolean antiAlias, boolean showTitle, boolean noBG, Color cBG, Color cPlotBG, boolean showXGrid, boolean showYGrid, boolean showXAxis, boolean showYAxis, 
			Font fMaster, Color cMaster, Font fAxesT, Color cAxesT, Font fAxesL, Color cAxesL, Font fTitle, Color cTitle) {
		this.setAntiAliased(antiAlias);
		this.setShowTitle(showTitle);
		this.setNoBackground(noBG);
		this.setShowXGrid(showXGrid);
		this.setShowYGrid(showYGrid);
		this.setShowXAxis(showXAxis);
		this.setShowYAxis(showYAxis);
		//
		
		this.setExtraLargeFont(fTitle);
		this.setLargeFont(fAxesT);
		this.setRegularFont(fAxesL);
		this.setAxisLabelPaint(cAxesT);
		this.setTickLabelPaint(cAxesL);
		this.setTitlePaint(cTitle);
		
		this.setChartBackgroundPaint(cBG);
		this.setPlotBackgroundPaint(cPlotBG);
		this.setLegendBackgroundPaint(cBG);
		
		masterFont = fMaster;
		masterFontColor = cMaster;
	}
	

	@Override
	public void apply(JFreeChart chart) {
		// TODO Auto-generated method stub
		super.apply(chart);
		//
		chart.getXYPlot().setDomainGridlinesVisible(showXGrid);
		chart.getXYPlot().setRangeGridlinesVisible(showYGrid);
		// all axes
		for(int i=0; i<chart.getXYPlot().getDomainAxisCount(); i++) {
			NumberAxis a = (NumberAxis) chart.getXYPlot().getDomainAxis(i);
			a.setTickMarkPaint(axisLinePaint);
			a.setAxisLinePaint(axisLinePaint);
			// visible?
			a.setVisible(showXAxis);
		}
		for(int i=0; i<chart.getXYPlot().getRangeAxisCount(); i++) {
			NumberAxis a = (NumberAxis) chart.getXYPlot().getRangeAxis(i);
			a.setTickMarkPaint(axisLinePaint);
			a.setAxisLinePaint(axisLinePaint);
			// visible?
			a.setVisible(showYAxis);
		}
		// apply bg 
        chart.setBackgroundPaint(this.getChartBackgroundPaint());
        chart.getPlot().setBackgroundPaint(this.getPlotBackgroundPaint());
        
        for(int i=0; i<chart.getSubtitleCount(); i++) 
        	if(PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass())) 
        		((PaintScaleLegend)chart.getSubtitle(i)).setBackgroundPaint(this.getChartBackgroundPaint());
        if(chart.getLegend()!=null)
        	chart.getLegend().setBackgroundPaint(this.getChartBackgroundPaint());
        
		//
		chart.setAntiAlias(isAntiAliased());
		chart.getTitle().setVisible(isShowTitle()); 
        chart.getPlot().setBackgroundAlpha(isNoBackground()?0:1);
	}


	public void setShortTitle(Color c, Color bg, Font font) {
		cShortTitle = c;
		cBGShortTitle = bg;
		fontShortTitle = font;
	}

	//#########################################################################
	// xml import export
	public void appendThemeSettingsToXML(Element elParent, Document doc) {
		Element el = doc.createElement(XML_DESC);
		elParent.appendChild(el);

		Settings.toXML(el, doc, "axisLinePaint", axisLinePaint); 
		Settings.toXML(el, doc, "themeID", themeID); 
		Settings.toXML(el, doc, "showXGrid", showXGrid); 
		Settings.toXML(el, doc, "showYGrid",showYGrid); 
		Settings.toXML(el, doc, "showXAxis", showXAxis ); 
		Settings.toXML(el, doc, "showYAxis", showYAxis); 
		Settings.toXML(el, doc, "isAntiAliased", isAntiAliased); 
		Settings.toXML(el, doc, "showTitle", isShowTitle); 
		Settings.toXML(el, doc, "noBackground", isNoBackground()); 
		
		Settings.toXML(el, doc, "fontShortTitle", fontShortTitle); 
		Settings.toXML(el, doc, "cShortTitle", cShortTitle); 
		Settings.toXML(el, doc, "cBGShortTitle", cBGShortTitle); 
		Settings.toXML(el, doc, "cBackground", getChartBackgroundPaint());
		Settings.toXML(el, doc, "cPlotBackground", getPlotBackgroundPaint());
		
		// fonts
		Settings.toXML(el, doc, "smallFont", getSmallFont()); 
		Settings.toXML(el, doc, "regularFont", getRegularFont()); 
		Settings.toXML(el, doc, "largeFont", getLargeFont()); 
		Settings.toXML(el, doc, "xxlFont", getExtraLargeFont()); 
		
		Settings.toXML(el, doc, "cAxis", getAxisLabelPaint()); 
		Settings.toXML(el, doc, "cItem", getItemLabelPaint()); 
		Settings.toXML(el, doc, "cTitle", getTitlePaint()); 
		Settings.toXML(el, doc, "cTick", getTickLabelPaint()); 
	}

	public void loadValuesFromXML(Element el, Document doc) {
		boolean hasNoBG = false;
		NodeList list = el.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
				Element nextElement = (Element) list.item(i);
				String paramName = nextElement.getNodeName();
				if(paramName.equals("axisLinePaint")) axisLinePaint = Settings.colorFromXML(nextElement); 
				else if(paramName.equals("themeID"))themeID = THEME.valueOf(nextElement.getTextContent());  
				else if(paramName.equals("showXGrid"))showXGrid = Settings.booleanFromXML(nextElement);  
				else if(paramName.equals("showYGrid"))showYGrid = Settings.booleanFromXML(nextElement);  
				else if(paramName.equals("showXAxis"))showXAxis = Settings.booleanFromXML(nextElement);  
				else if(paramName.equals("showYAxis"))showYAxis = Settings.booleanFromXML(nextElement);  
				else  if(paramName.equals("isAntiAliased")) isAntiAliased = Settings.booleanFromXML(nextElement); 
					else if(paramName.equals("showTitle"))isShowTitle = Settings.booleanFromXML(nextElement);  
					else if(paramName.equals("noBackground")){
						hasNoBG = Settings.booleanFromXML(nextElement);
						setNoBackground(hasNoBG);  
					}
					else if(paramName.equals("fontShortTitle"))fontShortTitle = Settings.fontFromXML(nextElement);  
					else if(paramName.equals("cShortTitle")) cShortTitle = Settings.colorFromXML(nextElement);  
					else if(paramName.equals("cBGShortTitle")) cBGShortTitle = Settings.colorFromXML(nextElement);    
					else if(paramName.equals("cBackground")) {
						Color c = Settings.colorFromXML(nextElement);
						setChartBackgroundPaint(c);  
						setLegendBackgroundPaint(c);
						if(hasNoBG)
							setNoBackground(hasNoBG);
					}
					else if(paramName.equals("cPlotBackground")) {
						Color c = Settings.colorFromXML(nextElement);
						setPlotBackgroundPaint(c);  
						if(hasNoBG)
							setNoBackground(hasNoBG);
					}
					else if(paramName.equals("cAxis")) setAxisLabelPaint(Settings.colorFromXML(nextElement));   
					else if(paramName.equals("cItem")) setItemLabelPaint(Settings.colorFromXML(nextElement));   
					else if(paramName.equals("cTitle")) setTitlePaint(Settings.colorFromXML(nextElement));   
					else if(paramName.equals("cTick")) setTickLabelPaint(Settings.colorFromXML(nextElement));   
				// fonts
					else if(paramName.equals("smallFont")) setSmallFont(Settings.fontFromXML(nextElement)); 
					else if(paramName.equals("regularFont")) setRegularFont(Settings.fontFromXML(nextElement));  
					else if(paramName.equals("largeFont")) setLargeFont(Settings.fontFromXML(nextElement));  
					else if(paramName.equals("xxlFont")) setExtraLargeFont(Settings.fontFromXML(nextElement));   
			}
		}
	}

	public boolean isNoBackground() { 
		return ((Color)this.getPlotBackgroundPaint()).getAlpha() == 0;
	}
	public void setNoBackground(boolean state) { 
		Color c = ((Color)this.getPlotBackgroundPaint());
		Color cchart = ((Color)this.getChartBackgroundPaint());
		this.setPlotBackgroundPaint(new Color(c.getRed(), c.getGreen(), c.getBlue(), state? 0 : 255));
		this.setChartBackgroundPaint(new Color(cchart.getRed(), cchart.getGreen(), cchart.getBlue(), state? 0 : 255));
		this.setLegendBackgroundPaint(new Color(cchart.getRed(), cchart.getGreen(), cchart.getBlue(), state? 0 : 255));
	}
	
	// GETTERS AND SETTERS
	public Paint getAxisLinePaint() {
		return axisLinePaint;
	}
	public boolean isShowTitle() {
		return isShowTitle;
	}

	public boolean isAntiAliased() {
		return isAntiAliased;
	}

	public void setAntiAliased(boolean isAntiAliased) {
		this.isAntiAliased = isAntiAliased;
	}

	public int getChartOrientation() {
		return chartOrientation;
	}

	public void setChartOrientation(int chartOrientation) {
		this.chartOrientation = chartOrientation;
	}
	public Font getFontShortTitle() {
		return fontShortTitle;
	}
	public Color getcShortTitle() {
		return cShortTitle;
	}
	public void setFontShortTitle(Font fontShortTitle) {
		this.fontShortTitle = fontShortTitle;
	}
	public void setcShortTitle(Color cShortTitle) {
		this.cShortTitle = cShortTitle;
	}
	public Color getcBGShortTitle() {
		return cBGShortTitle;
	}
	public void setcBGShortTitle(Color cBGShortTitle) {
		this.cBGShortTitle = cBGShortTitle;
	}

	public void setShowTitle(boolean showTitle) {
		isShowTitle = showTitle;
	}

	public void setAxisLinePaint(Paint axisLinePaint) {
		this.axisLinePaint = axisLinePaint;
	}
	public THEME getID() { 
		return themeID;
	}
	public void setID(THEME themeID) {
		this.themeID = themeID;
	} 
	public void setShowXGrid(boolean showXGrid) {
		this.showXGrid = showXGrid;
	}
	public void setShowYGrid(boolean showYGrid) {
		this.showYGrid = showYGrid;
	} 
	public boolean isShowXGrid() {
		return showXGrid;
	} 
	public boolean isShowYGrid() {
		return showYGrid;
	}

	public boolean isShowXAxis() {
		return showXAxis;
	}

	public void setShowXAxis(boolean showXAxis) {
		this.showXAxis = showXAxis;
	}

	public boolean isShowYAxis() {
		return showYAxis;
	}

	public void setShowYAxis(boolean showYAxis) {
		this.showYAxis = showYAxis;
	} 
	public Font getMasterFont() {
		return masterFont;
	}

	public Color getMasterFontColor() {
		return masterFontColor;
	}

	public void setMasterFont(Font masterFont) {
		this.masterFont = masterFont;
	}

	public void setMasterFontColor(Color masterFontColor) {
		this.masterFontColor = masterFontColor;
	}
}
