package pachasketch.experiments.runners;

import pachasketch.Sketch;
import pachasketch.experiments.utils.DatasetMetadata;
import pachasketch.experiments.utils.PrepareOmniSketch;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.omni.OmniSketch;
import pachasketch.pacha.PachaSketch;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DifferentDatasets {

    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        String[] datasets = new String[]{
                "/lineitem_0.1.csv",
                "/acs_folktables.csv",
                "/online_retail.csv",
                "/bank_marketing.csv"
        };

        System.out.println("Running different datasets experiments...");
        for (String dataset : datasets) {
            System.out.println("Running dataset: " + dataset);
            double pachaSize = runPacha(dataDir+dataset, resultsBaseDir+"/pacha");
            runOmni(pachaSize, dataDir+dataset, resultsBaseDir+"/omni");
        }
        System.out.println("Finished different datasets experiments.");
        System.out.println("=====================================\n");
    }

    public static double runPacha(String pathToData, String resultsBaseDir) throws IOException {
        PachaSketch pachaSketch = PreparePachaSketch.loadWithStandardValues(pathToData);
        runDatasetQueries(pachaSketch, pathToData, resultsBaseDir);

        return pachaSketch.getSizeInMB();
    }

    public static void runOmni(double memBudget, String pathToData, String resultsBaseDir) throws IOException {
        OmniSketch omniSketch = PrepareOmniSketch.loadWithStandardValues(memBudget, pathToData);
        runDatasetQueries(omniSketch, pathToData, resultsBaseDir);
    }

    public static void runDatasetQueries(Sketch sketch, String pathToData, String resultsBaseDir) throws IOException {
        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(pathToData);
        String datasetName = datasetMetadata.getDatasetName();
        int nCat = datasetMetadata.getnCat();
        int nNum = datasetMetadata.getnNum();
        int nDominant = Math.max(nCat, nNum);


        String baseResourcePath = "queries/" + datasetName + "/";
        String resultsDir = resultsBaseDir+"/"+datasetName;

        // Random queries
        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsDir));
        String queriesFile = baseResourcePath + datasetName + "_random.json";
        String resultFile = resultsDir+"/"+datasetName+"_random.csv";
        QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);

        // Selectivity-based queries
        System.out.println("Evaluating selectivity-based queries...");
        Files.createDirectories(Path.of(resultsDir+"/selectivities"));
        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
        for (double sel : selectivities) {
            queriesFile = String.format(baseResourcePath+"selectivities/"+datasetName+"_sel_%.2f.json", sel);
            resultFile = String.format(resultsDir+"/selectivities/"+datasetName+"_sel_%.2f.csv", sel);
            QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);
        }

        // Categorical queries
        System.out.println("Evaluating categorical queries...");
        Files.createDirectories(Path.of(resultsDir+"/categorical"));
        for (int i = 1; i <= nCat; i++) {
            queriesFile = String.format(baseResourcePath+"categorical/"+datasetName+"_cat_%d.json", i);
            resultFile = String.format(resultsDir+"/categorical/"+datasetName+"_cat_%d.csv", i);
            QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);
        }

        // Numerical queries
        System.out.println("Evaluating numerical queries...");
        Files.createDirectories(Path.of(resultsDir+"/numerical"));
        for (int i = 1; i <= nNum; i++) {
            queriesFile = String.format(baseResourcePath+"numerical/"+datasetName+"_num_%d.json", i);
            resultFile = String.format(resultsDir+"/numerical/"+datasetName+"_num_%d.csv", i);
            QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);
        }

        // Mixed queries
        System.out.println("Evaluating mixed queries...");
        Files.createDirectories(Path.of(resultsDir+"/mixed"));
        for (int i = 1; i <= nDominant; i++) {
            queriesFile = String.format(baseResourcePath+"mixed/"+datasetName+"_mix_%d.json", i);
            resultFile = String.format(resultsDir+"/mixed/"+datasetName+"_mix_%d.csv", i);
            QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);
        }

    }

}
