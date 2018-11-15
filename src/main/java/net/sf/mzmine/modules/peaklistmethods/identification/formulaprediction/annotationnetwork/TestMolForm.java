package net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.annotationnetwork;

import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import net.sf.mzmine.datamodel.identities.iontype.IonModification;
import net.sf.mzmine.datamodel.identities.iontype.IonType;
import net.sf.mzmine.util.FormulaUtils;

public class TestMolForm {

  public static void main(String[] args) {
    IonType ionType = new IonType(IonModification.H, IonModification.H2O);
    IMolecularFormula cdkFormula = FormulaUtils.createMajorIsotopeMolFormula("C2H4O2");
    String stringFormula = MolecularFormulaManipulator.getString(cdkFormula);
    System.out.println(stringFormula);

    IMolecularFormula ion;
    try {
      ion = ionType.addToFormula(cdkFormula);
      String f2 = MolecularFormulaManipulator.getString(ion);
      System.out.println(f2);
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }
  }

}
