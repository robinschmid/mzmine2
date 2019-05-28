package net.sf.mzmine.util.color;

import java.awt.Color;
import java.util.List;
import org.jfree.chart.renderer.LookupPaintScale;
import org.jfree.chart.renderer.PaintScale;


public class PaintScaleGenerator {
  private static final Color transparent = new Color(0, 0, 0, 0);



  // old version PaintScales
  public static PaintScale generateStepPaintScale(double min, double max, double promin,
      double promax, Color cmin, Color cmax, int stepCount) {
    // Min ist pormin von max
    double realmin = max * promin;
    double realmax = max * promax;
    //
    LookupPaintScale paintScale = new LookupPaintScale(min, max, Color.lightGray);
    // Bei null den min Wert hinzuf�gen
    paintScale.add(0, cmin);
    //
    for (int i = 0; i < stepCount; i++) {
      double value = realmin + realmax / (stepCount - 1) * i;
      paintScale.add(value, interpolate(cmin, cmax, i / (stepCount - 1.0f)));
    }
    //
    paintScale.add(max, cmax);
    //
    return paintScale;
  }

  /**
   * 
   * @param min the absolute data minimum
   * @param max the absolute data maximum
   * @param realmin the paintscale minimum
   * @param realmax the paintscale maximum
   * @param list colors
   * @param isInverted invert colours
   * @param firstTransparent values < realmin are transparent
   * @param lastTransparent values > realmax are transparent
   * @param steps how many steps
   * @return
   */
  public static PaintScale generateGreyscale(double min, double max, double realmin, double realmax,
      boolean isInverted, boolean firstTransparent, boolean lastTransparent, int steps,
      boolean forLegend) {
    if ((max <= min) || (realmax <= realmin)) {
      // no real data
      return null;
    } else {
      // with minimum and maximum bounds
      double lower = min;
      double upper = max;
      LookupPaintScale paintScale;
      if (forLegend) {
        lower = realmin;
        upper = realmax;
        paintScale = new LookupPaintScale(lower, upper, Color.lightGray);
      } else
        paintScale =
            new LookupPaintScale(Double.NEGATIVE_INFINITY, Double.MAX_VALUE, Color.lightGray);
      Color c = null;
      if (firstTransparent)
        c = transparent;
      else {
        float i = isInverted ? 0 : 1;
        c = new Color(i, i, i);
      }
      paintScale.add(Double.NEGATIVE_INFINITY, c);
      paintScale.add(realmin - Double.MIN_VALUE, c);


      // add list
      for (int i = 1; i < steps; i++) {
        float p = i / (steps - 1.f);
        double v = (float) (realmin + (realmax - realmin) * p);
        if (!isInverted)
          p = 1.f - p;
        c = new Color(p, p, p);
        paintScale.add(v, c);
      }

      // add one point to the minimum value in dataset (Changed from 0-> min because can be <0)
      if (lastTransparent)
        c = transparent;
      else {
        float i = isInverted ? 1 : 0;
        c = new Color(i, i, i);
      }
      paintScale.add(realmax + Double.MIN_VALUE, c);
      paintScale.add(Double.MAX_VALUE, c);

      //
      return paintScale;
    }
  }


  /**
   * 
   * @param min the absolute data minimum
   * @param max the absolute data maximum
   * @param realmin the paintscale minimum
   * @param realmax the paintscale maximum
   * @param list colors
   * @param isInverted invert colours
   * @param firstTransparent values < realmin are transparent
   * @param lastTransparent values > realmax are transparent
   * @param steps how many steps
   * @return
   */
  public static PaintScale generateColorListPaintScale(double min, double max, double realmin,
      double realmax, List<Color> list, boolean isInverted, boolean firstTransparent,
      boolean lastTransparent, int steps, boolean forLegend) {
    if ((max <= min) || (realmax <= realmin)) {
      // no real data
      return null;
    } else {
      int size = list.size();
      // with minimum and maximum bounds
      double lower = min;
      double upper = max;
      LookupPaintScale paintScale;
      if (forLegend) {
        lower = realmin;
        upper = realmax;
        paintScale = new LookupPaintScale(lower, upper, Color.lightGray);
      } else
        paintScale =
            new LookupPaintScale(Double.NEGATIVE_INFINITY, Double.MAX_VALUE, Color.lightGray);

      Color c = null;
      if (firstTransparent)
        c = transparent;
      else {
        // non inverted
        int i = 0;
        if (isInverted)
          i = size - 1;
        c = list.get(i);
      }
      paintScale.add(Double.NEGATIVE_INFINITY, c);
      // paintScale.add(realmin - Double.MIN_VALUE, c);


      // add list
      int rsteps = Math.min(steps, size);
      for (int i = 0; i < rsteps; i++) {
        double v = realmin + (realmax - realmin) * i / (rsteps);
        c = list.get(isInverted ? size - 1 - i : i);
        paintScale.add(v, c);
      }

      // add one point to the minimum value in dataset (Changed from 0-> min because can be <0)
      if (lastTransparent)
        c = transparent;
      // paintScale.add(realmax + Double.MIN_VALUE, c);
      paintScale.add(Double.MAX_VALUE, c);

      //
      return paintScale;
    }
  }


