package net.sf.mzmine.util.chartexport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.NumberFormat;
import javax.swing.JMenuItem;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.encoders.EncoderUtil;
import org.jfree.chart.encoders.ImageFormat;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.util.Args;
import org.w3c.dom.DOMImplementation;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import net.sf.mzmine.util.chartexport.window.GraphicsExportDialog;
import net.sf.mzmine.util.chartthemes.ChartThemeFactory;
import net.sf.mzmine.util.chartthemes.MyStandardChartTheme;

/**
 * only export of charts to images
 * 
 * @author vukmir69
 *
 */
public class ChartExportUtil {
  // ######################################################################################
  // VECTORS: PDF uses ITextpdf lib
  public static MyStandardChartTheme theme;

  /**
   * takes Only Width in account
   * 
   * @param chart
   * @param sett
   * @throws Exception
   */
  public static void writeChartToImage(ChartPanel chart, SettingsExportGraphics sett)
      throws Exception {
    // Size only by width?
    if (sett.isUseOnlyWidth()) {
      sett.setHeight(ChartLogics.calcHeightToWidth(chart, sett.getSize().getWidth()));
    }
    writeChartToImage(chart.getChart(), sett);
  }

  /**
   * This method is used to save all image formats. it uses the specific methods for each file
   * format
   * 
   * @param chart
   * @param sett
   */
  private static void writeChartToImage(JFreeChart chart, SettingsExportGraphics sett)
      throws Exception {
    // Background color
    Paint saved = chart.getBackgroundPaint();
    chart.setBackgroundPaint(sett.getColorBackground());
    chart.setBackgroundImageAlpha(sett.getColorBackground().getAlpha());
    if (chart.getLegend() != null)
      chart.getLegend().setBackgroundPaint(sett.getColorBackground());
    // legends and stuff
    for (int i = 0; i < chart.getSubtitleCount(); i++)
      if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
        ((PaintScaleLegend) chart.getSubtitle(i)).setBackgroundPaint(sett.getColorBackground());

    // apply bg
    chart.getPlot().setBackgroundPaint(sett.getColorBackground());

    // create folder
    File f = sett.getFullFilePath();
    if (!f.exists()) {
      if (f.getParentFile() != null)
        f.getParentFile().mkdirs();
      // f.createNewFile();
    }

    applyTheme(chart);

    // Format
    switch (sett.getFormat()) {
      case SettingsExportGraphics.FORMAT_PDF:
        writeChartToPDF(chart, sett.getSize().width, sett.getSize().height, f);
        break;
      case SettingsExportGraphics.FORMAT_PNG:
        writeChartToPNG(chart, sett.getSize().width, sett.getSize().height, f,
            sett.getResolution());
        break;
      case SettingsExportGraphics.FORMAT_JPG:
        writeChartToJPEG(chart, sett.getSize().width, sett.getSize().height, f,
            sett.getResolution());
        break;
      case SettingsExportGraphics.FORMAT_EPS:
        writeChartToEPS(chart, sett.getSize().width, sett.getSize().height, f);
        break;
      case SettingsExportGraphics.FORMAT_SVG:
        writeChartToSVG(chart, sett.getSize().width, sett.getSize().height, f);
        break;
    }
    //
    chart.setBackgroundPaint(saved);
    chart.setBackgroundImageAlpha(255);
    if (chart.getLegend() != null)
      chart.getLegend().setBackgroundPaint(saved);
    // legends and stuff
    for (int i = 0; i < chart.getSubtitleCount(); i++)
      if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
        ((PaintScaleLegend) chart.getSubtitle(i)).setBackgroundPaint(saved);

    // apply bg
    chart.getPlot().setBackgroundPaint(saved);
  }

  public static void applyTheme(JFreeChart chart) {
    // change font Arial 11
    if (theme == null) {
      theme = ChartThemeFactory.createBlackNWhiteTheme();
    }
    //
    NumberAxis domain = (NumberAxis) chart.getXYPlot().getDomainAxis();
    NumberAxis range = (NumberAxis) chart.getXYPlot().getRangeAxis();

    NumberFormat domainFormat = domain.getNumberFormatOverride();
    NumberFormat rangeFormat = range.getNumberFormatOverride();

    theme.apply(chart);

    // number format
    domain.setNumberFormatOverride(domainFormat);
    range.setNumberFormatOverride(rangeFormat);
  }

