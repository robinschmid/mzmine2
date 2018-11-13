package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.identities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CombinedAdductType extends AdductType {

  private AdductType[] adducts;

  /**
   * fast creation of combined adducts
   * 
   * @param adduct
   */
  public CombinedAdductType(AdductType... adduct) {
    super();

    // all adducts
    List<AdductType> ad = new ArrayList<AdductType>();
    for (int i = 0; i < adduct.length; i++) {
      for (AdductType n : adduct[i].getAdducts()) {
        ad.add(n);
      }
    }
    adducts = ad.toArray(new AdductType[ad.size()]);
    Arrays.sort(adducts);
    type = IonModificationType.getType(adducts);

    double md = 0;
    int z = 0;
    for (int i = 0; i < adducts.length; i++) {
      AdductType a = adducts[i];
      md += a.getMass();
      z += a.getCharge();
    }
    charge = z;
    mass = md;
    this.parsedName = parseName();
  }

  @Override
  public AdductType[] getAdducts() {
    return adducts;
  }

  @Override
  public int getNumberOfAdducts() {
    return adducts == null ? 0 : adducts.length;
  }

  /**
   * 
   * @return array of names
   */
  @Override
  public String[] getRawNames() {
    if (adducts == null)
      return new String[0];
    String[] names = new String[adducts.length];
    for (int i = 0; i < names.length; i++)
      names[i] = adducts[i].getName();
    return names;
  }

  @Override
  public String parseName() {
    String nname = "";
    if (adducts != null) {
      String s = null;
      int counter = 0;
      for (int i = 0; i < adducts.length; i++) {
        String cs = adducts[i].getName();
        if (s == null) {
          s = cs;
          counter = 1;
        } else if (s == cs)
          counter++;
        else {
          String sign = (adducts[i - 1].getMass() < 0 ? "-" : "+");
          String counterS = counter > 1 ? String.valueOf(counter) : "";
          nname += sign + counterS + s;
          s = cs;
          counter = 1;
        }
      }
      String sign = (adducts[adducts.length - 1].getMass() < 0 ? "-" : "+");
      String counterS = counter > 1 ? String.valueOf(counter) : "";
      nname += sign + counterS + s;
    }
    return nname;
  }

  @Override
  public AdductType remove(AdductType type) {
    List<AdductType> newList = new ArrayList<>();
    for (AdductType m : this.getAdducts())
      newList.add(m);

    for (AdductType m : type.getAdducts())
      newList.remove(m);

    if (newList.isEmpty())
      return null;
    else if (newList.size() == 1)
      return new AdductType(newList.get(0));
    else
      return new CombinedAdductType(newList.toArray(new AdductType[newList.size()]));
  }
}
