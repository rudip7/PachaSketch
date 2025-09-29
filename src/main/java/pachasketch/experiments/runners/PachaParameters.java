package pachasketch.experiments.runners;

import pachasketch.experiments.utils.DatasetMetadata;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.pacha.PachaSketch;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class PachaParameters {
    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        System.out.println("PachaSketch varying parameters experiment...");
        String datasetPath = dataDir+"/lineitem_0.1.csv";

//        int[] levelsToTest = new int[]{3, 5, 7, 9};
//        for (int levels : levelsToTest) {
//            runDifferentLevels(levels, datasetPath, resultsBaseDir);
//        }

        int[][] basesToTest = new int[][]{
                {2, 2, 2, 2, 2},
                {5, 5, 5, 5, 5},
                {5, 5, 5, 10, 2},
                {10, 10, 10, 10, 10}
        };

        for (int[] bases : basesToTest) {
            runDifferentBases(bases, datasetPath, resultsBaseDir);
        }

        System.out.println("Finished PachaSketch varying parameters experiment.");
        System.out.println("=====================================\n");


    }

    public static void runDifferentLevels(int levels, String datasetPath, String resultsBaseDir) throws IOException {
        System.out.println("Running with levels: " + levels);
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;

        PachaSketch pachaSketch = PreparePachaSketch.loadWithGuarantees(falsePositiveRate, eps, delta, levels, datasetPath);

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();
        String resourcePath = "queries/" + datasetName + "/";
        String resultsPath = resultsBaseDir+"/parameters/levels/"+datasetName+"random_levels_"+levels+".csv";

        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsBaseDir+"/parameters/levels"));
        String queriesFile = resourcePath + datasetName + "_random.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
    }

    public static void runDifferentBases(int[] bases, String datasetPath, String resultsBaseDir) throws IOException {
        System.out.println("Running with bases: " + Arrays.toString(bases));
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        PachaSketch pachaSketch = PreparePachaSketch.withGuarantees(falsePositiveRate, eps, delta, levels, datasetPath);
        pachaSketch.setBases(bases);
        DataLoaders.loadCSV(pachaSketch, datasetPath, 100_000);

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();
        String resourcePath = "queries/" + datasetName + "/";
        String label = Arrays.stream(bases).mapToObj(String::valueOf).reduce((a, b) -> a + "_" + b).orElse("");
        String resultsPath = resultsBaseDir+"/parameters/bases/"+datasetName+"random_bases_"+label+".csv";

        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsBaseDir+"/parameters/bases"));
        String queriesFile = resourcePath + datasetName + "_random.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
    }

}
