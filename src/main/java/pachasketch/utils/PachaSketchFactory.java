package pachasketch.utils;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.CountMinSketch;
import pachasketch.pacha.baseSketches.Filter;
import pachasketch.pacha.baseSketches.ScalableBloomFilter;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;

public class PachaSketchFactory {
    public static PachaSketch buildWithSizeParameters(int[] catColMap, int[] numColMap,
                                                      int levels, int[] bases,
                                                      ADTree adTree, MaterializedCombinations materialized,
                                                      int catIndexK, int catIndexM,
                                                      int numIndexK, int numIndexM,
                                                      int regionIndexK, int regionIndexM,
                                                      int width, int depth) {

        BloomFilter catIndex = new BloomFilter(catIndexK, catIndexM);
        BloomFilter numIndex = new BloomFilter(numIndexK, numIndexM);
        BloomFilter regionIndex = new BloomFilter(regionIndexK, regionIndexM);
        CountMinSketch[] baseSketches = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketches[i] = new CountMinSketch(width, depth);
        }
        return new PachaSketch(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketches);
    }

    public static PachaSketch buildWithErrorParameters(int[] catColMap, int[] numColMap,
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

        CountMinSketch[] baseSketches = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketches[i] = CountMinSketch.buildFromGuarantees(adjustedEps, delta);
        }

        return new PachaSketch(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketches);
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
