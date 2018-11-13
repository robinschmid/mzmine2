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
  public CombinedAdductType(AdductType[] adduct) {
    super();
    this.adducts = adduct;
    Arrays.sort(adducts);

    double md = 0;
    int z = 0;
    for (int i = 0; i < adduct.length; i++) {
      AdductType a = adduct[i];
      md += a.getMass();
      z += a.getCharge();
    }
    charge = z;
    mass = md;
    this.parsedName = parseName();
  }


  /**
   * for combining two adducts
   * 
   * @param a1
   * @param a2
   */
  public CombinedAdductType(final AdductType a1, final AdductType a2) {
    name = "";
    // all adducts
    List<AdductType> ad = new ArrayList<AdductType>();
    for (AdductType n : a1.getAdducts())
      ad.add(n);
    for (AdductType n : a2.getAdducts())
      ad.add(n);
    adducts = ad.toArray(new AdductType[ad.size()]);
    Arrays.sort(adducts);
    charge = a1.getCharge() + a2.getCharge();
    mass = a1.getMass() + a2.getMass();
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
}
