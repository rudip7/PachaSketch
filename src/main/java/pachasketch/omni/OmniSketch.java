package pachasketch.omni;

import pachasketch.Sketch;
import pachasketch.omni.utils.OmniQueryResult;

import java.util.*;

public class OmniSketch implements Sketch {

    public double delta;
    public double eps;
    public int maxSize;
    public int dyadicRangeBits;
    public int numAttributes;
    public int[] catColMap;
    public int[] numColMap;

    public Kmin kminTmp;

    public int width;
    public int depth;
    public CountMin[] cmSketches;
    public CountMinDyad[][] cmSketchesRange;

    public OmniSketch(int[] catColMap, int[] numColMap, double delta, double eps, int maxSize,
                      int dyadicRangeBits) {
        this.delta = delta;
        this.eps = eps;
        this.depth = (int) Math.ceil(Math.log(2 / delta));
        this.width = 1 + (int) Math.ceil(Math.E * Math.pow((eps + 1) / eps, 1.0 / this.depth));

        this.maxSize = maxSize;
        this.numAttributes = catColMap.length + numColMap.length;
        this.dyadicRangeBits = dyadicRangeBits;
        this.catColMap = catColMap;
        this.numColMap = numColMap;

        this.cmSketches = new CountMin[catColMap.length];
        for (int i = 0; i < catColMap.length; i++) {
            this.cmSketches[i] = new CountMin(catColMap[i], width, depth, delta / 2, maxSize);
        }

        this.cmSketchesRange = new CountMinDyad[numColMap.length][dyadicRangeBits + 1];
        for (int i = 0; i < numColMap.length; i++) {
            for (int j = dyadicRangeBits; j > -1; j--) {
                this.cmSketchesRange[i][dyadicRangeBits - j] = new CountMinDyad(numColMap[i], j,
                        width, depth, delta / 2, maxSize, dyadicRangeBits);
            }
        }

        kminTmp = new Kmin(delta/2, maxSize);
    }


    public void add(int id, String[] element) {
        long hx = kminTmp.hash(id);

        // Add categorical values to the CountMin sketches
        for (int i = 0; i < catColMap.length; i++) {
            long catValue = element[catColMap[i]].hashCode();
            cmSketches[i].add(catValue, hx);
        }

        // Add numerical values to the CountMinDyad sketches
        for (int i = 0; i < numColMap.length; i++) {
            long numValue = Long.parseLong(element[numColMap[i]]);
            // Compute all dyadic ranges it belongs to.
            // Insert in all those ranges.
            long[][] ranges = wrapperInitLogRanges(numValue);
            for (int j = 0; j < ranges[2].length; j++) {
                cmSketchesRange[i][j].add(ranges[1][j], ranges[2][j], hx);
            }
        }
    }

    private long[][] wrapperInitLogRanges(long l) {
        //l += (long) Math.pow(2, Main.dyadicRangeBits - 1); // shift to positive
        long[][] ranges;
        if (l < 0) {
            ranges = getLogRangesNegative(l);
        } else {
            ranges = getLogRanges(l + 1);
            for (int i = 0; i < ranges[2].length; i++) {
                ranges[1][i] = ranges[1][i] - 1L;//- (long) Math.pow(2, Main.dyadicRangeBits - 1);
                ranges[2][i] = ranges[2][i] - 1L;//- (long) Math.pow(2, Main.dyadicRangeBits - 1);
            }
        }
        return ranges;
    }

    public long[][] getLogRangesNegative(long inputKey) {
        long[][] logRanges = getLogRanges(-1 * inputKey);
        for (int i = 0; i < logRanges.length; i++) {
            for (int j = 0; j < logRanges[i].length; j++) {
                logRanges[i][j] = -1 * logRanges[i][j];
            }
        }
        for (int i = 0; i < logRanges[0].length; i++) {
            logRanges[0][i] = logRanges[0][i] - 1L ;//- (long) Math.pow(2, Main.dyadicRangeBits - 1);
        }
        long[] upper = Arrays.copyOf(logRanges[2], logRanges[2].length);
        logRanges[2] = logRanges[1];
        logRanges[1] = upper;
        return logRanges;
    }