  /**
   * This method saves a chart as a PDF with given dimensions
   * 
   * @param chart
   * @param width
   * @param height
   * @param fileName is a full path
   */
  public static void writeChartToPDF(JFreeChart chart, int width, int height, File fileName)
      throws Exception {
    PdfWriter writer = null;

    Document document = new Document(new Rectangle(width, height));

    try {
      writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
      document.open();
      PdfContentByte contentByte = writer.getDirectContent();
      PdfTemplate template = contentByte.createTemplate(width, height);
      Graphics2D graphics2d = template.createGraphics(width, height, new DefaultFontMapper());
      Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);

      chart.draw(graphics2d, rectangle2d);

      graphics2d.dispose();
      contentByte.addTemplate(template, 0, 0);

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    } finally {
      document.close();
    }
  }

  public static void writeChartToPDF(JFreeChart chart, int width, int height, File path,
      String fileName) throws Exception {
    writeChartToPDF(chart, width, height, FileAndPathUtil.getRealFilePath(path, fileName, ".pdf"));
  }

  // ######################################################################################
  // PIXELS: JPG PNG
  public static void writeChartToPNG(JFreeChart chart, int width, int height, File fileName)
      throws IOException {
    ChartUtils.saveChartAsPNG(fileName, chart, width, height);
  }

  public static void writeChartToPNG(JFreeChart chart, int width, int height, File fileName,
      int resolution) throws IOException {
    if (resolution == 72)
      writeChartToPNG(chart, width, height, fileName);
    else {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
      try {
        BufferedImage image = paintScaledChartToBufferedImage(chart, out, width, height, resolution,
            BufferedImage.TYPE_INT_ARGB);
        out.write(ChartUtils.encodeAsPNG(image));
      } finally {
        out.close();
      }
    }
  }

  public static void writeChartToJPEG(JFreeChart chart, int width, int height, File fileName)
      throws IOException {
    ChartUtils.saveChartAsJPEG(fileName, chart, width, height);
  }

  public static void writeChartToJPEG(JFreeChart chart, int width, int height, File fileName,
      int resolution) throws IOException {
    // Background color
    Paint saved = chart.getBackgroundPaint();
    if (((Color) saved).getAlpha() == 0) {
      chart.setBackgroundPaint(Color.WHITE);
      chart.setBackgroundImageAlpha(255);
      if (chart.getLegend() != null)
        chart.getLegend().setBackgroundPaint(Color.WHITE);
      // legends and stuff
      for (int i = 0; i < chart.getSubtitleCount(); i++)
        if (PaintScaleLegend.class.isAssignableFrom(chart.getSubtitle(i).getClass()))
          ((PaintScaleLegend) chart.getSubtitle(i)).setBackgroundPaint(Color.WHITE);

      // apply bg
      chart.getPlot().setBackgroundPaint(Color.WHITE);
    }
    //
    if (resolution == 72)
      writeChartToJPEG(chart, width, height, fileName);
    else {
      OutputStream out = new BufferedOutputStream(new FileOutputStream(fileName));
      try {
        BufferedImage image = paintScaledChartToBufferedImage(chart, out, width, height, resolution,
            BufferedImage.TYPE_INT_RGB);
        EncoderUtil.writeBufferedImage(image, ImageFormat.JPEG, out, 1.f);
      } finally {
        out.close();
      }
    }
  }


  /**
   * Paints a chart with scaling options
   * 
   * @param chart
   * @param out
   * @param width
   * @param height
   * @param resolution
   * @return BufferedImage of a given chart with scaling to resolution
   * @throws IOException
   */
  private static BufferedImage paintScaledChartToBufferedImage(JFreeChart chart, OutputStream out,
      int width, int height, int resolution, int bufferedIType) throws IOException {
    Args.nullNotPermitted(out, "out");
    Args.nullNotPermitted(chart, "chart");

    double scaleX = resolution / 72.0;
    double scaleY = resolution / 72.0;

    double desiredWidth = width * scaleX;
    double desiredHeight = height * scaleY;
    double defaultWidth = width;
    double defaultHeight = height;
    boolean scale = false;

    // get desired width and height from somewhere then...
    if ((scaleX != 1) || (scaleY != 1)) {
      scale = true;
    }

    BufferedImage image = new BufferedImage((int) desiredWidth, (int) desiredHeight, bufferedIType);
    Graphics2D g2 = image.createGraphics();

    if (scale) {
      AffineTransform saved = g2.getTransform();
      g2.transform(AffineTransform.getScaleInstance(scaleX, scaleY));
      chart.draw(g2, new Rectangle2D.Double(0, 0, defaultWidth, defaultHeight), null, null);
      g2.setTransform(saved);
      g2.dispose();
    } else {
      chart.draw(g2, new Rectangle2D.Double(0, 0, defaultWidth, defaultHeight), null, null);
    }
    return image;
  }

  // ######################################################################################
  // VECTORS: EPS uses EpsGraphics2D
  // SVG uses BATIK lib
  public static void writeChartToSVG(JFreeChart chart, int width, int height, File name)
      throws Exception {
    // Get a DOMImplementation
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    svgGenerator.setSVGCanvasSize(new Dimension(width, height));
    chart.draw(svgGenerator, new Rectangle2D.Double(0, 0, width, height));


    boolean useCSS = true; // we want to use CSS style attribute

    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(name), "UTF-8");
      svgGenerator.stream(out, useCSS);
    } catch (UnsupportedEncodingException | FileNotFoundException e) {
      e.printStackTrace();
      throw e;
    } catch (SVGGraphics2DIOException e) {
      e.printStackTrace();
      throw e;
    } finally {
      try {
        out.close();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        throw e;
      }
    }
  }


  public static void writeChartToEPS(JFreeChart chart, int width, int height, File name)
      throws IOException {
    EpsGraphics g;
    try {
      g = new EpsGraphics("EpsTools Drawable Export", new FileOutputStream(name), 0, 0, width,
          height, ColorMode.COLOR_RGB);
      Rectangle2D rectangle2d = new Rectangle2D.Double(0, 0, width, height);
      chart.draw((Graphics2D) g, rectangle2d);
      g.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      throw e;
    }
    /*
     * Writer out=new FileWriter(name); out.write(g.toString()); out.close();
     */
  }

  /*
   * ############################################################### Export Graphics
   */
  /**
   * adds an export graphics menu to a chart panel
   * 
   * @param chart
   */
  public static void addExportMenu(final ChartPanel chart) {
    chart.getPopupMenu().addSeparator();
    // Graphics Export
    JMenuItem exportGraphics = new JMenuItem("Export Graphics");
    exportGraphics.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // open export Graphics dialog
        GraphicsExportDialog.openInstDialog(chart.getChart());
      }
    });

    // add to panel
    chart.getPopupMenu().add(exportGraphics);
  }

}
