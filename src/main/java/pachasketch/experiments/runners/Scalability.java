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

public class Scalability {

    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        String[] datasets = new String[]{
                "/lineitem_0.1.csv",
                "/lineitem_0.5.csv",
                "/lineitem_0.5.csv",
                "/lineitem_2.csv",
                "/lineitem_8.csv",
        };

        int[] nElements = new int[]{187_500, 750_000, 3_000_000, 12_000_000, 48_000_000};

        System.out.println("Running scalability experiments...");
        for (int i = 0; i < datasets.length; i++) {
            String dataset = datasets[i];
            runForDataset(nElements[i], dataDir + dataset, resultsBaseDir);
        }
        System.out.println("Finished scalability experiments.");
        System.out.println("=====================================\n");

    }

    public static void runForDataset(int nElements, String datasetPath, String resultsBaseDir) throws IOException {
        System.out.println("Running for dataset: " + datasetPath+" with nElements: "+nElements);

        PachaSketch pachaSketch = PreparePachaSketch.standardValues(datasetPath, nElements);
        DataLoaders.loadCSV(pachaSketch, datasetPath, 100_000, nElements);

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();

        String resourcePath = "queries/" + datasetName + "/";
        String label = (nElements >= 1_000_000) ? (nElements / 1_000_000) + "_M" : (nElements / 1_000) + "_k";
        String resultsPath = resultsBaseDir+"/scalability/"+datasetName+"random_scale_"+label+".csv";

        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsBaseDir+"/scalability"));
        String queriesFile = resourcePath + datasetName + "_random.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
    }


}
