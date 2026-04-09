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

public class ProjectedSubSpaces {
    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        System.out.println("PachaSketch projected subspaces experiment...");
        String datasetPath = dataDir+"/lineitem_0.1.csv";

        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        ADTree adTree = ADTree.fromResource("ad_trees/tpch_lineitem.json");
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        List<String> numCols = Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity");

        List<List<String>> combinations= Arrays.asList(
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

        MaterializedCombinations materializedAll = MaterializedCombinations.createAllCombinations(numCols);

        List<List<String>> combinationsNoSubSpaces = Arrays.asList(
                Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")
        );
        MaterializedCombinations materializedNoSubSpaces = new MaterializedCombinations(numCols, combinationsNoSubSpaces);


        List<List<String>> combinationsSingle = Arrays.asList(
                Arrays.asList("n_shipdate"),
                Arrays.asList("n_commitdate"),
                Arrays.asList("n_receiptdate"),
                Arrays.asList("n_extendedprice"),
                Arrays.asList("n_quantity"),

                Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")
        );
        MaterializedCombinations materializedSingle = new MaterializedCombinations(numCols, combinationsSingle);

        List<List<String>> combinationsDefault = Arrays.asList(
                Arrays.asList("n_shipdate"),
                Arrays.asList("n_commitdate"),
                Arrays.asList("n_receiptdate"),
                Arrays.asList("n_extendedprice"),
                Arrays.asList("n_quantity"),

                Arrays.asList("n_shipdate", "n_commitdate"),
                Arrays.asList("n_shipdate", "n_receiptdate"),
                Arrays.asList("n_shipdate", "n_extendedprice"),
                Arrays.asList("n_shipdate", "n_quantity"),
                Arrays.asList("n_commitdate", "n_receiptdate"),
                Arrays.asList("n_commitdate", "n_extendedprice"),
                Arrays.asList("n_commitdate", "n_quantity"),
                Arrays.asList("n_receiptdate", "n_extendedprice"),
                Arrays.asList("n_receiptdate", "n_quantity"),
                Arrays.asList("n_extendedprice", "n_quantity"),

                Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")
        );

        MaterializedCombinations materializedDefault = new MaterializedCombinations(numCols, combinationsDefault);



        List<MaterializedCombinations> materializedList = Arrays.asList(materialized, materializedNoSubSpaces, materializedSingle, materializedDefault, materializedAll);
        List<String> materializedNames = Arrays.asList("custom", "none", "single", "default", "all");

        for (int i = 0; i < materializedList.size(); i++) {
            MaterializedCombinations mat = materializedList.get(i);
            String name = materializedNames.get(i);
            PachaSketch pachaSketch = PachaSketchFactory.buildWithErrorParameters(
                    catColMap, numColMap, levels, bases,
                    adTree, mat,
                    falsePositiveRate, eps, delta,
                    599_934
            );
            System.out.println("Running PachaSketch with materialized combinations: " + name);
            run(pachaSketch, datasetPath, resultsBaseDir, name);
        }

        System.out.println("Finished projected subspaces experiment.");
        System.out.println("=====================================\n");
    }

    public static void run(PachaSketch pachaSketch, String datasetPath, String resultsBaseDir, String filename) throws IOException {
        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();
        String resourcePath = "queries/" + datasetName + "/";
        DataLoaders.loadCSV(pachaSketch, datasetPath, 100_000);

        Files.createDirectories(Path.of(resultsBaseDir+"/subspaces"));


        System.out.println("Evaluating workload random queries...");
        String resultsPath = resultsBaseDir+"/subspaces/mat_subspaces_"+filename+"_workload.csv";
        String queriesFile = resourcePath + datasetName + "_random_mat_workload.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);

//        System.out.println("Evaluating uniform random queries...");
//        resultsPath = resultsBaseDir+"/subspaces/mat_subspaces_"+filename+"_uniform.csv";
//        queriesFile = resourcePath + datasetName + "_random_mat_uniform.json";
//        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
    }
}
