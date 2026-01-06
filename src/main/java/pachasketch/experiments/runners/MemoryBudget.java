package pachasketch.experiments.runners;

import pachasketch.experiments.utils.DatasetMetadata;
import pachasketch.experiments.utils.PrepareOmniSketch;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.omni.OmniSketch;
import pachasketch.pacha.PachaSketch;
import pachasketch.utils.QuerySetEvaluator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MemoryBudget {
    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        int[] memBudgets = new int[]{64, 128, 256, 512, 1024}; // in MB
        int[] maxNCubes = new int[]{1_000, 25_000, 100_000, 1_000_000, 1_000_000};
        int[] nHashIndex = new int[]{1, 1, 2, 2, 3};

        int[] limits = new int[]{3_000_000, 3_000_000, 3_000_000, 3_000_000, 3_000_000};
        int batchSize = 100_000;

        System.out.println("New!: Running memory budget experiments...");
        System.out.println("PachaSketch:");
        String datasetPath = dataDir+"/lineitem_8.csv";
        for (int i = 0; i < memBudgets.length; i++) {
            runPacha(memBudgets[i], maxNCubes[i], nHashIndex[i], limits[i], batchSize, datasetPath, resultsBaseDir);
        }

        System.out.println("OmniSketch:");
        for (int i = 0; i < memBudgets.length; i++) {
            runOmni(memBudgets[i], limits[i], batchSize, datasetPath, resultsBaseDir);
        }
        System.out.println("Finished memory budget experiments.");
        System.out.println("=====================================\n");
    }

    public static void runPacha(int memBudget, int maxNCubes, int nHashIndex, int limit, int batchSize, String datasetPath, String resultsBaseDir) throws IOException {
        System.out.println("Running for dataset: " + datasetPath+", memory budget: " + memBudget + " MB");

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();

        String resourcePath = "queries/" + datasetName + "/";
        String queriesFile = resourcePath + datasetName + "_random.json";

        Files.createDirectories(Path.of(resultsBaseDir+"/memory_budget/pacha"));
        if (!datasetName.equals("tpch")){
            throw new IllegalArgumentException("For now only TPC-H Lineitem is supported");
        }
        PachaSketch pachaSketch = PreparePachaSketch.forTpchWithMemoryBudget(memBudget, maxNCubes, nHashIndex);
        try (BufferedReader br = new BufferedReader(new FileReader(datasetPath))) {
            String header = br.readLine(); // Read the header
            if (header == null) {
                throw new IOException("CSV file is empty");
            }

            int numColumns = header.split(",").length; // Determine the number of columns
            String[][] currentBatch = new String[batchSize][numColumns]; // Pre-allocate batch
            String line;
            int currentIndex = 0;
            int recordCount = 0;
            int batchCount = 1;

            while ((line = br.readLine()) != null && (limit < 0 || recordCount < limit)) {
                currentBatch[currentIndex++] = line.split(",");
                recordCount++;

                if (currentIndex == batchSize) {
                    System.out.println("Processing batch "+ batchCount);
                    for (int i = 0; i < currentIndex; i++) {
                        pachaSketch.update(currentBatch[i]);
                    }
                    String resultsPath = resultsBaseDir+"/memory_budget/pacha/"+datasetName+"memory_"+memBudget+"_MB_"+batchCount+".csv";

                    System.out.println("Evaluating random queries...");
                    QuerySetEvaluator.evaluateQuerySetFromResource(pachaSketch, queriesFile, resultsPath);

                    currentIndex = 0; // Reset index for the next batch
                    batchCount++;
                }
            }

        }  catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void runOmni(int memBudget, int limit, int batchSize, String datasetPath, String resultsBaseDir) throws IOException {
        System.out.println("Running for dataset: " + datasetPath+", memory budget: " + memBudget + " MB");

        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        String datasetName = datasetMetadata.getDatasetName();

        String resourcePath = "queries/" + datasetName + "/";
        String queriesFile = resourcePath + datasetName + "_random.json";

        Files.createDirectories(Path.of(resultsBaseDir+"/memory_budget/omni"));

        if (!datasetName.equals("tpch")){
            throw new IllegalArgumentException("For now only TPC-H Lineitem is supported");
        }
        double eps = 0.1;
        double delta = 0.1;
        OmniSketch omniSketch = PrepareOmniSketch.forTpch(eps, delta, memBudget);

        try (BufferedReader br = new BufferedReader(new FileReader(datasetPath))) {
            String header = br.readLine(); // Read the header
            if (header == null) {
                throw new IOException("CSV file is empty");
            }

            int numColumns = header.split(",").length; // Determine the number of columns
            String[][] currentBatch = new String[batchSize][numColumns]; // Pre-allocate batch
            String line;
            int currentIndex = 0;
            int recordCount = 0;
            int batchCount = 1;

            while ((line = br.readLine()) != null && (limit < 0 || recordCount < limit)) {
                currentBatch[currentIndex++] = line.split(",");
                recordCount++;

                if (currentIndex == batchSize) {
                    System.out.println("Processing batch "+ batchCount);
                    for (int i = 0; i < currentIndex; i++) {
                        omniSketch.add(recordCount - (currentIndex - i), currentBatch[i]);
                    }
                    String resultsPath = resultsBaseDir+"/memory_budget/omni/"+datasetName+"memory_"+memBudget+"_MB_"+batchCount+".csv";

                    System.out.println("Evaluating random queries...");
                    QuerySetEvaluator.evaluateQuerySetFromResource(omniSketch, queriesFile, resultsPath);

                    currentIndex = 0; // Reset index for the next batch
                    batchCount++;
                }
            }

        }  catch (IOException e) {
            e.printStackTrace();
        }
    }
}
