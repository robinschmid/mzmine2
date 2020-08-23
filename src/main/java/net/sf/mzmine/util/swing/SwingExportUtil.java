/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.util.swing;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.w3c.dom.DOMImplementation;
import com.itextpdf.awt.DefaultFontMapper;
import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;
import net.sf.mzmine.util.files.FileAndPathUtil;
import net.sf.mzmine.util.spectraldb.parser.UnsupportedFormatException;

/**
 * Export swing components to pdf, emf, eps
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SwingExportUtil {
  private static final Logger logger = Logger.getLogger(SwingExportUtil.class.getName());

  /**
   * 
   * @param panel
   * @param path
   * @param fileName
   * @param format without . (ALL, PDF, EMF, EPS, SVG). ALL = export all at once
   */
  public static void writeToGraphics(JComponent panel, File file, String format)
      throws IOException, DocumentException, UnsupportedFormatException {
    writeToGraphics(panel, file.getParentFile(), file.getName(), format);
  }

  public static void writeToGraphics(JComponent panel, File path, String fileName, String format)
      throws IOException, DocumentException, UnsupportedFormatException {
    switch (format.toLowerCase()) {
      case "all":
        writeToPDF(panel, path, fileName);
        writeToEMF(panel, path, fileName);
        writeToEPS(panel, path, fileName);
        writeToSVG(panel, path, fileName);
        break;
      case "pdf":
        writeToPDF(panel, path, fileName);
        break;
      case "emf":
        writeToEMF(panel, path, fileName);
        writeToEPS(panel, path, fileName);
        break;
      case "eps":
        writeToEPS(panel, path, fileName);
        break;
      case "svg":
        writeToSVG(panel, path, fileName);
        break;
      default:
        throw new UnsupportedFormatException("Format is not supported for image export: " + format);
    }
  }

  /**
   * 
   * @param panel
   * @param path
   * @param fileName
   * @param format without . (ALL, PDF, EMF, EPS, SVG). ALL = export all at once
   */
  public static void writeToGraphics(JComponent panel, File file, String format, int width,
      int height) throws IOException, DocumentException, UnsupportedFormatException {
    writeToGraphics(panel, file.getParentFile(), file.getName(), format, width, height);
  }

  public static void writeToGraphics(JComponent panel, File path, String fileName, String format,
      int width, int height) throws IOException, DocumentException, UnsupportedFormatException {
    switch (format.toLowerCase()) {
      case "all":
        writeToPDF(panel, path, fileName, width, height);
        writeToEMF(panel, path, fileName, width, height);
        writeToEPS(panel, path, fileName, width, height);
        writeToSVG(panel, path, fileName, width, height);
        break;
      case "pdf":
        writeToPDF(panel, path, fileName, width, height);
        break;
      case "emf":
        writeToEMF(panel, path, fileName, width, height);
        writeToEPS(panel, path, fileName, width, height);
        break;
      case "eps":
        writeToEPS(panel, path, fileName, width, height);
        break;
      case "svg":
        writeToSVG(panel, path, fileName, width, height);
        break;
      default:
        throw new UnsupportedFormatException("Format is not supported for image export: " + format);
    }
  }

  /**
   * Writes swing to pdf
   * 
   * @param panel
   * @param fileName
   * @throws DocumentException
   * @throws Exception
   */
  public static void writeToPDF(JComponent panel, File fileName)
      throws IOException, DocumentException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getHeight();
    writeToPDF(panel, fileName, width, height);
  }

  /**
   * 
   * @param panel
   * @param fileName
   * @param width
   * @param height
   * @throws IOException
   * @throws DocumentException
   */
  public static void writeToPDF(JComponent panel, File fileName, int width, int height)
      throws IOException, DocumentException {
    logger.info(
        () -> MessageFormat.format("Exporting panel to PDF file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));
    Document document = new Document(new Rectangle(width, height));
    PdfWriter writer = null;
    try {
      writer = PdfWriter.getInstance(document, new FileOutputStream(fileName));
      document.open();
      PdfContentByte contentByte = writer.getDirectContent();
      PdfTemplate template = contentByte.createTemplate(width, height);
      Graphics2D g2 = new PdfGraphics2D(contentByte, width, height, new DefaultFontMapper());
      panel.print(g2);
      g2.dispose();
      contentByte.addTemplate(template, 0, 0);
      writer.setPageEmpty(false);
      document.close();
      writer.close();
    } finally {
      if (document.isOpen()) {
        document.close();
      }
    }
  }

  /**
   * Writes swing to EPS
   * 
   * @param panel
   * @param fileName
   * @throws Exception
   */
  public static void writeToEPS(JComponent panel, File fileName) throws IOException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getHeight();
    writeToEPS(panel, fileName, width, height);
  }

  /**
   * 
   * @param panel
   * @param fileName
   * @param width
   * @param height
   * @throws IOException
   */
  public static void writeToEPS(JComponent panel, File fileName, int width, int height)
      throws IOException {
    logger.info(
        () -> MessageFormat.format("Exporting panel to EPS file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));
    EpsGraphics g;
    g = new EpsGraphics("EpsTools Drawable Export", new FileOutputStream(fileName), 0, 0, width,
        height, ColorMode.COLOR_RGB);
    panel.print(g);
    g.close();
  }

  /**
   * Writes swing to EMF
   * 
   * @param panel
   * @param fileName
   * @throws Exception
   */
  public static void writeToEMF(JComponent panel, File fileName) throws IOException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getHeight();
    writeToEMF(panel, fileName, width, height);
  }

  /**
   * 
   * @param panel
   * @param fileName
   * @throws IOException
   */
  public static void writeToEMF(JComponent panel, File fileName, int width, int height)
      throws IOException {
    logger.info(
        () -> MessageFormat.format("Exporting panel to EMF file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));

    VectorGraphics g = new EMFGraphics2D(fileName, new Dimension(width, height));
    g.startExport();
    panel.print(g);
    g.endExport();
  }

  public static void writeToSVG(JComponent panel, File fileName) throws IOException {
    // print the panel to pdf
    int width = panel.getWidth();
    int height = panel.getHeight();
    writeToSVG(panel, fileName, width, height);
  }

  /**
   * 
   * @param panel
   * @param fileName
   * @param width
   * @param height
   * @throws IOException
   */
  public static void writeToSVG(JComponent panel, File fileName, int width, int height)
      throws IOException {
    logger.info(
        () -> MessageFormat.format("Exporting panel to SVG file (width x height; {0} x {1}): {2}",
            width, height, fileName.getAbsolutePath()));

    // Get a DOMImplementation
    DOMImplementation domImpl = SVGDOMImplementation.getDOMImplementation();
    org.w3c.dom.Document document = domImpl.createDocument(null, "svg", null);
    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
    svgGenerator.setSVGCanvasSize(new Dimension(width, height));
    panel.print(svgGenerator);

    boolean useCSS = true; // we want to use CSS style attribute

    try (Writer out = new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8")) {
      svgGenerator.stream(out, useCSS);
    }
  }



  /**
   * Writes swing to pdf
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws Exception
   */
  public static void writeToPDF(JComponent panel, File path, String fileName)
      throws IOException, DocumentException {
    writeToPDF(panel, FileAndPathUtil.getRealFilePath(path, fileName, "pdf"));
  }

  public static void writeToPDF(JComponent panel, File path, String fileName, int width, int height)
      throws IOException, DocumentException {
    writeToPDF(panel, FileAndPathUtil.getRealFilePath(path, fileName, "pdf"), width, height);
  }

  /**
   * Writes swing to EPS
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws Exception
   */
  public static void writeToEPS(JComponent panel, File path, String fileName, int width, int height)
      throws IOException {
    writeToEPS(panel, FileAndPathUtil.getRealFilePath(path, fileName, "eps"), width, height);
  }

  public static void writeToEPS(JComponent panel, File path, String fileName) throws IOException {
    writeToEPS(panel, FileAndPathUtil.getRealFilePath(path, fileName, "eps"));
  }

  /**
   * Writes swing to EMF
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws Exception
   */
  public static void writeToEMF(JComponent panel, File path, String fileName, int width, int height)
      throws IOException {
    writeToEMF(panel, FileAndPathUtil.getRealFilePath(path, fileName, "emf"), width, height);
  }

  public static void writeToEMF(JComponent panel, File path, String fileName) throws IOException {
    writeToEMF(panel, FileAndPathUtil.getRealFilePath(path, fileName, "emf"));
  }

  /**
   * Writes swing to SVG (scalable vector graphics)
   * 
   * @param panel
   * @param path
   * @param fileName
   * @throws IOException
   */
  public static void writeToSVG(JComponent panel, File path, String fileName, int width, int height)
      throws IOException {
    writeToSVG(panel, FileAndPathUtil.getRealFilePath(path, fileName, "svg"), width, height);
  }

  public static void writeToSVG(JComponent panel, File path, String fileName) throws IOException {
    writeToSVG(panel, FileAndPathUtil.getRealFilePath(path, fileName, "svg"));
  }
}
