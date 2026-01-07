package pachasketch.experiments.runners;

import pachasketch.Sketch;
import pachasketch.experiments.utils.*;
import pachasketch.omni.OmniSketch;
import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.PachaSketchAvg;
import pachasketch.pacha.PachaSketchSum;
import pachasketch.pacha.baseSketches.CountMinSketch;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DifferentAggregates {

    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        String dataset = "/lineitem_0.1.csv";

        System.out.println("Running different aggregates experiments...");

        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;
        PachaSketch pachaSketch = PreparePachaSketch.forTpch(falsePositiveRate, eps, delta, levels, 599_934);
        int width = pachaSketch.baseSketches[0].width * 100;
        int depth = pachaSketch.baseSketches[0].depth;

        PachaSketchSum pachaSketchSum = PreparePachaSketchSum.loadTpchWD(falsePositiveRate, width, depth, levels, dataDir+dataset);
        runDatasetQueries(pachaSketchSum, dataDir+dataset, resultsBaseDir+"/pacha_sum");

        PachaSketchAvg pachaSketchAvg = PreparePachaSketchAvg.loadTpchWD(falsePositiveRate, width, depth, levels, dataDir+dataset);
        runDatasetQueries(pachaSketchAvg, dataDir+dataset, resultsBaseDir+"/pacha_avg");

        System.out.println("Memory Pacha Sketch: "+pachaSketch.getSizeInMB()+" MB");
        System.out.println("Memory Pacha Sum Sketch: "+pachaSketchSum.getSizeInMB()+" MB");
        System.out.println("Memory Pacha Avg Sketch: "+pachaSketchAvg.getSizeInMB()+" MB");

        System.out.println("Finished different aggregates experiments.");
        System.out.println("=====================================\n");
    }

    public static double runPachaSum(String pathToData, String resultsBaseDir) throws IOException {
        PachaSketch pachaSketch = PreparePachaSketch.loadWithStandardValues(pathToData);
        runDatasetQueries(pachaSketch, pathToData, resultsBaseDir);

        return pachaSketch.getSizeInMB();
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
    }

}
