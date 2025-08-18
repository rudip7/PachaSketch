package pachasketch.pacha;

import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.CountMinSketch;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;

import java.util.List;

public class PachaSketchFactory {
    public static PachaSketch createSizeParameters(int levels, int[] catColMap, int[] numColMap,
                                                 int[] bases, ADTree adTree, MaterializedCombinations materialized,
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
}
