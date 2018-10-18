package net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms;

import java.util.ArrayList;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.datastructure.param.ESIAdductType;
import net.sf.mzmine.modules.peaklistmethods.identification.metamsecorrelate.msms.identity.MSMSMultimerIdentity;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class MSMSLogic {


  /**
   * Checks the MSMS scan for matches of x-mers to the x-mer precursorMZ
   * 
   * @param scan
   * @param masslist
   * @param precursorMZ
   * @param adduct only the basic information is taken (charge and deltaMass, molecules are then
   *        added from 1-maxM)
   * @param maxM
   * @param mzTolerance
   * @return List of identities. The first is always the one for the precursor
   */
  public static List<MSMSMultimerIdentity> checkMultiMolCluster(Scan scan, String masslist,
      double precursorMZ, ESIAdductType adduct, int maxM, MZTolerance mzTolerance) {
    MassList masses = scan.getMassList(masslist);
    if (masses == null)
      return null;

    // generate all M adducts 3M+X -> 2M+X -> M+X
    List<ESIAdductType> list = new ArrayList<>();
    for (int i = 1; i <= maxM; i++) {
      ESIAdductType m = new ESIAdductType(adduct);
      m.setMolecules(i);
      list.add(m);
    }

    // result best with the highest number of identities
    List<MSMSMultimerIdentity> ident = null;
    List<MSMSMultimerIdentity> best = null;

    // datapoints of masslist
    DataPoint[] dps = masses.getDataPoints();

    // find precursor in MSMS or create dummy
    DataPoint precursorDP = findDPAt(dps, precursorMZ, mzTolerance);
    if (precursorDP == null)
      precursorDP = new SimpleDataPoint(precursorMZ, 1);

    // check each adduct againt all other
    for (int i = 1; i < list.size(); i++) {
      ident = new ArrayList<>();
      ESIAdductType b = list.get(i);
      double massb = b.getMass(precursorMZ);
      for (int k = 0; k < i; k++) {
        ESIAdductType a = list.get(k);

        // calc mz for neutral mass with this adduct type
        double mza = a.getMZ(massb);

        // check with precursor mz
        DataPoint dp = findDPAt(dps, mza, mzTolerance);
        if (dp != null) {
          // id found
          // find out if there are already some identities
          MSMSMultimerIdentity ia = null;
          MSMSMultimerIdentity ib = null;
          for (MSMSMultimerIdentity old : ident) {
            if (old.getType().equals(a))
              ia = old;
            if (old.getType().equals(b))
              ib = old;
          }

          // create new if empty
          if (ib == null) {
            ib = new MSMSMultimerIdentity(precursorDP, b);
            ident.add(ib);
          }
          if (ia == null) {
            ia = new MSMSMultimerIdentity(dp, a);
            ident.add(ia);
          }

          // add this reference to both
          ia.addLink(ib);
          ib.addLink(ia);
        }
      }
      // highest number of identities
      if (!ident.isEmpty() && (best == null || best.size() < ident.size()))
        best = ident;
    }
    return best;
  }


  /**
   * Heighest dp within mzTolerance
   * 
   * @param dps
   * @param precursorMZ
   * @param mzTolerance
   * @return
   */
  public static DataPoint findDPAt(DataPoint[] dps, double precursorMZ, MZTolerance mzTolerance) {
    DataPoint best = null;
    for (DataPoint dp : dps)
      if ((best == null || dp.getIntensity() > best.getIntensity())
          && mzTolerance.checkWithinTolerance(dp.getMZ(), precursorMZ))
        best = dp;
    return best;
  }

}