    public long[][] getLogRanges(long inputKey) {
        long[] coeff      = new long[dyadicRangeBits];
        long[] lowerBound = new long[dyadicRangeBits]; // at pos i we store the x for ranges of size 2^(maxSize-i)
        long[] upperBound = new long[dyadicRangeBits]; // at pos i we store the x for ranges of size 2^(maxSize-i)

        long halfPoint = (long) Math.pow(2, dyadicRangeBits -1);
        if (inputKey < halfPoint) {
            coeff[0]=0;
            lowerBound[0] = 1; // inclusive, starting from 1
            upperBound[0] = halfPoint; // inclusive
        } else {
            coeff[0]=1;
            lowerBound[0] = halfPoint; //inclusive, starting from 1
            upperBound[0] = halfPoint*2; // inclusive
        }

        long pow = halfPoint;
        for (int i = 1; i< dyadicRangeBits -1; i++) {
            long prevCoeff = coeff[i-1];
            long newCoeffLower = prevCoeff*2;
            pow/=2;
            if ((newCoeffLower + 1) *pow < inputKey) {
                newCoeffLower++;
                coeff[i] = newCoeffLower;
            } else {
                coeff[i] = newCoeffLower;
            }
            lowerBound[i] = coeff[i]*pow+1;
            upperBound[i] = (coeff[i]+1)*pow;
        }
        lowerBound[dyadicRangeBits -1] = inputKey;
        upperBound[dyadicRangeBits -1] = inputKey;
        coeff[dyadicRangeBits -1] = inputKey;

        long[][] result = new long[3][];
        result[0] = coeff;
        result[1] = lowerBound;
        result[2] = upperBound;
        return result;
    }

    int minCMRow = 0;

    public int withinConstraint= 0;
    public int outsideConstraint = 0;
    public OmniQueryResult query(List<Object> query) {
        if (query.size() != numAttributes) {
            throw new IllegalArgumentException("Query must have the same number of dimensions as the sketch. Expected: " + numAttributes + ", got: " + query.size());
        }


        // Process categorical predicates
        List<Integer> catDimensions = new ArrayList<>(catColMap.length);
        List<Set<Long>> catPredicates = new ArrayList<>(catColMap.length);
        for (int i = 0; i < catColMap.length; i++) {
            int idx = catColMap[i];
            Object catPredicate = query.get(idx);
            HashSet<Long> tempSet = new HashSet<>();
            if (catPredicate instanceof List<?>) {
                for (Object p : (List<?>) catPredicate) {
                    if (p instanceof String) {
                        tempSet.add((long) p.hashCode());
                    } else if (p instanceof Number) {
                        tempSet.add(((Number) p).longValue());
                    } else {
                        throw new IllegalArgumentException("Categorical predicate contains unsupported type: " + p.getClass());
                    }
                }
                catPredicates.add(tempSet);
                catDimensions.add(i);
            } else if (!(catPredicate instanceof String && catPredicate.equals("*"))) {
                throw new IllegalArgumentException("Query predicate at index " + idx + " expected to be a set or '*'.");
            }
        }

        // Process numerical predicates
        List<Integer> numDimensions = new ArrayList<>();
        List<int[]> numPredicates = new ArrayList<>(numColMap.length);
        for (int i = 0; i < numColMap.length; i++) {
            int idx = numColMap[i];
            Object numPredicate = query.get(idx);
            if ((numPredicate instanceof List<?> bounds) && bounds.size() == 2) {
                int lower = ((Double) bounds.get(0)).intValue();
                int upper = ((Double) bounds.get(1)).intValue();
                if (lower > upper) {
                    throw new IllegalArgumentException("Lower bound cannot be greater than upper bound.");
                }
                numPredicates.add(new int[]{lower, upper});
                numDimensions.add(i);
            } else if (!(numPredicate instanceof String && numPredicate.equals("*"))) {
                throw new IllegalArgumentException("Query predicate at index " + idx +
                        " expected to be a numerical list of size 2 with [lower_bound, upper_bound] or '*'.");
            }
        }

        double S_cap;
        int n_max;
        int B_virtual = 0;
        int nPredicates = catPredicates.size() + numPredicates.size();
        TreeSet<Long>[] samples = new TreeSet[nPredicates * depth];


        int[][] ns = new int[numAttributes][depth];
        int[] Bs = new int[numAttributes];

        for (int i = 0; i < catPredicates.size(); i++) {
            int attr = catDimensions.get(i);
            TreeSet<Long>[] set = new TreeSet[depth];
            Set<Long> predicate = catPredicates.get(i);
//            if (B_virtual < predicate.size() * maxSize) {
//                B_virtual = predicate.size() * maxSize;
//            }
            Bs[attr] = predicate.size() * maxSize;
//            B_virtual = predicate.size() * maxSize;
            for (Long p : predicate) {
                Kmin[] temp = cmSketches[attr].query(p);
                for (int d = 0; d < depth; d++) {
                    if (set[d] == null) {
                        set[d] = temp[d].sketch;
                    } else {
                        set[d].addAll(temp[d].sketch);
                    }
                    ns[attr][d] += temp[d].n;
                }
            }
            System.arraycopy(set, 0, samples, i * depth, depth);
        }

        for (int i = 0; i < numPredicates.size(); i++) {
            int attr = numDimensions.get(i);
            int[] predicate = numPredicates.get(i);
            ArrayList<long[]> rangesList = wrapLogRanges(predicate[0], predicate[1]);

            TreeSet<Long>[] set = new TreeSet[depth];
//            if (B_virtual < rangesList.size() * maxSize) {
//                B_virtual = rangesList.size() * maxSize;
//            }
            Bs[catColMap.length + attr] = rangesList.size() * maxSize;
//            B_virtual = rangesList.size() * maxSize;
            for (int j = 0; j < rangesList.size(); j++) {
                int indexOfRange = (dyadicRangeBits-1) - getIndexOfRange(rangesList.get(j)) ;
                CountMinDyad cm = cmSketchesRange[attr][indexOfRange];
                TreeSet<Long>[] rangeSet = cm.rangeQuery(rangesList.get(j)[0], rangesList.get(j)[1], ns[catColMap.length + attr]);
                for (int d = 0; d < depth; d++) {
                    if (set[d] == null) {
                        set[d] = rangeSet[d];
                    } else {
                        set[d].addAll(rangeSet[d]);
                    }
                }

            }
            System.arraycopy(set, 0, samples, (catPredicates.size()+i) * depth, depth);
        }

//        n_max = getNmax(ns);
        int[] nMaxIndex = getNMaxIndex(ns);
        n_max = ns[nMaxIndex[0]][nMaxIndex[1]];
        B_virtual = Bs[nMaxIndex[0]];

        S_cap = getAltEstKMV(samples);
        double constraint = 3 * Math.log((4 * nPredicates * depth * Math.sqrt(B_virtual))
                / delta)/(eps * eps);

        if (S_cap < constraint) {
            int estimate = (int) (Math.ceil(2 * n_max * Math.log((4 * nPredicates * depth *
                    Math.sqrt(B_virtual)) / delta)/(B_virtual * eps * eps)));
            return new OmniQueryResult(estimate, 1);
        } else {
            int estimate = (int) Math.ceil(S_cap * n_max / B_virtual);
            return new OmniQueryResult(estimate, 2);
        }

    }

