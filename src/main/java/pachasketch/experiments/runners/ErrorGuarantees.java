package pachasketch.experiments.runners;

import pachasketch.experiments.utils.DatasetMetadata;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.pacha.PachaSketch;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ErrorGuarantees {
    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        System.out.println("PachaSketch varying error guarantees experiment...");
        double[] falsePositiveRates = new double[]{0.0025, 0.005, 0.01, 0.02, 0.04};
        double[] epsilons = new double[]{0.000025, 0.00005, 0.0001, 0.0002, 0.0004};
        double delta = 0.01;
        int levels = 5;

        String datasetPath = dataDir+"/lineitem_0.1.csv";
        for(int i = 0; i < falsePositiveRates.length; i++) {
            runWithGuarantees(falsePositiveRates[i], epsilons[i], delta, levels, datasetPath, resultsBaseDir);
        }
        System.out.println("Finished PachaSketch varying error guarantees experiment.");
        System.out.println("=====================================\n");
    }

    public static void runWithGuarantees(double falsePositiveRate, double eps, double delta, int levels, String datasetPath, String resultsBaseDir) throws IOException {
        System.out.println("Running with error guarantees: p=" + falsePositiveRate + ", eps=" + eps + ", delta=" + delta);
        PachaSketch pachaSketch = PreparePachaSketch.loadWithGuarantees(falsePositiveRate, eps, delta, levels, datasetPath);

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();

        String resourcePath = "queries/" + datasetName + "/";
        String resultsPath = resultsBaseDir+"/guarantees/"+datasetName+"random_eps_"+eps+"_p_"+falsePositiveRate+".csv";

        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsBaseDir+"/guarantees"));
        String queriesFile = resourcePath + datasetName + "_random.json";
        QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);
    }
}
