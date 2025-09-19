package pachasketch.utils;

import pachasketch.omni.OmniSketch;
import pachasketch.pacha.PachaSketch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataLoaders {
    public static double loadCSV(PachaSketch pachaSketch, String filePath, int batchSize) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String header = br.readLine(); // Read the header
            if (header == null) {
                throw new IOException("CSV file is empty");
            }
            int numColumns = header.split(",").length; // Determine the number of columns
            String[][] currentBatch = new String[batchSize][numColumns]; // Pre-allocate batch
            String line;
            int currentIndex = 0;
            long recordCount = 0;
            long totalUpdateTime = 0;
            int batchCount = 1;

            while ((line = br.readLine()) != null) {
                currentBatch[currentIndex++] = line.split(",");

                if (currentIndex == batchSize) {
                    System.out.println("Processing batch "+ batchCount+": " + currentIndex);
                    long startTime = System.nanoTime(); // Start timing
                    for (int i = 0; i < currentIndex; i++) {
                        pachaSketch.update(currentBatch[i]);
                    }
                    totalUpdateTime += (System.nanoTime() - startTime); // Accumulate update time

                    recordCount += currentIndex;
                    currentIndex = 0; // Reset index for the next batch
                    batchCount++;
                }
            }

            if (currentIndex > 0){
                // Process remaining records in the batch
                long startTime = System.nanoTime(); // Start timing
                for (int i = 0; i < currentIndex; i++) {
                    pachaSketch.update(currentBatch[i]);
                }
                totalUpdateTime += (System.nanoTime() - startTime); // Accumulate update time
                recordCount += currentIndex;
            }
            // Calculate throughput
            double throughput = (recordCount / (totalUpdateTime / 1_000_000_000.0)); // Records per second

            System.out.println("Processed " + recordCount + " records");
            System.out.println("Total update time: " + (totalUpdateTime / 1_000_000) + " ms");
            System.out.println("Throughput: " + throughput + " records/second");
            return throughput;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static double loadCSV(OmniSketch omniSketch, String filePath, int batchSize) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String header = br.readLine(); // Read the header
            if (header == null) {
                throw new IOException("CSV file is empty");
            }
            int numColumns = header.split(",").length; // Determine the number of columns
            String[][] currentBatch = new String[batchSize][numColumns]; // Pre-allocate batch
            String line;
            int currentIndex = 0;
            int recordCount = 0;
            long totalUpdateTime = 0;
            int batchCount = 1;
            while ((line = br.readLine()) != null) {
                currentBatch[currentIndex++] = line.split(",");
                if (currentIndex == batchSize) {
                    System.out.println("Processing batch "+ batchCount+": " + currentIndex);
                    long startTime = System.nanoTime(); // Start timing
                    for (int i = 0; i < currentIndex; i++) {
                        omniSketch.add(recordCount + i, currentBatch[i]);
                    }
                    totalUpdateTime += (System.nanoTime() - startTime); // Accumulate update time

                    recordCount += currentIndex;
                    currentIndex = 0; // Reset index for the next batch
                    batchCount++;
                }
            }

            if (currentIndex > 0){
                // Process remaining records in the batch
                long startTime = System.nanoTime(); // Start timing
                for (int i = 0; i < currentIndex; i++) {
                    omniSketch.add(recordCount + i, currentBatch[i]);
                }
                totalUpdateTime += (System.nanoTime() - startTime); // Accumulate update time
                recordCount += currentIndex;
            }
            // Calculate throughput
            double throughput = (recordCount / (totalUpdateTime / 1_000_000_000.0)); // Records per second

            System.out.println("Processed " + recordCount + " records");
            System.out.println("Total update time: " + (totalUpdateTime / 1_000_000) + " ms");
            System.out.println("Throughput: " + throughput + " records/second");
            return throughput;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
