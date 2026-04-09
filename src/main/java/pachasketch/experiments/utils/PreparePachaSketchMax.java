package pachasketch.experiments.utils;

import pachasketch.pacha.PachaSketchMax;
import pachasketch.pacha.PachaSketchSum;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchMaxFactory;
import pachasketch.utils.PachaSketchSumFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PreparePachaSketchMax {


    public static PachaSketchMax forTpch(double falsePositiveRate, int width, int depth, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/tpch_lineitem.json");

        List<String> numCols = Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity");
        List<List<String>> combinations = Arrays.asList(
                Arrays.asList("n_shipdate"),
                Arrays.asList("n_commitdate"),
                Arrays.asList("n_receiptdate"),
                Arrays.asList("n_extendedprice"),
                Arrays.asList("n_quantity"),

                Arrays.asList("n_shipdate", "n_commitdate"),
                Arrays.asList("n_shipdate", "n_receiptdate"),
                Arrays.asList("n_shipdate", "n_quantity"),
                Arrays.asList("n_commitdate", "n_receiptdate"),
                Arrays.asList("n_commitdate", "n_extendedprice"),
                Arrays.asList("n_extendedprice", "n_quantity"),

                Arrays.asList("n_commitdate", "n_receiptdate", "n_extendedprice"),

                Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")
        );

        MaterializedCombinations materialized = new MaterializedCombinations(numCols, combinations);

        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        return PachaSketchMaxFactory.buildWithErrorParametersWD(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, width, depth,
                nElements
        );
    }

    public static PachaSketchMax loadTpchWD(double falsePositiveRate, int width, int depth, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketchMax pachaSketch = forTpch(falsePositiveRate, width, depth, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10, 8);

        return pachaSketch;
    }
}
