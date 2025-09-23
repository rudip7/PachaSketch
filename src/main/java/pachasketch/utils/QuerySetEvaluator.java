package pachasketch.utils;

import pachasketch.omni.OmniSketch;
import pachasketch.omni.utils.OmniQueryResult;
import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.utils.PachaQueryResult;
import pachasketch.pacha.utils.QueryStats;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.createDirectories;

public class QuerySetEvaluator {
    public static void evaluateQuerySet(PachaSketch pachaSketch, String querySetFile, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromJSON(querySetFile);
        List<PachaQueryResult> results = new ArrayList<>(queries.size());
        int queryIndex = 0;
        for(List<Object> query : queries){
            try {
                long startTime = System.nanoTime();
                PachaQueryResult result = pachaSketch.query(query, true, false);
                long queryTime = System.nanoTime() - startTime;
                result.setRuntime(queryTime / 1_000_000.0); // Convert to milliseconds
                results.add(result);
            } catch (Exception e) {
                String[] split = querySetFile.split("/");
                String querySetName = split[split.length - 1];
                throw new RuntimeException("Error processing query at index " + queryIndex + " from set "+querySetName+" : " + query, e);
            }
            queryIndex++;
        }

        writePachaResultsToFile(results, resultFile);
    }

    private static void writePachaResultsToFile(List<PachaQueryResult> results, String resultFile) throws IOException {
        String header = "estimates,runtime,forced,relevant_nodes,cat_regions,b_adic_cubes,num_regions,candidate_regions,query_regions";
        int nLevels = results.get(0).stats().queriesPerLevel().length;
        for (int i = 0; i < nLevels; i++) {
            header += ",level_" + i + "_queries";
        }

        createDirectories(Paths.get(resultFile).getParent());
        try (FileWriter writer = new FileWriter(resultFile)) {
            // Write the header
            writer.write(header + "\n");

            // Write each QueryResult as a row
            for (PachaQueryResult result : results) {
                StringBuilder row = new StringBuilder();

                // Add basic fields
                row.append(result.estimate()).append(",")
                        .append(result.getRuntime()).append(",")
                        .append(result.getForcedAlignment()).append(",");

                // Add QueryStats fields
                QueryStats stats = result.stats();
                if (stats != null) {
                    row.append(stats.relevantNodes()).append(",")
                            .append(stats.catRegions()).append(",")
                            .append(stats.bAdicCubes()).append(",")
                            .append(stats.numRegions()).append(",")
                            .append(stats.candidateRegions()).append(",")
                            .append(stats.queryRegions()).append(",");

                    // Add queries per level
                    int[] queriesPerLevel = stats.queriesPerLevel();
                    for (int i = 0; i < queriesPerLevel.length; i++) {
                        row.append(queriesPerLevel[i]);
                        if (i < queriesPerLevel.length - 1) {
                            row.append(",");
                        }
                    }
                } else {
                    // Fill with zeros if QueryStats is null
                    //TODO: Check if possible
                    row.append("0,0,0,0,0,0,0,0,0,0,0,0");
                }

                // Write the row to the file
                writer.write(row.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write results to file: " + resultFile, e);
        }
    }

    public static void evaluateQuerySet(OmniSketch omniSketch, String querySetFile, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromJSON(querySetFile);
        List<OmniQueryResult> results = new ArrayList<>(queries.size());
        int queryIndex = 0;
        for(List<Object> query : queries){
            try {
                long startTime = System.nanoTime();
                OmniQueryResult result = omniSketch.query(query);
                long queryTime = System.nanoTime() - startTime;
                result.setRuntime(queryTime / 1_000_000.0); // Convert to milliseconds
                results.add(result);
            } catch (Exception e) {
                String[] split = querySetFile.split("/");
                String querySetName = split[split.length - 1];
                throw new RuntimeException("Error processing query at index " + queryIndex + " from set "+querySetName+" : " + query, e);
            }
            queryIndex++;
        }

        writeOmniResultsToFile(results, resultFile);
    }

    private static void writeOmniResultsToFile(List<OmniQueryResult> results, String resultFile) throws IOException {
        String header = "estimates,runtime,case";
        createDirectories(Paths.get(resultFile).getParent());
        try (FileWriter writer = new FileWriter(resultFile)) {
            // Write the header
            writer.write(header + "\n");

            // Write each QueryResult as a row
            for (OmniQueryResult result : results) {
                StringBuilder row = new StringBuilder();

                // Add estimate
                row.append(result.getEstimate()).append(",")
                        .append(result.getRuntime()).append(",")
                        .append(result.getEstimateCase());
                // Write the row to the file
                writer.write(row.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write results to file: " + resultFile, e);
        }
    }
}
