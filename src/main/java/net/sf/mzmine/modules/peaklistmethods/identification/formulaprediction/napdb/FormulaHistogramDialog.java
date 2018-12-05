package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.napdb;

import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.openscience.cdk.interfaces.IMolecularFormula;
import net.miginfocom.swing.MigLayout;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.HistogramData;
import net.sf.mzmine.modules.visualization.mzhistogram.chart.HistogramDialog;
import net.sf.mzmine.util.FormulaUtils;

public class FormulaHistogramDialog extends HistogramDialog {
  private JTextField txtContains;
  private JTextField txtCount;
  private JLabel lbResults;
  private List<IMolecularFormula> formulas;

  public FormulaHistogramDialog(String title, String xLabel, List<IMolecularFormula> formulas) {
    super(title, xLabel, null, 1);
    getHistoPanel().setBinShift(0.5);
    this.formulas = formulas;

    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.NORTH);
    panel.setLayout(new MigLayout("", "[][][][][][][grow]", "[][]"));

    JLabel lblContains = new JLabel("contains");
    panel.add(lblContains, "cell 0 0,alignx trailing");

    txtContains = new JTextField();
    txtContains.setText("P");
    panel.add(txtContains, "cell 1 0,growx");
    txtContains.setColumns(10);

    JLabel lblCount = new JLabel("count");
    panel.add(lblCount, "cell 3 0,alignx trailing");

    txtCount = new JTextField();
    txtCount.setText("O");
    panel.add(txtCount, "cell 4 0,growx");
    txtCount.setColumns(10);

    JButton btnCalc = new JButton("calc");
    btnCalc.addActionListener(a -> recalcData());
    panel.add(btnCalc, "cell 5 0");

    JLabel lblResults = new JLabel("Results");
    panel.add(lblResults, "cell 0 1");

    lbResults = new JLabel("NONE");
    panel.add(lbResults, "cell 1 1 6 1");
  }

  private void recalcData() {
    try {
      String[] containedElements = txtContains.getText().replaceAll(" ", "").split(",");

      // create filtered list
      List<IMolecularFormula> filtered = formulas.stream()
          .filter(f -> containedElements.length == 0 || Arrays.stream(containedElements)
              .allMatch(element -> FormulaUtils.containsElement(f, element)))
          .collect(Collectors.toList());

      String countElement = txtCount.getText().replaceAll(" ", "");

      if (countElement.length() > 0) {
        double[] data = filtered.stream().mapToInt(f -> FormulaUtils.countElement(f, countElement))
            .mapToDouble(c -> (double) c).toArray();
        HistogramData histo = new HistogramData(data);
        getHistoPanel().setData(histo);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public JTextField getTxtContains() {
    return txtContains;
  }

  public JTextField getTxtCount() {
    return txtCount;
  }

  public JLabel getLbResults() {
    return lbResults;
  }
}