    private double getAltEstKMV(TreeSet<Long>[] samples) {
        int numJoins = samples.length;
        int c = 0;
        Iterator<Long> iter = samples[0].iterator();
        while (iter != null && iter.hasNext()) {
            boolean found = true;
            Long i = iter.next();
            for (int j = 1; j < numJoins; j++) {
                Long otherElement = samples[j].ceiling(i);
                if (otherElement == null) {
                    found = false;
                    iter = null;
                    break;
                } // not contained
                else if (otherElement.equals(i)) continue; // is contained
                else {
                    iter = samples[0].tailSet(otherElement).iterator(); // fast-forward iter0
                    found = false;
                    break; // but now you need to start from iter.hasNext() again
                }
            }
            if (found) c++;

        }
        return c;
    }

    private int[] getNMaxIndex(int[][] ns) {
        int n_max = 0;
        int maxRow = -1;
        int maxCol = -1;
        for (int i = 0; i < ns.length; i++) {
            for (int j = 0; j < ns[i].length; j++) {
                if (ns[i][j] > n_max) {
                    n_max = ns[i][j];
                    maxRow = i;
                    maxCol = j;
                }
            }
        }
        return new int[]{maxRow, maxCol};
    }

    private int getNmax(int[][] ns) {
        int n_max = 0;
        for (int i = 0; i < ns.length; i++) {
            for (int j = 0; j < ns[i].length; j++) {
                if (ns[i][j] > n_max) {
                    n_max = ns[i][j];
                }
            }
        }
        return n_max;
    }

    private ArrayList<long[]> wrapLogRanges(long low, long up) {
        ArrayList<long[]> temp;
        //low += (long) Math.pow(2, Main.dyadicRangeBits - 1);
        //up += (long) Math.pow(2, Main.dyadicRangeBits - 1);
        if (low < 0 && up < 0) {
            temp = getLogRangesArrListNegative(low, up);
        } else if (low < 0) {
            ArrayList<long[]> tempLow = getLogRangesArrListNegative(low, -1);
            ArrayList<long[]> tempUp = getLogRangesArrList(1, up + 1);
            for (long[] i: tempUp) {
                i[0] = i[0] - 1L;// - (long) Math.pow(2, Main.dyadicRangeBits - 1);
                i[1] = i[1] - 1L;// - (long) Math.pow(2, Main.dyadicRangeBits - 1);
            }
            temp = new ArrayList<>(tempLow);
            temp.addAll(tempUp);
        } else{
            temp = getLogRangesArrList(low + 1, up + 1);
            for (long[] i: temp) {
                i[0] = i[0] - 1L;// - (long) Math.pow(2, Main.dyadicRangeBits - 1);
                i[1] = i[1] - 1L;// - (long) Math.pow(2, Main.dyadicRangeBits - 1);
            }
        }
        return temp;
    }

