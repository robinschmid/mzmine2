package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import com.google.common.collect.Range;
import gnu.trove.list.array.TDoubleArrayList;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.scans.ScanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pick isotopes from MS1
 */
public class IsotopeUtils {

    public static DataPoint[] extractIsotopes(PeakListRow row) {
        final List<Feature> bestFeatures = new ArrayList<>();
        final double intensityThreshold = row.getBestPeak().getHeight()*0.5d;
        for (Feature peak : row.getPeaks()) {
            if (peak.getHeight() >= intensityThreshold)
                bestFeatures.add(peak);
        }

        final List<DataPoint[]> isotopesPatterns = bestFeatures.stream().map(IsotopeUtils::extractIsotopes).collect(Collectors.toList());
        // average across intensities and mzs
        int n = isotopesPatterns.stream().mapToInt(x -> x.length).max().orElse(0);
        final DataPoint[] pattern = new DataPoint[n];
        for (int k=0; k < n; ++k) {
            final int PeakIndex = k;
            final DataPoint[] dps = isotopesPatterns.stream().filter(x->x.length>PeakIndex).map(x->x[PeakIndex]).toArray(DataPoint[]::new);
            pattern[k] = mergeDataPointsWithOutlierRemoval(dps,0.2d);
        }
        return pattern;
    }

    public static DataPoint[] extractIsotopes(Feature feature) {
        final int[] scanNumbers = feature.getScanNumbers();
        final DataPoint[] chromatographicPeak = new DataPoint[scanNumbers.length];
        for (int k=0; k < scanNumbers.length; ++k) {
            chromatographicPeak[k] = feature.getDataPoint(scanNumbers[k]);
        }
        final int apex = Arrays.binarySearch(scanNumbers, feature.getRepresentativeScanNumber());
        int left = apex;
        double intensity = chromatographicPeak[apex].getIntensity();
        while (left >= 0) {
            if (chromatographicPeak[left].getIntensity() < intensity*0.25) {
                break;
            }
            --left;
        }
        ++left;
        int right = apex;
        while (right < chromatographicPeak.length) {
            if (chromatographicPeak[right].getIntensity() < intensity*0.25) {
                break;
            }
            ++right;
        }
        int len = right-left;
        final int[] usedScanNumbers = new int[len];
        int k=0;
        for (int i=left; i < right; ++i)
            usedScanNumbers[k++] = scanNumbers[i];
        return detectPattern(feature, Arrays.binarySearch(usedScanNumbers,scanNumbers[apex]), usedScanNumbers);
    }

    // - in each scan we grab all peaks which are in mass range of an isotope pattern8
    // - we then select the subset that correlates with the main peak
    private static DataPoint[] detectPattern(Feature feature, int apex, int[] usedScanNumbers) {

        final Range<Double>[] isoRanges = new Range[]{
                Range.closed(0.99664664-0.002, 1.00342764+0.002),
                Range.closed(1.99653883209004-0.002,  2.0067426280592295+0.002),
                Range.closed(2.9950584-0.002, 3.00995027+0.002),
                Range.closed(3.99359037-0.002, 4.01300058+0.002),
                Range.closed(4.9937908-0.002 , 5.01572941+0.002)
        };

        DataPoint[][][] potentialIsotopePeaks = new DataPoint[usedScanNumbers.length][isoRanges.length+1][];
        final RawDataFile raw = feature.getDataFile();
        for (int k=0; k < usedScanNumbers.length; ++k) {
            final DataPoint[] spectrum = raw.getScan(usedScanNumbers[k]).getDataPointsByMass(Range.closed(feature.getMZ()-1, feature.getMZ()+6));
            ScanUtils.sortDataPointsByMz(spectrum);
            final double monoIsotopic = feature.getDataPoint(usedScanNumbers[k]).getMZ();
            final Range<Double> mzrange = feature.getRawDataPointsMZRange();
            potentialIsotopePeaks[k][0] = detectPeak(spectrum, monoIsotopic, Range.closed(mzrange.lowerEndpoint()-monoIsotopic, mzrange.upperEndpoint()-monoIsotopic));
            for (int i=1; i <= isoRanges.length; ++i) {
                potentialIsotopePeaks[k][i] = detectPeak(spectrum, monoIsotopic, isoRanges[i-1]);
                if (potentialIsotopePeaks[k][i].length==0) break;
            }
        }
        // next step: align each peak to a chromatographic peak
        final List<DataPoint> selectedPeaks = new ArrayList<>();
        selectedPeaks.add(new SimpleDataPoint(feature.getMZ(), 1d));
        for (int k=1; k <= isoRanges.length; ++k) {
            List<ChromatographicTrace> chromatographicPeaks = align(feature, potentialIsotopePeaks, k, apex, usedScanNumbers);
            if (chromatographicPeaks.isEmpty())
                return selectedPeaks.toArray(new DataPoint[0]);
            ChromatographicTrace trace = chromatographicPeaks.get(0);
            if (trace.correlation>=0.9) {
                selectedPeaks.add(trace.toPeak(feature));
                for (int i = 1; i < chromatographicPeaks.size(); ++i) {
                    if (chromatographicPeaks.get(i).correlation >= 0.90 && chromatographicPeaks.get(i).correlation >= (trace.correlation - 0.05)) {
                        selectedPeaks.add(chromatographicPeaks.get(i).toPeak(feature));
                    }
                }
            } else break;
        }
        return selectedPeaks.toArray(new DataPoint[0]);
    }

