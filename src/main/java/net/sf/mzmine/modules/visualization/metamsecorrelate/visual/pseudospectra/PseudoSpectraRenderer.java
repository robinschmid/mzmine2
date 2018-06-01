/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.metamsecorrelate.visual.pseudospectra;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.text.TextUtils;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.modules.visualization.spectra.renderers.SpectraToolTipGenerator;

public class PseudoSpectraRenderer extends XYBarRenderer {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static final float TRANSPARENCY = 0.8f;

  public static final AlphaComposite alphaComp =
      AlphaComposite.getInstance(AlphaComposite.SRC_OVER, TRANSPARENCY);

  private boolean isTransparent;

  public PseudoSpectraRenderer(Color color, boolean isTransparent) {

    this.isTransparent = isTransparent;

    // Set painting color
    setDefaultPaint(color);

    // Shadow makes fake peaks
    setShadowVisible(false);

    // Set the tooltip generator
    SpectraToolTipGenerator tooltipGenerator = new SpectraToolTipGenerator();
    setDefaultToolTipGenerator(tooltipGenerator);

    // We want to paint the peaks using simple color without any gradient
    // effects
    setBarPainter(new StandardXYBarPainter() {
      @Override
      public void paintBar(Graphics2D g2, XYBarRenderer renderer, int row, int column,
          RectangularShape bar, RectangleEdge base) {
        super.paintBar(g2, renderer, row, column, new Rectangle2D.Double(
            bar.getX() + (bar.getWidth() - 2) / 2, bar.getY(), 2, bar.getHeight()), base);
      }
    });
  }

