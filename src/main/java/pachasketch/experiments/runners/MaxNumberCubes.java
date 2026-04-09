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

public class MaxNumberCubes {

    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        System.out.println("PachaSketch max number of cubes experiment...");
        String datasetPath = dataDir+"/lineitem_0.1.csv";

        int[] maxNCubes = new int[]{100_000_000, 10_000_000, 1_000_000, 100_000, 10_000, 1_000, 100};

        PachaSketch pachaSketch = PreparePachaSketch.standardValues(datasetPath, 599_934);
        DataLoaders.loadCSV(pachaSketch, datasetPath, 100_000, 599_934);

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();
        String resourcePath = "queries/" + datasetName + "/";
        String queriesFile = resourcePath + datasetName + "_random.json";

        Files.createDirectories(Path.of(resultsBaseDir+"/maxNCubes"));

        for (int limit : maxNCubes) {
            System.out.println("Running with max number of cubes: " + limit);
            pachaSketch.setMaxNCubes(limit);
            String resultsPath = resultsBaseDir + "/maxNCubes/" + datasetName + "random_maxNCubes_" + limit + ".csv";
            QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
        }
        System.out.println("Finished max number of cubes experiments.");
        System.out.println("=====================================\n");

    }
}