    private static class ChromatographicTrace {
        private final ChromatographicPeak[] peaks;
        private int start, end;
        private double correlation;

        public ChromatographicTrace(ChromatographicPeak[] peaks, int start, int end, double correlation) {
            this.peaks = peaks;
            this.start = start;
            this.end = end;
            this.correlation = correlation;
        }

        public DataPoint toPeak(Feature feature) {
            final DataPoint[] origins = new DataPoint[end-start];
            for (int i=start; i < end; ++i) {
                origins[i-start] = new SimpleDataPoint(peaks[i].getMZ(), (peaks[i].getIntensity()/feature.getDataPoint(peaks[i].scanId).getIntensity()));

            }
            return mergeDataPointsWithOutlierRemoval(origins, 0.2d);
        }
    }

    private static DataPoint mergeDataPointsWithOutlierRemoval(DataPoint[] dataPoints, double percentile) {
        dataPoints = outlierRemoval(dataPoints,percentile);
        double mz=0d;
        double intens=0d;
        for (DataPoint d : dataPoints) {
            mz += d.getMZ();
            intens += d.getIntensity();
        }
        return new SimpleDataPoint(mz/dataPoints.length,intens/dataPoints.length);
    }

    private static DataPoint[] outlierRemoval(DataPoint[] dataPoints, double percentil) {
        final int len = dataPoints.length;
        final int t = (int)Math.floor(len*percentil);
        if (t <= 0) return dataPoints;
        final double[] mzvalues = new double[len];
        final double[] intvalues = new double[len];
        for (int i=0; i < dataPoints.length; ++i) {
            mzvalues[i] = dataPoints[i].getMZ();
            intvalues[i] = dataPoints[i].getIntensity();
        }
        Arrays.sort(mzvalues);
        Arrays.sort(intvalues);

        Range<Double> mzValues = Range.closed(mzvalues[t], mzvalues[mzvalues.length-t]);
        Range<Double> intValues = Range.closed(intvalues[t], intvalues[intvalues.length-t]);
        final ArrayList<DataPoint> extracted = new ArrayList<>();
        for (DataPoint dp : dataPoints) {
            if (mzValues.contains(dp.getMZ()) && intValues.contains(dp.getIntensity()))
                extracted.add(dp);
        }
        if (extracted.isEmpty()) {
            System.err.println("OH NOOO. SUCH STUFF REALLY HAPPENS?");
            return dataPoints;
        } else return extracted.toArray(new DataPoint[extracted.size()]);

    }

    private static class ChromatographicPeak implements DataPoint {
        private final int scanId;
        private final double mz;
        private final double intensity;

        public ChromatographicPeak(int scanId, double mz, double intensity) {
            this.scanId = scanId;
            this.mz = mz;
            this.intensity = intensity;
        }
        public ChromatographicPeak(int scanId, DataPoint dp) {
            this(scanId,dp.getMZ(),dp.getIntensity());
        }

        @Override
        public double getMZ() {
            return mz;
        }

        @Override
        public double getIntensity() {
            return intensity;
        }
    }