  public static PaintScale generateMonochrome(Color color, double min, double max, boolean oneSided,
      int steps) {
    LookupPaintScale scale = new LookupPaintScale(min, max, Color.GRAY);
    scale.add(Double.NEGATIVE_INFINITY, Color.black);

    scale.add(min, color);
    for (int i = 0; i < steps; i++) {
      float p = (float) i / (steps);
      double v = (min + (max - min) * p);
      Color g = null;
      if (oneSided)
        g = interpolateMonochromOneSided(color, p, true);
      else
        g = interpolateMonochrom(color, p, 1, false);

      scale.add(v, g);
    }

    if (!oneSided) {
      scale.add(max, Color.white);
      scale.add(Double.POSITIVE_INFINITY, Color.white);
    } else {
      scale.add(max, color);
      scale.add(Double.POSITIVE_INFINITY, color);
    }
    return scale;
  }

  /**
   * interpolate with weights for specified hue values
   * 
   * @param start (real starting color
   * @param end (real ending color)
   * @param p
   * @param hue
   * @param position
   * @return
   */
  public static Color interpolateWeighted(Color start, Color end, float p, float[] hue,
      float[] position, boolean invertedPos) {
    float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
    float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

    float brightness = (startHSB[2] + endHSB[2]) / 2;
    float saturation = (startHSB[1] + endHSB[1]) / 2;

    float hueMin = startHSB[0];
    float hueMax = endHSB[0];

    float p0 = 0.f;
    float p1 = 1.f;

    // between which position?
    // start .... hue[0] .. hue[1] .. hue[n] ... end
    if (position != null && position.length > 0) {
      if (invertedPos) {
        int s = position.length;
        if (p < 1.f - position[s - 1]) {
          hueMax = hue[s - 1];
          p1 = 1.f - position[s - 1];
        } else {
          for (int i = 1; i < s; i++) {
            float pos = 1.f - position[s - 1 - i];
            if (p <= pos) {
              hueMin = hue[s - 1 - i];
              hueMax = hue[s - i];
              p0 = pos;
              p1 = 1.f - position[s - i];
              break;
            }
          }
          // end step
          if (p0 == 0.f) {
            p0 = 1.f - position[0];
            hueMin = hue[0];
          }
        }
      } else {
        if (p < position[0]) {
          hueMax = hue[0];
          p1 = position[0];
        } else {
          int max = position.length;
          for (int i = 1; i < max; i++) {
            if (p <= position[i]) {
              i--;
              hueMin = hue[i];
              hueMax = hue[i + 1];
              p0 = position[i];
              p1 = position[i + 1];
              break;
            }
          }
          // end step
          if (p0 == 0.f) {
            p0 = position[position.length - 1];
            hueMin = hue[hue.length - 1];
          }
        }
      }
    }

    float newp = (p - p0) / (p1 - p0);

    float H = ((hueMax - hueMin) * newp) + hueMin;

    // TODO add brightness and saturation modifiers
    // brightness = 1.f - 0.25f/10.f*pb;
    // saturation = 1.f - 0.25f/10.f*pb;

    return Color.getHSBColor(H, saturation, brightness);
  }

  /**
   * interpolate without black and white as min/max
   * 
   * @param start
   * @param end
   * @param p
   * @return
   */
  public static Color interpolate(Color start, Color end, float p) {
    float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
    float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

    float brightness = (startHSB[2] + endHSB[2]) / 2;
    float saturation = (startHSB[1] + endHSB[1]) / 2;

    float hueMax = 0;
    float hueMin = 0;

    hueMin = startHSB[0];
    hueMax = endHSB[0];

    float hue = ((hueMax - hueMin) * p) + hueMin;

    // TODO add brightness and saturation modifiers
    // brightness = 1.f - 0.25f/10.f*pb;
    // saturation = 1.f - 0.25f/10.f*pb;

    return Color.getHSBColor(hue, saturation, brightness);
  }

  /**
   * interpolate with option for black and white at the end
   * 
   * @param start
   * @param end
   * @param p
   * @param pSaturationBrightness
   * @param white
   * @param black
   * @param hue
   * @param position
   * @return
   */
  private static Color interpolateWithBlackAndWhiteWeighted(Color start, Color end, float p,
      float pSaturationBrightness, boolean white, boolean black, float[] hue, float[] position,
      boolean inverted) {
    // pSaturationBrightness as position = inverse
    float posBS = 1.f / pSaturationBrightness;

    // HSB
    float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
    float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);

    // Saturation rises; Hue between lowHue and highHue; Brightness falls at the end;
    // white?
    if (white && p <= posBS) {
      float B = 1;
      float H = startHSB[0];
      // p between 0-posBS
      float S = p / posBS;
      if (S > 1.f)
        S = 1.f;

      return Color.getHSBColor(H, S, B);
    }

