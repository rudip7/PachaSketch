package pachasketch.utils;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.CountMinSketch;
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

        double adjustedEps = eps / regionUpdates;

        BloomFilter catIndex = BloomFilter.buildFromGuarantees(falsePositiveRate, nElements * catUpdates);
        BloomFilter numIndex = BloomFilter.buildFromGuarantees(falsePositiveRate, nElements * numUpdates);
        BloomFilter regionIndex = BloomFilter.buildFromGuarantees(falsePositiveRate, nElements * regionUpdates);

        CountMinSketch[] baseSketches = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketches[i] = CountMinSketch.buildFromGuarantees(adjustedEps, delta);
        }

        return new PachaSketch(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketches);
    }
}
