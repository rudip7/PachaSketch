package pachasketch.experiments.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    public static int countRowsInCsv(String filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(Path.of(filePath))) {
            // Skip the header line and count the remaining lines
            return (int) reader.lines().skip(1).count();
        }
    }

    public static DatasetMetadata getDatasetMetadata(String pathToData){
        String datasetName;
        int nCat;
        int nNum;
        if (pathToData.contains("lineitem")) {
            datasetName = "tpch";
            nCat = 5;
            nNum = 5;
        } else if (pathToData.contains("acs")) {
            datasetName = "census";
            nCat = 7;
            nNum = 3;
        } else if (pathToData.contains("online_retail")) {
            datasetName = "retail";
            nCat = 3;
            nNum = 3;
        } else if (pathToData.contains("bank_marketing")) {
            datasetName = "bank";
            nCat = 6;
            nNum = 4;
        } else {
            throw new IllegalArgumentException("Unknown workload for dataset in path: " + pathToData);
        }
        return new DatasetMetadata(datasetName, nCat, nNum);
    }
}