    public ArrayList<long[]> getLogRangesArrListNegative(long startInclusive, long stopInclusive) {
        ArrayList<long[]> logRangesArrList = getLogRangesArrList(-1*stopInclusive, -1*startInclusive);
        for (long[] range: logRangesArrList) {
            long temp = range[0];
            range[0] = -1*range[1];
            range[1] = -1*temp;
        }
        return logRangesArrList;
    }

    private int getIndexOfRange(long[] longs) {
        // get distance between the two elements in longs.
        return (int) (Math.log(longs[1] - longs[0] + 1) / Math.log(2));
    }

    public ArrayList<long[]> getLogRangesArrList(long startInclusive, long stopInclusive) {
        startInclusive--;stopInclusive--;
        long initDiff=stopInclusive-startInclusive+1;
        ArrayList<long[]> result = new ArrayList<>();
        long totalSum = 0;
        long pow = 1;
        for (int j = 0; j <= dyadicRangeBits - 1; j++) {
            if (startInclusive+pow-1>stopInclusive)
                break;
            else if (startInclusive%(pow*2)==0 && startInclusive+pow-1<=stopInclusive) ;
                // do nothing, increase the power further
            else {
                result.add(new long[]{1+startInclusive, 1+startInclusive + pow - 1});
                totalSum+=pow;
                startInclusive+=pow;
            }
            pow*=2;
        }

        pow= (long) Math.pow(2,dyadicRangeBits);
        for (int j=dyadicRangeBits;j>=0;j--) {
            if (startInclusive%pow==0L && startInclusive+pow-1<=stopInclusive) {
                result.add(new long[]{1+startInclusive, 1+startInclusive + pow - 1});
                totalSum+=pow;
                startInclusive+=pow;
            }
            pow/=2;
        }

        if (totalSum != initDiff) {
            System.err.println("Error - no full coverage");
        }
        return result;
    }


//    public long getMemoryUsage() {
//        long maxMemoryUsage = 0;
//        maxMemoryUsage += (long) depth * width * (long) catColMap.length * (maxSize * (dyadicRangeBits + 1) * (maxSize + 3 *32 + 1) + 32);
//        maxMemoryUsage += (long) Main.depth * Main.width * (long) numColMap.length * (maxSize * (maxSize + 3 *32 + 1) + 32);
//
//        long memoryUsage = 0;
//        long memUsageSketch = 0;
//        long memUsageArray = 0;
//        long totalSavedByArrays = 0;
//        long totalSavedByMaxBits = 0;
//        for (int i = 0; i < Main.numAttributes; i++) {
//            if (hasPredicate[i]) {
//                if (Main.rangeQueries) {
//                    for (int j = 0; j < Main.dyadicRangeBits + 1; j++) {
//                        if (maxBits[i] + 1 < Main.dyadicRangeBits - j) { // no need to store empty ranges
//                            totalSavedByMaxBits += CMSketchesRange[i][j].getMemoryUsage();
//                            //System.out.println("No need to store empty range " + (Main.dyadicRangeBits - j) + " for attribute " + i);
//                            continue;
//                        }
//                        memUsageSketch = CMSketchesRange[i][j].getMemoryUsage();
//                        memUsageArray  = getMemUsageArray(j);
//                        if (memUsageArray < memUsageSketch) {
//                            totalSavedByArrays += memUsageSketch - memUsageArray;
//                            /*System.out.println("Array is better for attribute "
//                                    + i + " and range " + j + " by "
//                                    + (memUsageSketch - memUsageArray) + " bits");
//                            System.out.println("Array: " + memUsageArray + " bits");
//                            System.out.println("Sketch: " + memUsageSketch + " bits");*/
//                        }
//                        memoryUsage += Math.min(memUsageArray, memUsageSketch);
//                    }
//                } else {
//                    memoryUsage += CMSketches[i].getMemoryUsage();
//                }
//            }
//        }
//        System.out.println("Total saved by arrays: " + totalSavedByArrays + " bits");
//        System.out.println("Total saved by max bits: " + totalSavedByMaxBits + " bits");
//
//        if (memoryUsage > maxMemoryUsage) {
//            System.err.println("Memory usage is " + (memoryUsage) + " bytes and max memory usage is " + maxMemoryUsage + " bytes");
//        }
//        memUsageSynopsis = memoryUsage;
//        return memoryUsage;
//    }
//
//    private long getMemUsageArray(int j) {
//        long memUsageArray = 0;
//
//        long C = (long) Math.pow(2, j);
//        memUsageArray = C * Main.maxSize * (Main.b + 3 * 32 + 1) + 32;
//        return memUsageArray;
//    }



}
