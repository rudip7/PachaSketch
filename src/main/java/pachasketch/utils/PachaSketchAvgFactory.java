package pachasketch.utils;

import pachasketch.pacha.PachaSketchAvg;
import pachasketch.pacha.PachaSketchSum;
import pachasketch.pacha.baseSketches.*;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;

public class PachaSketchAvgFactory {
    public static PachaSketchAvg buildWithSizeParameters(int[] catColMap, int[] numColMap,
                                                         int levels, int[] bases,
                                                         ADTree adTree, MaterializedCombinations materialized,
                                                         int catIndexK, int catIndexM,
                                                         int numIndexK, int numIndexM,
                                                         int regionIndexK, int regionIndexM,
                                                         int width, int depth) {

        BloomFilter catIndex = new BloomFilter(catIndexK, catIndexM);
        BloomFilter numIndex = new BloomFilter(numIndexK, numIndexM);
        BloomFilter regionIndex = new BloomFilter(regionIndexK, regionIndexM);
        CountMinSketchLong[] baseSketchesSum = new CountMinSketchLong[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = new CountMinSketchLong(width, depth);
        }
        CountMinSketch[] baseSketchesCount = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesCount[i] = new CountMinSketch(width, depth);
        }
        return new PachaSketchAvg(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketchesSum, baseSketchesCount);
    }

    public static PachaSketchAvg buildWithErrorParameters(int[] catColMap, int[] numColMap,
                                                       int levels, int[] bases,
                                                       ADTree adTree, MaterializedCombinations materialized,
                                                       double falsePositiveRate,
                                                       double eps, double delta,
                                                       int nElements) {
        // Calculate the number of updates per element
        int catUpdates = adTree.getNumberUpdates();
        int numUpdates = materialized.getNumCombinations() * levels + 1;
        int regionUpdates = catUpdates * numUpdates;

        double adjustedEps = eps / (catUpdates * materialized.getNumCombinations());

        Filter catIndex = createFilter(falsePositiveRate, (long) nElements * catUpdates);
        Filter numIndex = createFilter(falsePositiveRate, (long) nElements * numUpdates);
        Filter regionIndex = createFilter(falsePositiveRate, (long) nElements * regionUpdates);

        CountMinSketchLong[] baseSketchesSum = new CountMinSketchLong[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = CountMinSketchLong.buildFromGuarantees(adjustedEps, delta);
        }
        CountMinSketch[] baseSketchesCount = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesCount[i] = CountMinSketch.buildFromGuarantees(adjustedEps, delta);
        }

        return new PachaSketchAvg(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketchesSum, baseSketchesCount);
    }

    public static PachaSketchAvg buildWithErrorParametersWD(int[] catColMap, int[] numColMap,
                                                          int levels, int[] bases,
                                                          ADTree adTree, MaterializedCombinations materialized,
                                                          double falsePositiveRate,
                                                            int width, int depth,
                                                          int nElements) {
        // Calculate the number of updates per element
        int catUpdates = adTree.getNumberUpdates();
        int numUpdates = materialized.getNumCombinations() * levels + 1;
        int regionUpdates = catUpdates * numUpdates;

        Filter catIndex = createFilter(falsePositiveRate, (long) nElements * catUpdates);
        Filter numIndex = createFilter(falsePositiveRate, (long) nElements * numUpdates);
        Filter regionIndex = createFilter(falsePositiveRate, (long) nElements * regionUpdates);

        CountMinSketchLong[] baseSketchesSum = new CountMinSketchLong[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = new CountMinSketchLong(width, depth);
        }
        CountMinSketch[] baseSketchesCount = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesCount[i] = new CountMinSketch(width, depth);
        }

        return new PachaSketchAvg(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketchesSum, baseSketchesCount);
    }

    public static Filter createFilter(double falsePositiveRate, long expectedElements){
        long size = (long) Math.ceil(-expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
        if (size > Integer.MAX_VALUE - 8) {
            return ScalableBloomFilter.buildFromGuarantees(falsePositiveRate, expectedElements);
        } else {
            return BloomFilter.buildFromGuarantees(falsePositiveRate, expectedElements);
        }
    }
}
