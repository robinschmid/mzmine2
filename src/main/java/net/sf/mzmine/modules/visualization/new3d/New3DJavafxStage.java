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

package net.sf.mzmine.modules.visualization.new3d;

import java.util.logging.Logger;
import javafx.event.EventHandler;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import net.sf.mzmine.util.color.ColorFXScale;
import net.sf.mzmine.util.color.PaintScaleGeneratorFX;

public class New3DJavafxStage extends Stage {

  final Group plot = new Group();

  private static final Logger LOG = Logger.getLogger(New3DJavafxStage.class.getName());

  private double mousePosX, mousePosY;
  private double mouseOldX, mouseOldY;

  private final Rotate rotateX = new Rotate(45, Rotate.X_AXIS);
  private final Rotate rotateY = new Rotate(-45, Rotate.Y_AXIS);

  private ColorFXScale scale;

  public New3DJavafxStage(float[][] intensityValues, int rtResolution, int mzResolution,
      double maxBinnedIntensity, double minIntensity, double maxintensity) {
    initColors(100, minIntensity, maxintensity);

    plot.getTransforms().addAll(rotateX, rotateY);
    int size = 500;

    StackPane root = new StackPane();
    root.getChildren().add(plot);

    TriangleMesh mesh = new TriangleMesh();

    // TreeSet setOfPeaks = new TreeSet();

    float amplification = 130;
    float factorX = size / rtResolution;
    float factorZ = size / mzResolution;

    float maxIntensityValue = -Float.MAX_VALUE;
    for (int i = 0; i < rtResolution; i++) {
      for (int j = 0; j < mzResolution; j++) {
        if (maxIntensityValue < intensityValues[i][j]) {
          maxIntensityValue = intensityValues[i][j];
        }
      }
    }

    for (int x = 0; x < rtResolution; x++) {
      for (int z = 0; z < mzResolution; z++) {
        mesh.getPoints().addAll(x * factorX, -intensityValues[x][z] * amplification, z * factorZ);
      }
    }

    int rtLength = rtResolution;
    int mzLength = mzResolution;
    float rtTotal = rtLength;
    float mzTotal = mzResolution;

    for (float x = 0; x < rtLength - 1; x++) {
      for (float y = 0; y < mzLength - 1; y++) {

        float x0 = x / rtTotal;
        float y0 = y / mzTotal;
        float x1 = (x + 1) / rtTotal;
        float y1 = (y + 1) / mzTotal;

        mesh.getTexCoords().addAll( //
            x0, y0, // 0, top-left
            x0, y1, // 1, bottom-left
            x1, y0, // 2, top-right
            x1, y1 // 3, bottom-right
        );
      }
    }

    // faces
    for (int x = 0; x < rtLength - 1; x++) {
      for (int z = 0; z < mzLength - 1; z++) {

        int tl = x * mzLength + z; // top-left
        int bl = x * mzLength + z + 1; // bottom-left
        int tr = (x + 1) * mzLength + z; // top-right
        int br = (x + 1) * mzLength + z + 1; // bottom-right

        int offset = (x * (mzLength - 1) + z) * 8 / 2; // div 2 because we have u AND v in the list

        // working
        mesh.getFaces().addAll(bl, offset + 1, tl, offset + 0, tr, offset + 2);
        mesh.getFaces().addAll(tr, offset + 2, br, offset + 3, bl, offset + 1);

      }
    }
    int width = rtLength;
    int height = mzLength;

    WritableImage wr = new WritableImage(width, height);
    PixelWriter pw = wr.getPixelWriter();
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        Color color = scale.getColor(intensityValues[x][y]);
        pw.setColor(x, y, color);
      }
    }

    Image diffuseMap = wr;

    PhongMaterial material = new PhongMaterial();
    material.setDiffuseMap(diffuseMap);

    int sizeX = rtLength;
    int sizeZ = mzLength;
    MeshView meshView = new MeshView(mesh);
    meshView.setTranslateX(-0.5 * sizeX);
    meshView.setTranslateZ(-0.5 * sizeZ);
    meshView.setMaterial(material);
    meshView.setCullFace(CullFace.NONE);
    meshView.setDrawMode(DrawMode.FILL);
    meshView.setDepthTest(DepthTest.ENABLE);

    plot.getChildren().addAll(meshView);

    Scene scene = new Scene(root, 800, 600, true, SceneAntialiasing.BALANCED);
    scene.setCamera(new PerspectiveCamera());

    scene.setOnMousePressed(me -> {
      mouseOldX = me.getSceneX();
      mouseOldY = me.getSceneY();
    });
    scene.setOnMouseDragged(me -> {
      mousePosX = me.getSceneX();
      mousePosY = me.getSceneY();
      rotateX.setAngle(rotateX.getAngle() - (mousePosY - mouseOldY));
      rotateY.setAngle(rotateY.getAngle() + (mousePosX - mouseOldX));
      mouseOldX = mousePosX;
      mouseOldY = mousePosY;

    });

    makeZoomable(root);
    this.setScene(scene);

  }

  private void initColors(int steps, double minIntensity, double maxintensity) {
    scale = PaintScaleGeneratorFX.generateMonochrome(Color.BLUE, minIntensity, maxintensity, true,
        steps);
  }

  public void makeZoomable(StackPane control) {

    final double MAX_SCALE = 20.0;
    final double MIN_SCALE = 0.1;

    control.addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {

      @Override
      public void handle(ScrollEvent event) {

        double delta = 1.2;

        double scale = control.getScaleX();

        if (event.getDeltaY() < 0) {
          scale /= delta;
        } else {
          scale *= delta;
        }

        scale = clamp(scale, MIN_SCALE, MAX_SCALE);

        control.setScaleX(scale);
        control.setScaleY(scale);

        event.consume();

      }

    });
  }

  public static double clamp(double value, double min, double max) {

    if (Double.compare(value, min) < 0)
      return min;

    if (Double.compare(value, max) > 0)
      return max;

    return value;
  }

}