    private static List<ChromatographicTrace> align(Feature feature, DataPoint[][][] potentialIsotopePeaks, int k, int apex, int[] scanNumbers) {
        // start with apex, then extend to both sides
        if (potentialIsotopePeaks[apex].length<=k)
            return Collections.emptyList();
        final double mzVar;
        {
            double[] values = Arrays.stream(scanNumbers).mapToDouble(x -> feature.getDataPoint(x).getMZ()).toArray();
            double mean = Arrays.stream(values).sum()/values.length;
            mzVar = Arrays.stream(values).map(x->(x-mean)*(x-mean)).sum()/values.length;

        }final DataPoint[] apexSpec = potentialIsotopePeaks[apex][k];
        final List<ChromatographicPeak>[] chrs = new ArrayList[apexSpec.length];
        for (int i=0; i < apexSpec.length; ++i) {
            chrs[i] = new ArrayList<>();
            chrs[i].add(new ChromatographicPeak(scanNumbers[apex], apexSpec[i]));
        }
        Arrays.sort(chrs, (u,v)->Double.compare(v.get(0).getIntensity(),u.get(0).getIntensity()));
        // now extend to the left
        boolean[] extandable = new boolean[chrs.length];
        Arrays.fill(extandable,true);
        for (int i=apex-1; i >= 0; --i) {
            if (potentialIsotopePeaks[i][k]==null) break;
            addChrom(feature, potentialIsotopePeaks[i][k], scanNumbers, mzVar, chrs, i, i+1,extandable);
        }
        Arrays.fill(extandable,true);
        for (List<ChromatographicPeak> dps : chrs) Collections.reverse(dps);
        // and extend to the right
        for (int i=apex+1; i < potentialIsotopePeaks.length; ++i) {
            if (potentialIsotopePeaks[i][k]==null) break;
            addChrom(feature, potentialIsotopePeaks[i][k], scanNumbers, mzVar, chrs, i, i-1,extandable);
        }
        final List<ChromatographicTrace> traces = new ArrayList<>();
        eachChromatogram:
        for (int i=0; i < chrs.length; ++i) {
            ChromatographicPeak[] peaks = chrs[i].toArray(new ChromatographicPeak[chrs[i].size()]);
            // build correlatiob
            int apx=0;
            for (; apx < scanNumbers.length; ++apx) if (peaks[apx].scanId==scanNumbers[apex]) break;
            final Correlation corr = new Correlation();
            corr.add(feature.getDataPoint(peaks[apx].scanId).getIntensity(), peaks[apx].intensity);
            int left=apx-1, right=apx+1;
            for (int q=0; q < 2; ++q) {
                if (left >=0 && right < peaks.length) {
                    if (feature.getDataPoint(peaks[left].scanId).getIntensity() >= feature.getDataPoint(peaks[right].scanId).getIntensity()) {
                        corr.add(feature.getDataPoint(peaks[left].scanId).getIntensity(), peaks[left].getIntensity());
                        --left;
                    } else {
                        corr.add(feature.getDataPoint(peaks[right].scanId).getIntensity(), peaks[right].getIntensity());
                        ++right;
                    }
                } else if (left >= 0) {
                    corr.add(feature.getDataPoint(peaks[left].scanId).getIntensity(), peaks[left].getIntensity());
                    --left;
                } else if (right < peaks.length) {
                    corr.add(feature.getDataPoint(peaks[right].scanId).getIntensity(), peaks[right].getIntensity());
                    ++right;
                } else {
                    continue eachChromatogram;
                }
            }
            do {
                double corLeft = 0d, corRight = 0d,a=0,b=0,c=0,d=0;
                if (left >= 0) {
                    a = feature.getDataPoint(peaks[left].scanId).getIntensity();
                    b = peaks[left].intensity;
                    corLeft = corr.correlation(a,b);
                }
                if (right < peaks.length) {
                    c = feature.getDataPoint(peaks[right].scanId).getIntensity();
                    d = peaks[right].intensity;
                    corRight = corr.correlation(c,d);
                }
                if (corLeft < 0.85 && corRight < 0.85)
                    break;
                if (corLeft > corRight) {
                    corr.add(a,b);
                    --left;
                } else {
                    corr.add(c,d);
                    ++right;
                }
            } while (true);
            ChromatographicTrace e = new ChromatographicTrace(peaks, left + 1, right, corr.correlation());
            // check if left and right edge of peak either are on noise level or are
            if (e.correlation >= 0.90 && (e.start==0 || e.peaks[e.start].intensity < feature.getDataPoint(scanNumbers[0]).getIntensity()) && (e.end==scanNumbers.length || e.peaks[e.end-1].intensity < feature.getDataPoint(scanNumbers[scanNumbers.length-1]).getIntensity())) {
                traces.add(e);
            } else {
                System.err.println("Do not add!");
            }
        }
        traces.sort((u,v)->Double.compare(v.correlation,u.correlation));
        return traces;
    }

