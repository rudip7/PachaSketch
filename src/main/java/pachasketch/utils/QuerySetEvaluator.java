package pachasketch.utils;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.utils.QueryResult;
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
        List<QueryResult> results = new ArrayList<>(queries.size());
        int queryIndex = 0;
        for(List<Object> query : queries){
            try {
                QueryResult result = pachaSketch.query(query, true, false);
                results.add(result);
            } catch (Exception e) {
                throw new RuntimeException("Error processing query at index " + queryIndex + ": " + query, e);
            }
            queryIndex++;
        }

        writePachaResultsToFile(results, resultFile);
    }

    private static void writePachaResultsToFile(List<QueryResult> results, String resultFile) throws IOException {
        String header = "estimates,relevant_nodes,cat_regions,b_adic_cubes,num_regions,candidate_regions,query_regions";
        int nLevels = results.get(0).stats().queriesPerLevel().length;
        for (int i = 0; i < nLevels; i++) {
            header += ",level_" + i + "_queries";
        }

        createDirectories(Paths.get(resultFile).getParent());
        try (FileWriter writer = new FileWriter(resultFile)) {
            // Write the header
            writer.write(header + "\n");

            // Write each QueryResult as a row
            for (QueryResult result : results) {
                StringBuilder row = new StringBuilder();

                // Add estimate
                row.append(result.estimate()).append(",");

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
}
