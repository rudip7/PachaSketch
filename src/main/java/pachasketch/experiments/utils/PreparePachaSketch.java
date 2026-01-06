package pachasketch.experiments.utils;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.CountMinSketch;
import pachasketch.pacha.baseSketches.Filter;
import pachasketch.pacha.baseSketches.ScalableBloomFilter;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.PachaSketchFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PreparePachaSketch {

    public static PachaSketch standardValues(String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        return standardValues(filePath, nElements);
    }

    public static PachaSketch standardValues(String filePath, int nElements) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        if (filePath.contains("lineitem")) {
            return forTpch(falsePositiveRate, eps, delta, levels, nElements);
        } else if (filePath.contains("acs")) {
            return forUSCensus(falsePositiveRate, eps, delta, levels, nElements);
        } else if (filePath.contains("online_retail")) {
            return forOnlineRetail(falsePositiveRate, eps, delta, levels, nElements);
        } else if (filePath.contains("bank_marketing")) {
            return forBankMarketing(falsePositiveRate, eps, delta, levels, nElements);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static PachaSketch withGuarantees(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        if (filePath.contains("lineitem")) {
            return forTpch(falsePositiveRate, eps, delta, levels, nElements);
        } else if (filePath.contains("acs")) {
            return forUSCensus(falsePositiveRate, eps, delta, levels, nElements);
        } else if (filePath.contains("online_retail")) {
            return forOnlineRetail(falsePositiveRate, eps, delta, levels, nElements);
        } else if (filePath.contains("bank_marketing")) {
            return forBankMarketing(falsePositiveRate, eps, delta, levels, nElements);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static PachaSketch loadWithStandardValues(String filePath) throws IOException {
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;
        int levels = 5;

        return loadWithGuarantees(falsePositiveRate, eps, delta, levels, filePath);
    }

    public static PachaSketch loadWithGuarantees(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        if (filePath.contains("lineitem")) {
            return loadTpch(falsePositiveRate, eps, delta, levels, filePath);
        } else if (filePath.contains("acs")) {
            return loadUSCensus(falsePositiveRate, eps, delta, levels, filePath);
        } else if (filePath.contains("online_retail")) {
            return loadOnlineRetail(falsePositiveRate, eps, delta, levels, filePath);
        } else if (filePath.contains("bank_marketing")) {
            return loadBankMarketing(falsePositiveRate, eps, delta, levels, filePath);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static PachaSketch forTpchWithMemoryBudget(int memBudget, int maxNCubes, int nHashIndex) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/tpch_lineitem.json");

        List<String> numCols = Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity");
        List<List<String>> combinations = Arrays.asList(
                Arrays.asList("n_shipdate"),
                Arrays.asList("n_commitdate"),
                Arrays.asList("n_receiptdate"),
                Arrays.asList("n_extendedprice"),
                Arrays.asList("n_quantity"),

                Arrays.asList("n_shipdate", "n_commitdate"),
                Arrays.asList("n_shipdate", "n_receiptdate"),
                Arrays.asList("n_shipdate", "n_quantity"),
                Arrays.asList("n_commitdate", "n_receiptdate"),
                Arrays.asList("n_commitdate", "n_extendedprice"),
                Arrays.asList("n_extendedprice", "n_quantity"),

                Arrays.asList("n_commitdate", "n_receiptdate", "n_extendedprice"),

                Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")
        );
        MaterializedCombinations materialized = new MaterializedCombinations(numCols, combinations);

        int levels = 5;
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        int adTreeLevels = adTree.getNumberUpdates();
        int numCombinations = combinations.size();

        int catUpdates = adTreeLevels;
        int numUpdates = numCombinations * levels + 1;
        int regionUpdates = catUpdates * numUpdates;

        double memCMs = memBudget * 0.25 / levels;
        int nCounters = (int) (memCMs * 1024 * 1024 / 4);
        int depth = 3;
        int width = nCounters / depth;

        double referenceEps = 0.0001 / (catUpdates * materialized.getNumCombinations());
        double errorEps = Math.E / width;
        while (errorEps < referenceEps && depth < 5) {
            depth++;
            width = nCounters / depth;
            errorEps = Math.E / width;
        }

        CountMinSketch[] baseSketches = new CountMinSketch[levels];
        for (int i = 0; i < levels; i++) {
            baseSketches[i] = new CountMinSketch(width, depth);
        }

        memCMs = baseSketches[0].getSizeInMB() * levels;
        System.out.println("Memory for Count-Min Sketches: " + memCMs + " MB");
        double memIndex = memBudget - memCMs;
        double pNumIndex = numUpdates / (double) (numUpdates + regionUpdates);
        double pRegionIndex = 1.0 - pNumIndex;
        double memNumIndex = memIndex * pNumIndex;
        System.out.println("Memory for numerical index: " + memNumIndex + " MB");
        double memRegionIndex = memIndex * pRegionIndex;
        System.out.println("Memory for region index: " + memRegionIndex + " MB");
        long nBitsNumIndex = (long) Math.ceil(memNumIndex * 1024 * 1024 * 8);
        long nBitsRegionIndex = (long) Math.ceil(memRegionIndex * 1024 * 1024 * 8);

        Filter numIndex;
        if (nBitsNumIndex > Integer.MAX_VALUE - 8){
            numIndex = new ScalableBloomFilter(nBitsNumIndex, nHashIndex);
        } else {
            numIndex = new BloomFilter(nHashIndex, (int) nBitsNumIndex);
        }

        Filter regionIndex;
        if (nBitsRegionIndex > Integer.MAX_VALUE - 8){
            regionIndex = new ScalableBloomFilter(nBitsRegionIndex, nHashIndex);
        } else {
            regionIndex = new BloomFilter(nHashIndex, (int) nBitsRegionIndex);
        }

        PachaSketch pachaSketch = new PachaSketch(
                levels,
                catColMap,
                numColMap,
                bases,
                adTree,
                materialized,
                null,
                numIndex,
                regionIndex,
                baseSketches
        );

        pachaSketch.setMaxNCubes(maxNCubes);

        System.out.println("PachaSketch with memory budget " + memBudget + " MB created. Real memory usage: " + pachaSketch.getSizeInMB() + " MB");

        return pachaSketch;
    }

    public static PachaSketch forTpch(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/tpch_lineitem.json");

        List<String> numCols = Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity");
        List<List<String>> combinations = Arrays.asList(
                Arrays.asList("n_shipdate"),
                Arrays.asList("n_commitdate"),
                Arrays.asList("n_receiptdate"),
                Arrays.asList("n_extendedprice"),
                Arrays.asList("n_quantity"),

                Arrays.asList("n_shipdate", "n_commitdate"),
                Arrays.asList("n_shipdate", "n_receiptdate"),
                Arrays.asList("n_shipdate", "n_quantity"),
                Arrays.asList("n_commitdate", "n_receiptdate"),
                Arrays.asList("n_commitdate", "n_extendedprice"),
                Arrays.asList("n_extendedprice", "n_quantity"),

                Arrays.asList("n_commitdate", "n_receiptdate", "n_extendedprice"),

                Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")
        );

        MaterializedCombinations materialized = new MaterializedCombinations(numCols, combinations);

        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }


    public static PachaSketch loadTpch(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketch pachaSketch = forTpch(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }

    public static PachaSketch forUSCensusSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/acs_folktables.json");
        List<String> numCols = Arrays.asList("AGEP", "PINCP", "PWGTP");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5, 6};
        int[] numColMap = new int[]{7, 8, 9};
        int[] bases = new int[]{2, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forUSCensus(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/acs_folktables.json");
        List<String> numCols = Arrays.asList("AGEP", "PINCP", "PWGTP");
        MaterializedCombinations materialized = MaterializedCombinations.createAllCombinations(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5, 6};
        int[] numColMap = new int[]{7, 8, 9};
        int[] bases = new int[]{2, 10, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch loadUSCensus(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketch pachaSketch = forUSCensus(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }

    public static PachaSketch forOnlineRetailSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/online_retail.json");
        List<String> numCols = Arrays.asList("date", "age", "total");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2};
        int[] numColMap = new int[]{3, 4, 5};
        int[] bases = new int[]{4, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forOnlineRetail(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/online_retail.json");
        List<String> numCols = Arrays.asList("date", "age", "total");
        MaterializedCombinations materialized = MaterializedCombinations.createAllCombinations(numCols);

        int[] catColMap = new int[]{0, 1, 2};
        int[] numColMap = new int[]{3, 4, 5};
        int[] bases = new int[]{4, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch loadOnlineRetail(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketch pachaSketch = forOnlineRetail(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }

    public static PachaSketch forBankMarketingSingle(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/bank_marketing.json");
        List<String> numCols = Arrays.asList("duration", "balance", "age", "date");
        MaterializedCombinations materialized = MaterializedCombinations.createSingleCombination(numCols);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5};
        int[] numColMap = new int[]{6, 7, 8, 9};
        int[] bases = new int[]{4, 5, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch forBankMarketing(double falsePositiveRate, double eps, double delta, int levels, int nElements) throws IOException {
        ADTree adTree = ADTree.fromResource("ad_trees/bank_marketing.json");
        List<String> numCols = Arrays.asList("duration", "balance", "age", "date");
        List<List<String>> combinations = Arrays.asList(
                Arrays.asList("duration"),
                Arrays.asList("balance"),
                Arrays.asList("age"),
                Arrays.asList("date"),

                Arrays.asList("duration", "age"),
                Arrays.asList("duration", "date"),
                Arrays.asList("duration", "balance"),
                Arrays.asList("balance", "age"),
                Arrays.asList("balance", "date"),

                Arrays.asList("duration", "balance", "age"),

                Arrays.asList("duration", "balance", "age", "date")
        );
        MaterializedCombinations materialized = new MaterializedCombinations(numCols, combinations);

        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5};
        int[] numColMap = new int[]{6, 7, 8, 9};
        int[] bases = new int[]{4, 5, 2, 2};

        return PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );
    }

    public static PachaSketch loadBankMarketing(double falsePositiveRate, double eps, double delta, int levels, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);
        PachaSketch pachaSketch = forBankMarketing(falsePositiveRate, eps, delta, levels, nElements);
        DataLoaders.loadCSV(pachaSketch, filePath, nElements / 10);

        return pachaSketch;
    }


}
