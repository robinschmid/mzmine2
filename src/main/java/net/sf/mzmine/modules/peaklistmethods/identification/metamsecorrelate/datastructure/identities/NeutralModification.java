package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities;

public class NeutralModification extends NeutralMolecule
    implements Comparable<NeutralModification> {
  public enum Type {
    NEUTRAL_LOSS, CLUSTER, UNDEFINED;
    @Override
    public String toString() {
      return toString().replaceAll("_", " ");
    }
  }

  protected Type type = Type.UNDEFINED;

  public NeutralModification(String name, double mass) {
    super(name, mass);
  }

  public NeutralModification(String name, double mass, Type type) {
    this(name, "", mass, type);
  }

  public NeutralModification(String name, String molFormula, double mass) {
    super(name, molFormula, mass);
  }

  public NeutralModification(String name, String molFormula, double mass, Type type) {
    super(name, molFormula, mass);
    this.type = type;
  }


  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getParsedName() {
    return parseName();
  }

  /**
   * sorting
   */
  @Override
  public int compareTo(NeutralModification a) {
    int i = this.getName().compareTo(a.getName());
    if (i == 0) {
      i = Double.compare(getMass(), a.getMass());
    }
    return i;
  }
}