    // black?
    else if (black && p >= 1.f - posBS) {
      float S = 1;
      float H = endHSB[0];
      // p between 0-posBS
      float B = (1.f - p) / posBS;
      if (B > 1.f)
        B = 1.f;

      return Color.getHSBColor(H, S, B);

    }

    // weighted hue scale
    else {
      //
      float realp = white ? p - posBS : p;
      float width = 1.f;
      if (white)
        width -= posBS;
      if (black)
        width -= posBS;

      realp = realp / width;

      return interpolateWeighted(start, end, realp, hue, position, inverted);
    }
  }

  /*
   * Determines what colour a heat map cell should be based upon the cell values. with black and
   * white
   */
  private static Color interpolateWithBlackAndWhite(Color start, Color end, float p,
      float pSaturationBrightness, boolean white, boolean black) {
    // HSB
    float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
    float[] endHSB = Color.RGBtoHSB(end.getRed(), end.getGreen(), end.getBlue(), null);
    // Saturation schnell hoch; Hue between lowHue and highHue --> auf 0 und 100%; Brightness am
    // ende schnell runter;
    float saturation = 1;
    if (white) {
      saturation = p * pSaturationBrightness;
      if (saturation > 1.f)
        saturation = 1.f;
    }
    // brightness
    float brightness = 1;
    if (black) {
      brightness = (1 - p) * pSaturationBrightness;
      if (brightness >= 1.f)
        brightness = 1.f;
      if (brightness <= 0.f)
        brightness = 0.f;
    }

    /*
     * float hueMax = 0; float hueMin = 0; hueMin = startHSB[0]; hueMax = endHSB[0];
     * 
     * float hue = ((hueMax - hueMin) * p) + hueMin;
     */

    // Test Huerange
    int bw = 0;
    if (white)
      bw++;
    if (black)
      bw++;
    // reduce range by one or two sides ( black and white area)
    float cut = 1.2f / pSaturationBrightness;
    // new range from 0 to max
    float max = 1.f - cut;
    float realp = p - cut / bw;

    if (realp < 0)
      realp = 0;
    if (realp > 1)
      realp = 1;

    realp = (1.f) * realp / max;

    if (realp < 0)
      realp = 0;
    if (realp > 1)
      realp = 1;

    // old style
    float hueMax = 0;
    float hueMin = 0;
    hueMin = startHSB[0];
    hueMax = endHSB[0];

    float hue = ((hueMax - hueMin) * realp) + hueMin;

    // Zweiter Versuch: Color als Array
    // Color color[] = new Color{new Color(255,255,255),new Color(247,255,145),new
    // Color(255,236,0),new Color(255,179,0),new Color(244,122,0),new Color(,,),new Color(,,),new
    // Color(,,),};

    return Color.getHSBColor(hue, saturation, brightness);
  }


  /*
   * Hue and saturation white: hsb = -01 black: hsb = -10 color: hsb = ?11 increasing saturation
   */
  private static Color interpolateMonochrom(Color start, float p, float pSaturationBrightness,
      boolean isGrey) {
    if (isGrey) {
      float brightness = (1 - p);
      if (brightness >= 1.f)
        brightness = 1.f;
      if (brightness <= 0.f)
        brightness = 0.f;
      return Color.getHSBColor(0, 0, brightness);
    } else {
      // HSB
      float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
      // Saturation schnell hoch; Hue between lowHue and highHue --> auf 0 und 100%; Brightness am
      // ende schnell runter;
      float saturation = p * pSaturationBrightness;
      if (saturation > 1.f)
        saturation = 1.f;
      // brightness
      float brightness = (1 - p) * pSaturationBrightness;
      if (brightness >= 1.f)
        brightness = 1.f;
      if (brightness <= 0.f)
        brightness = 0.f;
      // hue
      float hue = startHSB[0];

      return Color.getHSBColor(hue, saturation, brightness);
    }
  }

  /*
   * Hue and saturation white: hsb = -01 black: hsb = -10 color: hsb = ?11 increasing saturation
   */
  private static Color interpolateMonochromOneSided(Color start, float p, boolean black) {
    // HSB
    float[] startHSB = Color.RGBtoHSB(start.getRed(), start.getGreen(), start.getBlue(), null);
    // Saturation schnell hoch; Hue between lowHue and highHue --> auf 0 und 100%; Brightness am
    // ende schnell runter;
    float saturation = 1;
    // brightness
    float brightness = (1 - p);
    if (brightness >= 1.f)
      brightness = 1.f;
    if (brightness <= 0.f)
      brightness = 0.f;

    // hue
    float hue = startHSB[0];

    if (black)
      return Color.getHSBColor(hue, saturation, brightness);
    else
      return Color.getHSBColor(hue, 1 - brightness, 1);
  }

}
