package pachasketch.experiments;

import pachasketch.experiments.runners.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Usage: java -jar <jarfile> <dataDir> <resultsBaseDir> [-a | -d | -e | -m | -p | -s | -u | -g]");
            return;
        }

        String dataDir = args[0];
        String resultsBaseDir = args[1];
        List<String> flags = Arrays.asList(args).subList(2, args.length);

        if (flags.contains("-a") || flags.isEmpty()) {
            System.out.println("Running all experiments...");
            UpdateEfficiency.run(dataDir, resultsBaseDir);
            DifferentDatasets.run(dataDir, resultsBaseDir);
            ErrorGuarantees.run(dataDir, resultsBaseDir);
            PachaParameters.run(dataDir, resultsBaseDir);
            Scalability.run(dataDir, resultsBaseDir);
            MemoryBudget.run(dataDir, resultsBaseDir);
            DifferentAggregates.run(dataDir, resultsBaseDir);
        } else {
            if (flags.contains("-u")) {
                UpdateEfficiency.run(dataDir, resultsBaseDir);
            }
            if (flags.contains("-d")) {
                DifferentDatasets.run(dataDir, resultsBaseDir);
            }
            if (flags.contains("-e")) {
                ErrorGuarantees.run(dataDir, resultsBaseDir);
            }
            if (flags.contains("-p")) {
                PachaParameters.run(dataDir, resultsBaseDir);
            }
            if (flags.contains("-s")) {
                Scalability.run(dataDir, resultsBaseDir);
            }
            if (flags.contains("-m")) {
                MemoryBudget.run(dataDir, resultsBaseDir);
            }
            if (flags.contains("-g")) {
                DifferentAggregates.run(dataDir, resultsBaseDir);
            }
        }
    }
}