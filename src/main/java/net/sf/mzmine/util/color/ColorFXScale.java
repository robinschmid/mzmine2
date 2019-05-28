package net.sf.mzmine.util.color;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.paint.Color;

public class ColorFXScale {
  private List<Color> colors;
  private List<Double> values;

  public ColorFXScale(List<Color> colors, List<Double> values) {
    this.colors = colors;
    this.values = values;
  }

  public ColorFXScale() {
    colors = new ArrayList<>();
    values = new ArrayList<>();
  }


  public List<Color> getColors() {
    return colors;
  }

  public Color getColor(double value) {
    if (colors == null || colors.size() == 0)
      return null;
    for (int i = 0; i < values.size(); i++) {
      if (value < values.get(i))
        return colors.get(i);
    }
    return colors.get(colors.size() - 1);
  }

  public void add(double value, Color color) {
    colors.add(color);
    values.add(value);
  }

}