    private static void addChrom(Feature feature, DataPoint[] dps1, int[] scanNumbers, double mzVar, List<ChromatographicPeak>[] chrs, int iNow, int iBefore, boolean[] extandable) {
        final double foldChange = feature.getDataPoint(scanNumbers[iBefore]).getIntensity() / feature.getDataPoint(scanNumbers[iNow]).getIntensity();
        int n = dps1.length;
        for (int a=0; a < chrs.length; ++a) {
            if (!extandable[a])
                continue; // do not allow gaps
            int bestB = 0;
            double maxScore = Double.NEGATIVE_INFINITY;
            for (int b=0; b < n; ++b) {
                final double s = score(chrs[a].get(chrs[a].size()-1), dps1[b], mzVar, foldChange*foldChange);
                if (s > maxScore) {
                    maxScore = s;
                    bestB = b;
                }
            }
            if (maxScore>1e-9) {
                chrs[a].add(new ChromatographicPeak(scanNumbers[iNow], dps1[bestB]));
                dps1[bestB] = dps1[n-1];
                --n;
                if (n<=0)
                    break;
            } else {
                extandable[a] = false;
            }
        }



    }

    private static class Correlation {
        private TDoubleArrayList left, right;

        public Correlation() {
            this.left = new TDoubleArrayList();
            this.right = new TDoubleArrayList();
        }

        public double correlation() {
            double meanL = left.sum()/left.size();
            double meanR = right.sum()/right.size();
            double a=0d, b=0d, c = 0d;
            for (int i=0; i < left.size(); ++i) {
                final double x = (left.getQuick(i) - meanL);
                final double y = (right.getQuick(i)-meanR);
                a += x*x;
                b += y*y;
                c += x*y;
            }
            return c / Math.sqrt(a*b);
        }

        public double correlation(double l, double r) {
            double meanL = (left.sum() + l)/(left.size()+1d);
            double meanR = (right.sum() + r)/(right.size()+1d);
            double a=l-meanL, b=r-meanR, c = (a-meanL)*(b-meanR);
            for (int i=0; i < left.size(); ++i) {
                final double x = (left.getQuick(i) - meanL);
                final double y = (right.getQuick(i)-meanR);
                a += x*x;
                b += y*y;
                c += x*y;
            }
            return c / Math.sqrt(a*b);
        }

        public void add(double l, double r) {
            left.add(l);
            right.add(r);
        }
    }

    static double score(DataPoint a, DataPoint b, double mzVar, double intVar) {

        double mzDiff = a.getMZ()-b.getMZ();
        if (Math.abs(mzDiff) > 8*Math.sqrt(mzVar)) return 0;
        mzDiff = mzDiff*mzDiff;
        final double intDiff = Math.pow(a.getIntensity()/b.getIntensity(),2);
        return Math.exp(-0.25 * (mzDiff/mzVar + intDiff/intVar))/(Math.sqrt(Math.PI*mzVar*intVar));
    }

    private static DataPoint[] detectPeak(DataPoint[] spectrum, double monoIsotopic, Range<Double> mzRange) {
        mzRange = Range.closed(mzRange.lowerEndpoint() + monoIsotopic, mzRange.upperEndpoint() + monoIsotopic);
        int i = ScanUtils.findFirstPeakWithin(spectrum, mzRange);
        if (i<0) return new DataPoint[0];
        int j=i+1;
        for (; j <= spectrum.length; ++j) {
            if (j >= spectrum.length || !mzRange.contains(spectrum[j].getMZ()))
                break;
        }
        final DataPoint[] extract = new DataPoint[j-i];
        for (int k=i; k < j; ++k)
            extract[k-i] = spectrum[k];
        return extract;

    }

}
