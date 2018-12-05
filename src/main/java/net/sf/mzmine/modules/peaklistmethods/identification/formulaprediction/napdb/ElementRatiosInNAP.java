package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.napdb;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openscience.cdk.interfaces.IMolecularFormula;
import net.sf.mzmine.util.FormulaUtils;

public class ElementRatiosInNAP {


  public static void main(String[] args) {
    System.out.println("Loading structures");
    Map<String, IMolecularFormula> map = loadFormulasFromNAP();
    System.out.println("Total structures (diff InChIKey1): " + map.size());

    List<IMolecularFormula> formulas = new ArrayList<>(map.values());
    FormulaHistogramDialog dialog =
        new FormulaHistogramDialog("Formula histograms", "element count in formula", formulas);
    dialog.setVisible(true);

  }

  private static Map<String, IMolecularFormula> loadFormulasFromNAP() {
    String file = "D:\\Daten\\spectral lib\\structure_natural_products_full_nap_db.txt";

    Map<String, IMolecularFormula> formulas = new HashMap<>();
    // stream line by line
    try (BufferedReader br = Files.newBufferedReader(Paths.get(file))) {
      // br returns as stream and convert it into a List
      br.lines().skip(1).map(line -> line.split("\t")) // split
          .filter(split -> split.length >= 7) // rows without mol formula
          .map(split -> new String[] {split[5], split[6]}) // 5:InChIKey1, 6:MolecularFormula
          .filter(data -> !data[1].contains(")")) // filter out ()n formulas
          .filter(data -> !formulas.containsKey(data[0])).forEach(data -> {
            String InChIKey1 = data[0];
            String f = data[1];
            IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(f);
            formulas.put(InChIKey1, formula);
          });

    } catch (IOException e) {
      e.printStackTrace();
    }
    return formulas;
  }


}
