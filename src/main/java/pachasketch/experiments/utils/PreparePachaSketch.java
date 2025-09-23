package pachasketch.experiments.utils;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class PreparePachaSketch {

    public static int countRowsInCsv(String filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            // Skip the header line and count the remaining lines
            return (int) reader.lines().skip(1).count();
        }
    }

    public static PachaSketch standardValues(String filePath) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        if (filePath.contains("lineitem")) {
            return loadTpch(falsePositiveRate, eps, delta, levels, filePath);
        } else if (filePath.contains("acs")) {
            return loadUSCensus(falsePositiveRate, eps, delta, levels, filePath);
        } else if (filePath.contains("online_retail")) {
            return loadOnlineRetail(falsePositiveRate, eps, delta, levels, filePath);
        } else if (filePath.contains("bank_marketing")) {
            return loadBankMarketing(falsePositiveRate, eps, delta, levels, filePath);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static  PachaSketch forTpchSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/tpch_lineitem.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forTpch(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/tpch_lineitem.json")
                        .getPath()
        );
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

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }


    public static PachaSketch loadTpch(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = countRowsInCsv(filePath);
        PachaSketch pachaSketch = forTpch(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }

    public static PachaSketch forUSCensusSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/acs_folktables.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("AGEP", "PINCP", "PWGTP");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5, 6};
        int[] numColMap = new int[]{7, 8, 9};
        int[] bases = new int[]{2, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forUSCensus(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/acs_folktables.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("AGEP", "PINCP", "PWGTP");
        MaterializedCombinations materialized = MaterializedCombinations.createAllCombinations(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5, 6};
        int[] numColMap = new int[]{7, 8, 9};
        int[] bases = new int[]{2, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch loadUSCensus(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = countRowsInCsv(filePath);
        PachaSketch pachaSketch = forUSCensus(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }

    public static PachaSketch forOnlineRetailSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/online_retail.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("date", "age", "total");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2};
        int[] numColMap = new int[]{3, 4, 5};
        int[] bases = new int[]{4, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forOnlineRetail(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/online_retail.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("date", "age", "total");
        MaterializedCombinations materialized = MaterializedCombinations.createAllCombinations(numCols);

        int[] catColMap = new int[]{0, 1, 2};
        int[] numColMap = new int[]{3, 4, 5};
        int[] bases = new int[]{4, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch loadOnlineRetail(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = countRowsInCsv(filePath);
        PachaSketch pachaSketch = forOnlineRetail(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }

    public static PachaSketch forBankMarketingSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/bank_marketing.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("duration", "balance", "age", "date");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5};
        int[] numColMap = new int[]{6, 7, 8, 9};
        int[] bases = new int[]{4, 5, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forBankMarketing(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromFile(
                PreparePachaSketch.class.getClassLoader()
                        .getResource("ad_trees/bank_marketing.json")
                        .getPath()
        );
        List<String> numCols = Arrays.asList("duration", "balance", "age", "date");
        List<List<String>> combinations = Arrays.asList(
                Arrays.asList("duration"),
                Arrays.asList("balance"),
                Arrays.asList("age"),
                Arrays.asList("date"),

                Arrays.asList("duration", "age"),
                Arrays.asList("duration", "date"),
                Arrays.asList("duration", "balance"),
                Arrays.asList("balance", "age"),
                Arrays.asList("balance", "date"),

                Arrays.asList("duration", "balance", "age"),

                Arrays.asList("duration", "balance", "age", "date")
        );
        MaterializedCombinations materialized = new MaterializedCombinations(numCols, combinations);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5};
        int[] numColMap = new int[]{6, 7, 8, 9};
        int[] bases = new int[]{4, 5, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch loadBankMarketing(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = countRowsInCsv(filePath);
        PachaSketch pachaSketch = forBankMarketing(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }


}
