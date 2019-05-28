package net.sf.mzmine.util.color;

import javafx.scene.paint.Color;


public class PaintScaleGeneratorFX {
  private static final Color transparent = new Color(0, 0, 0, 0);

  public static ColorFXScale generateMonochrome(Color color, double min, double max,
      boolean oneSided, int steps) {
    ColorFXScale scale = new ColorFXScale();

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
      scale.add(max, Color.WHITE);
    } else {
      scale.add(max, color);
    }
    return scale;
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
      return Color.hsb(0, 0, brightness);
    } else {
      // HSB
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
      return Color.hsb(start.getHue(), saturation, brightness);
    }
  }

  /*
   * Hue and saturation white: hsb = -01 black: hsb = -10 color: hsb = ?11 increasing saturation
   */
  private static Color interpolateMonochromOneSided(Color start, float p, boolean black) {
    // HSB
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
    if (black)
      return Color.hsb(start.getHue(), saturation, brightness);
    else
      return Color.hsb(start.getHue(), 1 - brightness, 1);
  }

}
