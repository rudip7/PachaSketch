package pachasketch.experiments.utils;

import pachasketch.pacha.PachaSketchAvg;
import pachasketch.pacha.PachaSketchSum;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchAvgFactory;
import pachasketch.utils.PachaSketchSumFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PreparePachaSketchAvg {

    public static PachaSketchAvg standardValues(String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        return standardValues(filePath, nElements);
    }

    public static PachaSketchAvg standardValues(String filePath, int nElements) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.01;
        double delta = 0.01;
        int levels = 5;

        if (filePath.contains("lineitem")) {
            return forTpch(falsePositiveRate, eps, delta, levels, nElements);
//        } else if (filePath.contains("acs")) {
//            return forUSCensus(falsePositiveRate, eps, delta, levels, nElements);
//        } else if (filePath.contains("online_retail")) {
//            return forOnlineRetail(falsePositiveRate, eps, delta, levels, nElements);
//        } else if (filePath.contains("bank_marketing")) {
//            return forBankMarketing(falsePositiveRate, eps, delta, levels, nElements);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static PachaSketchAvg withGuarantees(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        if (filePath.contains("lineitem")) {
            return forTpch(falsePositiveRate, eps, delta, levels, nElements);
//        } else if (filePath.contains("acs")) {
//            return forUSCensus(falsePositiveRate, eps, delta, levels, nElements);
//        } else if (filePath.contains("online_retail")) {
//            return forOnlineRetail(falsePositiveRate, eps, delta, levels, nElements);
//        } else if (filePath.contains("bank_marketing")) {
//            return forBankMarketing(falsePositiveRate, eps, delta, levels, nElements);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static PachaSketchAvg loadWithStandardValues(String filePath) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        return loadWithGuarantees(falsePositiveRate, eps, delta, levels, filePath);
    }

    public static PachaSketchAvg loadWithGuarantees(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        if (filePath.contains("lineitem")) {
            return loadTpch(falsePositiveRate, eps, delta, levels, filePath);
//        } else if (filePath.contains("acs")) {
//            return loadUSCensus(falsePositiveRate, eps, delta, levels, filePath);
//        } else if (filePath.contains("online_retail")) {
//            return loadOnlineRetail(falsePositiveRate, eps, delta, levels, filePath);
//        } else if (filePath.contains("bank_marketing")) {
//            return loadBankMarketing(falsePositiveRate, eps, delta, levels, filePath);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static PachaSketchAvg forTpch(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
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

        return PachaSketchAvgFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }


    public static PachaSketchAvg loadTpch(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketchAvg pachaSketch = forTpch(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10, 8);

        return pachaSketch;
    }

    public static PachaSketchAvg forTpch(double falsePositiveRate, int width, int depth, int levels, int nElements) throws IOException {
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

        return PachaSketchAvgFactory.buildWithErrorParametersWD(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, width, depth,
                nElements
        );
    }

    public static PachaSketchAvg loadTpchWD(double falsePositiveRate, int width, int depth, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketchAvg pachaSketch = forTpch(falsePositiveRate, width, depth, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10, 8);

        return pachaSketch;
    }
}
