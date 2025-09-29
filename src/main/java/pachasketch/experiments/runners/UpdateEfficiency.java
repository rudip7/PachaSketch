package pachasketch.experiments.runners;

import pachasketch.experiments.utils.DatasetMetadata;
import pachasketch.experiments.utils.PrepareOmniSketch;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.omni.OmniSketch;
import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.utils.PachaQueryResult;
import pachasketch.pacha.utils.QueryStats;
import pachasketch.utils.DataLoaders;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.Files.createDirectories;

public class UpdateEfficiency {
    public static void run(String dataDir, String resultsBaseDir) throws IOException {
        String[] datasets = new String[]{
//                "/lineitem_0.1.csv",
                "/acs_folktables.csv",
                "/online_retail.csv",
                "/bank_marketing.csv"
        };

        System.out.println("Running update efficiency experiments...");
        for (String dataset : datasets) {
            System.out.println("Running dataset: " + dataset);
            String datasetPath = dataDir + dataset;
            runForDataset(datasetPath, resultsBaseDir);
        }
        System.out.println("Finished update efficiency experiments.");
        System.out.println("=====================================\n");
    }

    public static void runForDataset(String datasetPath, String resultsBaseDir) throws IOException {
        DatasetMetadata datasetMetadata = Utils.getDatasetMetadata(datasetPath);
        int nElements = Utils.countRowsInCsv(datasetPath);

        double pachaSketchSize = PreparePachaSketch.standardValues(datasetPath).getSizeInMB();

        int[] partitions = new int[]{10, 1};

        System.out.println("Measuring throughputs for OmniSketch");
        double[] throughputsOmni = new double[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            System.out.println(partitions[i]+" partitions");
            OmniSketch omniSketch = PrepareOmniSketch.standardValues(pachaSketchSize, datasetPath);
            throughputsOmni[i] = DataLoaders.loadCSV(omniSketch, datasetPath, nElements / partitions[i]);
        }

        System.out.println("Measuring throughputs for PachaSketch");
        double[] throughputsPacha = new double[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            System.out.println(partitions[i]+" partitions");
            PachaSketch pachaSketch = PreparePachaSketch.standardValues(datasetPath);
            throughputsPacha[i] = DataLoaders.loadCSV(pachaSketch, datasetPath, nElements / partitions[i]);
        }

        System.out.println("Measuring throughputs for PachaSketch (single combination)");
        double[] throughputsPachaSingle = new double[partitions.length];
        for (int i = 0; i < partitions.length; i++) {
            System.out.println(partitions[i]+" partitions");
            PachaSketch pachaSketch = PreparePachaSketch.standardValues(datasetPath);
            pachaSketch.limitToSingleCombination();
            throughputsPachaSingle[i] = DataLoaders.loadCSV(pachaSketch, datasetPath, nElements / partitions[i]);
        }

        String resultFile = resultsBaseDir + "/update_efficiency/"+datasetMetadata.getDatasetName()+"_throughputs.csv";
        Files.createDirectories(Path.of(resultsBaseDir + "/update_efficiency"));
        writePachaResultsToFile(throughputsPacha, throughputsPachaSingle, throughputsOmni, resultFile);
    }

    private static void writePachaResultsToFile(double[] throughputsPacha, double[] throughputsPachaSingle, double[] throughputsOmni, String resultFile) throws IOException {
        String header = "pacha_sketch,pacha_sketch_single,omni_sketch";

        createDirectories(Paths.get(resultFile).getParent());
        try (FileWriter writer = new FileWriter(resultFile)) {
            // Write the header
            writer.write(header + "\n");

            for (int i = 0; i < throughputsPacha.length; i++) {
                StringBuilder row = new StringBuilder();

                row.append(throughputsPacha[i]).append(",")
                        .append(throughputsPachaSingle[i]).append(",")
                        .append(throughputsOmni[i]).append(",");
                writer.write(row.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write results to file: " + resultFile, e);
        }
    }


}
