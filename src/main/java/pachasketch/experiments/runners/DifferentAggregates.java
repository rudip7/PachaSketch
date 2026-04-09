package pachasketch.experiments.runners;

import pachasketch.Sketch;
import pachasketch.experiments.utils.*;
import pachasketch.omni.OmniSketch;
import pachasketch.pacha.*;
import pachasketch.pacha.baseSketches.CountMinSketch;
import pachasketch.sampling.PrioritySampler;
import pachasketch.utils.DataLoaders;
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
        int width = pachaSketch.baseSketches[0].width;
        int depth = pachaSketch.baseSketches[0].depth;

//        PachaSketchSum pachaSketchSum = PreparePachaSketchSum.loadTpchWD(falsePositiveRate, width, depth, levels, dataDir+dataset);
//        runDatasetQueries(pachaSketchSum, dataDir+dataset, resultsBaseDir+"/aggregates", "sums");
////
//        PachaSketchAvg pachaSketchAvg = PreparePachaSketchAvg.loadTpchWD(falsePositiveRate, width, depth, levels, dataDir+dataset);
//        runDatasetQueries(pachaSketchAvg, dataDir+dataset, resultsBaseDir+"/aggregates", "avgs");

        PachaSketchMin pachaSketchMin = PreparePachaSketchMin.loadTpchWD(falsePositiveRate, width, depth, levels, dataDir+dataset);
        runDatasetQueries(pachaSketchMin, dataDir+dataset, resultsBaseDir+"/aggregates", "mins");

        PachaSketchMax pachaSketchMax = PreparePachaSketchMax.loadTpchWD(falsePositiveRate, width, depth, levels, dataDir+dataset);
        runDatasetQueries(pachaSketchMax, dataDir+dataset, resultsBaseDir+"/aggregates", "maxs");

//        For now hard-coded TPCH
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int sampleSize = 15_000;
        PrioritySampler samplerCount = new PrioritySampler(sampleSize, catColMap, numColMap);
        PrioritySampler samplerSum = new PrioritySampler(sampleSize, catColMap, numColMap);
//        int n = 47_994_720;
//        DataLoaders.loadCSV(samplerCount, dataDir+dataset, n/10);
//        runDatasetQueries(samplerCount, dataDir+dataset, resultsBaseDir+"/aggregates/8", "count");
//
//        DataLoaders.loadCSV(samplerSum, dataDir+dataset, n/10, 8);
//        runDatasetQueries(samplerSum, dataDir+dataset, resultsBaseDir+"/aggregates/8", "sum");



        System.out.println("Memory Pacha Sketch: "+pachaSketch.getSizeInMB()+" MB");
//        System.out.println("Memory Pacha Sum Sketch: "+pachaSketchSum.getSizeInMB()+" MB");
//        System.out.println("Memory Pacha Avg Sketch: "+pachaSketchAvg.getSizeInMB()+" MB");
        System.out.println("Memory Priority Sampler: "+(samplerCount.getSizeInMB())+" MB");

        System.out.println("Finished different aggregates experiments.");
        System.out.println("=====================================\n");
    }


    public static void runDatasetQueries(Sketch sketch, String pathToData, String resultsBaseDir, String aggType) throws IOException {
        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(pathToData);
        String datasetName = datasetMetadata.getDatasetName();

        String baseResourcePath = "queries/" + datasetName + "/";
        String resultsDir = resultsBaseDir+"/"+datasetName;

        // Random queries
        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsDir));
        String queriesFile = baseResourcePath + datasetName + "_random.json";
        String resultFile = resultsDir+"/"+datasetName+"_"+aggType+".csv";


        QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);

        if (!(sketch instanceof PachaSketchSum)){
            return;
        }
        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
        for (double sel : selectivities) {
            queriesFile = String.format(baseResourcePath+"selectivities/"+datasetName+"_sel_%.2f.json", sel);
            resultFile = String.format(resultsDir+"/selectivities/"+datasetName+"_sum_sel_%.2f.csv", sel);
            QuerySetEvaluator.evaluateQuerySetFromResource(sketch, queriesFile, resultFile);
        }


    }

    public static void runDatasetQueries(PrioritySampler sampler, String pathToData, String resultsBaseDir, String aggType) throws IOException {
        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(pathToData);
        String datasetName = datasetMetadata.getDatasetName();

        String baseResourcePath = "queries/" + datasetName + "/";
        String resultsDir = resultsBaseDir+"/"+datasetName;

        // Random queries
        System.out.println("Evaluating random queries...");
        Files.createDirectories(Path.of(resultsDir));
        String queriesFile = baseResourcePath + datasetName + "_random.json";
        String resultFile = resultsDir+"/"+datasetName+"_sample_"+aggType+".csv";

        QuerySetEvaluator.evaluateQuerySetSamplerFromResource(sampler, queriesFile, resultFile);

//        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
//        for (double sel : selectivities) {
//            queriesFile = String.format(baseResourcePath+"selectivities/"+datasetName+"_sel_%.2f.json", sel);
//            resultFile = String.format(resultsDir+"/selectivities/"+datasetName+"_sample_"+aggType+"_sel_%.2f.csv", sel);
//            QuerySetEvaluator.evaluateQuerySetSamplerFromResource(sampler, queriesFile, resultFile);
//        }
    }

}
