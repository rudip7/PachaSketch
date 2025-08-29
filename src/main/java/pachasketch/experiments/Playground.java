package pachasketch.experiments;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchFactory;

import java.io.IOException;

public class Playground {
    public static void main(String[] args) throws IOException {
        // Using Lineitem table from TPC-H SF 0.1
        // Size of Lineitem 0.1
        int nElements = 599_934;

        ADTree adTree = ADTree.fromJson("src/main/resources/ad_trees/tpch_lineitem.json");
        MaterializedCombinations materialized = MaterializedCombinations.fromJson("src/main/resources/relevantCombinations/tpch_lineitem.json");

        // Error parameters
        double falsePositiveRate = 0.01;
        double eps = 0.0005;
        double delta = 0.01;

        // Other parameters
        int levels = 5;
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 5};

        PachaSketch pachaSketch = PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );

        DataLoaders.loadCSV(pachaSketch, "src/main/resources/data/lineitem_0.1.csv", nElements / 10);


    }
}
