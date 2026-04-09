package pachasketch.experiments.runners;

import pachasketch.experiments.utils.DatasetMetadata;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchFactory;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class ADTreeInfluence {
    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        System.out.println("PachaSketch AD-Tree ordering experiment...");
        String datasetPath = dataDir+"/lineitem_0.1.csv";

        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

//        System.out.println("\nTPCH Lineitem dataset...");
//        PachaSketch pachaSketch = PreparePachaSketch.forTpch(falsePositiveRate, eps, delta, levels, 599_934);
//        System.out.println("Running PachaSketch with AD-Tree ordering...");
//        run(pachaSketch, datasetPath, resultsBaseDir, "pacha_ad_tree_ordering");
//
//        PachaSketch pachaSketchNoOrdering = buildPachaSketchWithADTreeTpch("ad_trees/tpch_lineitem_default.json", new int[]{2, 1, 0, 4, 3});
//        System.out.println("Running PachaSketch with default AD-Tree ordering...");
//        run(pachaSketchNoOrdering, datasetPath, resultsBaseDir, "pacha_default_ad_tree");

        System.out.println("\nUS Census dataset...");
        datasetPath = dataDir+"/acs_folktables.csv";
//        PachaSketch pachaSketchCensus = PreparePachaSketch.forUSCensus(falsePositiveRate, eps, delta, levels, 377_575);
//        System.out.println("Running PachaSketch with AD-Tree ordering...");
//        run(pachaSketchCensus, datasetPath, resultsBaseDir, "pacha_ad_tree_ordering_census");

        PachaSketch pachaSketchCensusNoOrdering = buildPachaSketchWithADTreeUSCensus("ad_trees/acs_folktables_default.json", new int[]{0, 3, 1, 5, 2, 4, 6});
        System.out.println("Running PachaSketch with default AD-Tree ordering...");
        run(pachaSketchCensusNoOrdering, datasetPath, resultsBaseDir, "pacha_default_ad_tree_census");


        System.out.println("Finished AD-Tree ordering experiment.");
        System.out.println("=====================================\n");
    }

    public static PachaSketch buildPachaSketchWithADTreeUSCensus(String adTreePath, int[] catColMap) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        ADTree adTree = ADTree.fromResource(adTreePath);
        List<String> numCols = Arrays.asList("AGEP", "PINCP", "PWGTP");
        MaterializedCombinations materialized = MaterializedCombinations.createAllCombinations(numCols);

//        int[] catColMap = new int[]{0, 3, 1, 5, 2, 4, 6};
        int[] numColMap = new int[]{7, 8, 9};
        int[] bases = new int[]{2, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                377_575
        );
    }

    public static PachaSketch buildPachaSketchWithADTreeTpch(String adTreePath, int[] catColMap) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        ADTree adTree = ADTree.fromResource(adTreePath);
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

//        int[] catColMap = new int[]{2, 1, 0, 4, 3};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                599_934
        );
    }

    public static void run(PachaSketch pachaSketch, String datasetPath, String resultsBaseDir, String filename) throws IOException {
        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();
        String resourcePath = "queries/" + datasetName + "/";
        DataLoaders.loadCSV(pachaSketch, datasetPath, 100_000);

        Files.createDirectories(Path.of(resultsBaseDir+"/adTree"));


        System.out.println("Evaluating skewed random queries...");
        String resultsPath = resultsBaseDir+"/adTree/"+filename+"_skewed.csv";
        String queriesFile = resourcePath + datasetName + "_random_skewed.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);

        System.out.println("Evaluating uniform random queries...");
        resultsPath = resultsBaseDir+"/adTree/"+filename+"_uniform.csv";
        queriesFile = resourcePath + datasetName + "_random_uniform.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
    }
}