  public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea,
      PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis,
      XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {

    if (isTransparent)
      g2.setComposite(alphaComp);

    super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis, dataset, series, item,
        crosshairState, pass);

  }

  @Override
  protected void drawItemLabel(Graphics2D g2, XYDataset dataset, int series, int item, XYPlot plot,
      XYItemLabelGenerator generator, Rectangle2D bar, boolean negative) {
    // super.drawItemLabel(g2, dataset, series, item, plot, generator, bar, negative);

    if (generator != null) {
      String label = generator.generateLabel(dataset, series, item);

      if (label != null) {
        Font labelFont = getItemLabelFont(series, item);
        Paint paint = getItemLabelPaint(series, item);
        g2.setFont(labelFont);
        g2.setPaint(paint);

        // get the label position..
        ItemLabelPosition position;
        if (!negative) {
          position = getPositiveItemLabelPosition(series, item);
        } else {
          position = getNegativeItemLabelPosition(series, item);
        }

        // work out the label anchor point...
        Point2D anchorPoint =
            calculateLabelAnchorPoint(position.getItemLabelAnchor(), bar, plot.getOrientation());

        // split by \n
        String symbol = "\n";
        String[] splitted = label.split(symbol);

        if (splitted.length > 1) {
          FontRenderContext frc = g2.getFontRenderContext();
          GlyphVector gv = g2.getFont().createGlyphVector(frc, "Fg,");
          int height = 4 + (int) gv.getPixelBounds(null, 0, 0).getHeight();
          // draw more than one row
          for (int i = 0; i < splitted.length; i++) {
            int offset = -height * (splitted.length - i - 1);

            TextUtils.drawRotatedString(splitted[i], g2, (float) anchorPoint.getX(),
                (float) anchorPoint.getY() + offset, position.getTextAnchor(), position.getAngle(),
                position.getRotationAnchor());
          }
        } else {
          // one row
          TextUtils.drawRotatedString(label, g2, (float) anchorPoint.getX(),
              (float) anchorPoint.getY(), position.getTextAnchor(), position.getAngle(),
              position.getRotationAnchor());
        }
      }
    }

  }

  /**
   * Calculates the item label anchor point.
   *
   * @param anchor the anchor.
   * @param bar the bar.
   * @param orientation the plot orientation.
   *
   * @return The anchor point.
   */
  private Point2D calculateLabelAnchorPoint(ItemLabelAnchor anchor, Rectangle2D bar,
      PlotOrientation orientation) {

    Point2D result = null;
    double offset = getItemLabelAnchorOffset();
    double x0 = bar.getX() - offset;
    double x1 = bar.getX();
    double x2 = bar.getX() + offset;
    double x3 = bar.getCenterX();
    double x4 = bar.getMaxX() - offset;
    double x5 = bar.getMaxX();
    double x6 = bar.getMaxX() + offset;

    double y0 = bar.getMaxY() + offset;
    double y1 = bar.getMaxY();
    double y2 = bar.getMaxY() - offset;
    double y3 = bar.getCenterY();
    double y4 = bar.getMinY() + offset;
    double y5 = bar.getMinY();
    double y6 = bar.getMinY() - offset;

    if (anchor == ItemLabelAnchor.CENTER) {
      result = new Point2D.Double(x3, y3);
    } else if (anchor == ItemLabelAnchor.INSIDE1) {
      result = new Point2D.Double(x4, y4);
    } else if (anchor == ItemLabelAnchor.INSIDE2) {
      result = new Point2D.Double(x4, y4);
    } else if (anchor == ItemLabelAnchor.INSIDE3) {
      result = new Point2D.Double(x4, y3);
    } else if (anchor == ItemLabelAnchor.INSIDE4) {
      result = new Point2D.Double(x4, y2);
    } else if (anchor == ItemLabelAnchor.INSIDE5) {
      result = new Point2D.Double(x4, y2);
    } else if (anchor == ItemLabelAnchor.INSIDE6) {
      result = new Point2D.Double(x3, y2);
    } else if (anchor == ItemLabelAnchor.INSIDE7) {
      result = new Point2D.Double(x2, y2);
    } else if (anchor == ItemLabelAnchor.INSIDE8) {
      result = new Point2D.Double(x2, y2);
    } else if (anchor == ItemLabelAnchor.INSIDE9) {
      result = new Point2D.Double(x2, y3);
    } else if (anchor == ItemLabelAnchor.INSIDE10) {
      result = new Point2D.Double(x2, y4);
    } else if (anchor == ItemLabelAnchor.INSIDE11) {
      result = new Point2D.Double(x2, y4);
    } else if (anchor == ItemLabelAnchor.INSIDE12) {
      result = new Point2D.Double(x3, y4);
    } else if (anchor == ItemLabelAnchor.OUTSIDE1) {
      result = new Point2D.Double(x5, y6);
    } else if (anchor == ItemLabelAnchor.OUTSIDE2) {
      result = new Point2D.Double(x6, y5);
    } else if (anchor == ItemLabelAnchor.OUTSIDE3) {
      result = new Point2D.Double(x6, y3);
    } else if (anchor == ItemLabelAnchor.OUTSIDE4) {
      result = new Point2D.Double(x6, y1);
    } else if (anchor == ItemLabelAnchor.OUTSIDE5) {
      result = new Point2D.Double(x5, y0);
    } else if (anchor == ItemLabelAnchor.OUTSIDE6) {
      result = new Point2D.Double(x3, y0);
    } else if (anchor == ItemLabelAnchor.OUTSIDE7) {
      result = new Point2D.Double(x1, y0);
    } else if (anchor == ItemLabelAnchor.OUTSIDE8) {
      result = new Point2D.Double(x0, y1);
    } else if (anchor == ItemLabelAnchor.OUTSIDE9) {
      result = new Point2D.Double(x0, y3);
    } else if (anchor == ItemLabelAnchor.OUTSIDE10) {
      result = new Point2D.Double(x0, y5);
    } else if (anchor == ItemLabelAnchor.OUTSIDE11) {
      result = new Point2D.Double(x1, y6);
    } else if (anchor == ItemLabelAnchor.OUTSIDE12) {
      result = new Point2D.Double(x3, y6);
    }

    return result;

  }

  /**
   * This method returns null, because we don't want to change the colors dynamically.
   */
  public DrawingSupplier getDrawingSupplier() {
    return null;
  }

}
