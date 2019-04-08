package net.sf.mzmine.modules.visualization.imaging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.alanmrace.jimzmlparser.exceptions.FatalParseException;
import com.alanmrace.jimzmlparser.imzml.ImzML;
import com.alanmrace.jimzmlparser.mzml.Spectrum;
import com.alanmrace.jimzmlparser.parser.ImzMLHandler;


public class EICImageWindow {

  // public static final String TEST_RESOURCE = "/src/test/resources/MatrixTests_N2.imzML";
  public static final String TEST_RESOURCE = "/src/test/resources/MatrixTests_N2.imzML";
  private JFrame frame;

  /**
   * Instance of ImzML to test, created from the test resource.
   */
  private ImzML instance;
  private JPanel pnImage;

  double[][] z;
  double zMax = 0, zMin = 0;
  private JTextField txtMZ;
  private JTextField txtWidth;
  private JButton btnLoad;
  private JButton btnTic;
  private JButton btnLoadbig;
  private JButton btnGc;

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        try {
          EICImageWindow window = new EICImageWindow();
          window.frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }

  /**
   * Create the application.
   */
  public EICImageWindow() {
    initialize();
  }

  private void loadData() {
    try {
      // System.out.println(new File(getClass().getResource(TEST_RESOURCE).getPath()).exists());
      // instance = ImzMLHandler.parseimzML(getClass().getResource(TEST_RESOURCE).getPath());
      // instance = ImzMLHandler.parseimzML("C:/DATA/MALDI Sh/von
      // Rebeca/20170309_ProbeF_10min_Subli._R/AREA01/imzml/20170309_MADLI_III_20170309_ProbeF_10min_Subli-001.imzML");
      instance = ImzMLHandler.parseimzML("D:\\Daten\\imzml\\Example_Processed.imzML");
    } catch (FatalParseException ex) {
      ex.printStackTrace();
    }
  }

  private void loadBigData() {
    try {
      instance = ImzMLHandler
          .parseimzML("C:/DATA/MALDI Sh/examples/HR2MSI mouse urinary bladder S096.imzML");
      int h = instance.getHeight();
      int w = instance.getWidth();
      System.out.println(w + "x" + h + " (" + w * h + ")");
    } catch (FatalParseException ex) {
      ex.printStackTrace();
    }
  }

  private void createTICImage() {
    z = instance.generateTICImage();
    setZ(z);

    pnImage.revalidate();
    pnImage.repaint();
  }

  protected void createMZImage(double centermz, double pm) {
    z = new double[instance.getWidth()][instance.getHeight()];
    for (int x = 0; x < z.length; x++) {
      for (int y = 0; y < z[x].length; y++) {
        Spectrum s = instance.getSpectrum(x + 1, y + 1);
        try {
          double[] i = s.getIntensityArray();
          double[] mz = s.getmzArray();
          z[x][y] = getMaxIntensity(mz, i, centermz, pm);
        } catch (IOException e) {
          e.printStackTrace();
          z[x][y] = 0;
        }
      }
    }
    setZ(z);
    pnImage.revalidate();
    pnImage.repaint();
  }

  /**
   * maximum intensity in range centermz+-pm
   * 
   * @param mz
   * @param i
   * @param centermz
   * @param pm
   * @return
   */
  private double getMaxIntensity(double[] mz, double[] i, double centermz, double pm) {
    double max = Double.NEGATIVE_INFINITY;
    for (int x = 0; x < mz.length; x++) {
      if (mz[x] >= centermz - pm && mz[x] <= centermz + pm)
        if (i[x] > max)
          max = i[x];
    }
    if (max == Double.NEGATIVE_INFINITY)
      max = 0;
    return max;
  }

  public void setZ(double[][] z) {
    this.z = z;
    if (z != null) {
      zMin = Double.POSITIVE_INFINITY;
      zMax = Double.NEGATIVE_INFINITY;
      StringBuilder s = new StringBuilder();
      for (int x = 0; x < z.length; x++) {
        for (int y = 0; y < z[x].length; y++) {
          if (z[x][y] < zMin)
            zMin = z[x][y];
          if (z[x][y] > zMax)
            zMax = z[x][y];
          s.append(String.valueOf(z[x][y]) + "\t");
        }
        s.append("\n");
      }
      System.out.println(s.toString());
    }
  }

  /**
   * Initialize the contents of the frame.
   */
  private void initialize() {
    frame = new JFrame();
    frame.setBounds(100, 100, 450, 300);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    pnImage = new JPanel() {
      @Override
      public void paint(Graphics g) {
        super.paint(g);
        if (z != null) {
          int w = 20;
          for (int x = 0; x < z.length; x++) {
            for (int y = 0; y < z[x].length; y++) {
              Color c =
                  interpolate(Color.BLUE, Color.RED, (float) ((z[x][y] - zMin) / (zMax - zMin)));
              g.setColor(c);
              g.fillRect(x * w, y * w, w, w);
            }
          }
        }
      }
    };
    frame.getContentPane().add(pnImage, BorderLayout.CENTER);

    JPanel panel = new JPanel();
    frame.getContentPane().add(panel, BorderLayout.NORTH);

    btnLoad = new JButton("load");
    btnLoad.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadData();
      }
    });

    btnGc = new JButton("GC");
    btnGc.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        System.gc();
      }
    });
    panel.add(btnGc);
    panel.add(btnLoad);

    btnTic = new JButton("TIC");
    btnTic.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        createTICImage();
      }
    });

    btnLoadbig = new JButton("loadbig");
    btnLoadbig.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        loadBigData();
      }
    });
    panel.add(btnLoadbig);
    panel.add(btnTic);

    txtMZ = new JTextField();
    txtMZ.setText("1168.38");
    panel.add(txtMZ);
    txtMZ.setColumns(10);

    txtWidth = new JTextField();
    txtWidth.setText("0.2");
    panel.add(txtWidth);
    txtWidth.setColumns(10);

    JButton btnCreate = new JButton("create");
    btnCreate.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        double mz = Double.valueOf(txtMZ.getText());
        double pm = Double.valueOf(txtWidth.getText());
        createMZImage(mz, pm);
      }
    });
    panel.add(btnCreate);

    JButton btnNext = new JButton("next");
    btnNext.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        double mz = Double.valueOf(txtMZ.getText()) + Double.valueOf(txtWidth.getText());
        txtMZ.setText(String.valueOf(mz));
      }
    });
    panel.add(btnNext);
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


  public static File getPathOfJar() {
    /*
     * File f = new File(System.getProperty("java.class.path")); File dir =
     * f.getAbsoluteFile().getParentFile(); return dir;
     */
    try {
      File jar = new File(EICImageWindow.class.getProtectionDomain().getCodeSource().getLocation()
          .toURI().getPath());
      return jar.getParentFile();
    } catch (Exception ex) {
      return new File("");
    }
  }

  public JPanel getPnImage() {
    return pnImage;
  }

  public JTextField getTxtMZ() {
    return txtMZ;
  }

  public JTextField getTxtWidth() {
    return txtWidth;
  }
}
