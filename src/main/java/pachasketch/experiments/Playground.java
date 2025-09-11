package pachasketch.experiments;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchFactory;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Playground {
    public static void main(String[] args) throws IOException {
        // Using Lineitem table from TPC-H SF 0.1
        // Size of Lineitem 0.1
        int nElements = 599_934;

        ADTree adTree = ADTree.fromFile("src/main/resources/ad_trees/tpch_lineitem.json");
        MaterializedCombinations materialized = MaterializedCombinations.fromFile("src/main/resources/relevantCombinations/tpch_lineitem.json");

        // Error parameters
        double falsePositiveRate = 0.01;
        double eps = 0.0005;
        double delta = 0.01;

        // Other parameters
        int levels = 5;
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        PachaSketch pachaSketch = PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );

        DataLoaders.loadCSV(pachaSketch, "src/main/resources/data/lineitem_0.1.csv", nElements / 10);

//        pachaSketch.saveAsJson("src/main/resources/skethes/tpch_pacha_sketch.json");

        QuerySetEvaluator.evaluateQuerySet(pachaSketch, "src/main/resources/queries/tpch/tpch_random.json", "src/main/resources/results/pacha/tpch/tpch_random.csv");
    }
}
