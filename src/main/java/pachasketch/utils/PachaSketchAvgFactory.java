package pachasketch.utils;

import pachasketch.pacha.PachaSketchAvg;
import pachasketch.pacha.PachaSketchSum;
import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.CountSketch;
import pachasketch.pacha.baseSketches.Filter;
import pachasketch.pacha.baseSketches.ScalableBloomFilter;
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
        CountSketch[] baseSketchesSum = new CountSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = new CountSketch(width, depth);
        }
        CountSketch[] baseSketchesCount = new CountSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesCount[i] = new CountSketch(width, depth);
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

        CountSketch[] baseSketchesSum = new CountSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = CountSketch.buildFromGuarantees(adjustedEps, delta);
        }
        CountSketch[] baseSketchesCount = new CountSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesCount[i] = CountSketch.buildFromGuarantees(adjustedEps, delta);
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

        CountSketch[] baseSketchesSum = new CountSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = new CountSketch(width, depth);
        }
        CountSketch[] baseSketchesCount = new CountSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesCount[i] = new CountSketch(width, depth);
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
