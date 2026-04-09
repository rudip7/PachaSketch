package pachasketch.utils;

import pachasketch.pacha.PachaSketchMax;
import pachasketch.pacha.PachaSketchMin;
import pachasketch.pacha.baseSketches.*;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;

public class PachaSketchMinFactory {
    public static PachaSketchMin buildWithSizeParameters(int[] catColMap, int[] numColMap,
                                                         int levels, int[] bases,
                                                         ADTree adTree, MaterializedCombinations materialized,
                                                         int catIndexK, int catIndexM,
                                                         int numIndexK, int numIndexM,
                                                         int regionIndexK, int regionIndexM,
                                                         int width, int depth) {

        BloomFilter catIndex = new BloomFilter(catIndexK, catIndexM);
        BloomFilter numIndex = new BloomFilter(numIndexK, numIndexM);
        BloomFilter regionIndex = new BloomFilter(regionIndexK, regionIndexM);
        MinSketch[] baseSketches = new MinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketches[i] = new MinSketch(width, depth);
        }
        return new PachaSketchMin(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketches);
    }

    public static PachaSketchMin buildWithErrorParametersWD(int[] catColMap, int[] numColMap,
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

        MinSketch[] baseSketchesSum = new MinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketchesSum[i] = new MinSketch(width, depth);
        }

        return new PachaSketchMin(levels, catColMap, numColMap, bases, adTree, materialized,
                catIndex, numIndex, regionIndex, baseSketchesSum);
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
