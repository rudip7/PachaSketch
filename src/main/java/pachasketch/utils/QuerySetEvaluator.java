package pachasketch.utils;

import pachasketch.Sketch;
import pachasketch.SketchWithAggregateColumn;
import pachasketch.omni.OmniSketch;
import pachasketch.omni.utils.OmniQueryResult;
import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.PachaSketchAvg;
import pachasketch.pacha.PachaSketchSum;
import pachasketch.pacha.utils.PachaQueryResult;
import pachasketch.pacha.utils.QueryStats;
import pachasketch.sampling.PrioritySampler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.Files.createDirectories;

public class QuerySetEvaluator {


    public static void evaluateQuerySetFromResource(Sketch pachaSketch, String resourcePath, String resultFile) throws IOException {
        if (pachaSketch instanceof PachaSketch || pachaSketch instanceof SketchWithAggregateColumn) {
            evaluateQuerySetPachaFromResource(pachaSketch, resourcePath, resultFile);
        } else if (pachaSketch instanceof OmniSketch) {
            evaluateQuerySetOmniFromResource((OmniSketch) pachaSketch, resourcePath, resultFile);
        } else {
            throw new IllegalArgumentException("Unsupported sketch type: " + pachaSketch.getClass().getName());
        }
    }

    public static void evaluateQuerySetPachaFromResource(Sketch pachaSketch, String resourcePath, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromResource(resourcePath);
        String[] split = resourcePath.split("/");
        String querySetName = split[split.length - 1];

        evaluateQuerySetPacha(pachaSketch, querySetName, queries, resultFile);
    }

    public static void evaluateQuerySetPachaFromJson(Sketch pachaSketch, String querySetFile, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromJson(querySetFile);
        String[] split = querySetFile.split("/");
        String querySetName = split[split.length - 1];

        evaluateQuerySetPacha(pachaSketch, querySetName, queries, resultFile);
    }

    public static void evaluateQuerySetPacha(Sketch pachaSketch, String querySetName, List<List<Object>> queries, String resultFile) throws IOException {
        List<PachaQueryResult> results = new ArrayList<>(queries.size());
        int queryIndex = 0;
        for(List<Object> query : queries){
            try {
                long startTime = System.nanoTime();
                PachaQueryResult result;
                if(pachaSketch instanceof PachaSketch) {
                    result = ((PachaSketch) pachaSketch).query(query, true, false);
                } else if (pachaSketch instanceof SketchWithAggregateColumn) {
                    result = ((SketchWithAggregateColumn) pachaSketch).query(query, true, false);
                } else if (pachaSketch instanceof PachaSketchAvg) {
                    result = ((PachaSketchAvg) pachaSketch).query(query, true, false);
                } else {
                    throw new IllegalArgumentException("Unsupported Pacha sketch type: " + pachaSketch.getClass().getName());
                }

                long queryTime = System.nanoTime() - startTime;
                result.setRuntime(queryTime / 1_000_000.0); // Convert to milliseconds
                results.add(result);
            } catch (Exception e) {
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
                    row.append("0,0,0,0,0,0,0,0,0,0,0");
                }

                // Write the row to the file
                writer.write(row.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write results to file: " + resultFile, e);
        }
    }

    public static void evaluateQuerySetOmniFromResource(OmniSketch omniSketch, String resourcePath, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromResource(resourcePath);
        String[] split = resourcePath.split("/");
        String querySetName = split[split.length - 1];

        evaluateQuerySetOmni(omniSketch, queries, querySetName, resultFile);
    }

    public static void evaluateQuerySetOmniFromJson(OmniSketch omniSketch, String querySetFile, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromJson(querySetFile);
        String[] split = querySetFile.split("/");
        String querySetName = split[split.length - 1];

        evaluateQuerySetOmni(omniSketch, queries, querySetName, resultFile);
    }

    public static void evaluateQuerySetOmni(OmniSketch omniSketch, List<List<Object>> queries, String querySetName, String resultFile) throws IOException {
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


    public static void evaluateQuerySetSamplerFromResource(PrioritySampler sampler, String resourcePath, String resultFile) throws IOException {
        List<List<Object>> queries = QueryFactory.fromResource(resourcePath);
        String[] split = resourcePath.split("/");
        String querySetName = split[split.length - 1];

        evaluateQuerySetSampler(sampler, querySetName, queries, resultFile);
    }

    public static void evaluateQuerySetSampler(PrioritySampler sampler, String querySetName, List<List<Object>> queries, String resultFile) throws IOException {
        List<SimpleQueryResult> results = new ArrayList<>(queries.size());
        int queryIndex = 0;
        for(List<Object> query : queries){
            try {
                long startTime = System.nanoTime();
                double estimate = sampler.query(query);
                long queryTime = System.nanoTime() - startTime;
                SimpleQueryResult result = new SimpleQueryResult(estimate);
                result.setRuntime(queryTime / 1_000_000.0); // Convert to milliseconds
                results.add(result);
            } catch (Exception e) {
                throw new RuntimeException("Error processing query at index " + queryIndex + " from set "+querySetName+" : " + query, e);
            }
            queryIndex++;
        }

        writeResultsToFile(results, resultFile);
    }

    private static void writeResultsToFile(List<SimpleQueryResult> results, String resultFile) throws IOException {
        String header = "estimates,runtime";
        createDirectories(Paths.get(resultFile).getParent());
        try (FileWriter writer = new FileWriter(resultFile)) {
            // Write the header
            writer.write(header + "\n");

            // Write each QueryResult as a row
            for (SimpleQueryResult result : results) {
                StringBuilder row = new StringBuilder();

                // Add estimate
                row.append(result.getEstimate()).append(",")
                        .append(result.getRuntime());
                // Write the row to the file
                writer.write(row.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to write results to file: " + resultFile, e);
        }
    }
}
