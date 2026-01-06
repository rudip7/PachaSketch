package pachasketch.experiments;

import pachasketch.experiments.runners.DifferentDatasets;
import pachasketch.experiments.runners.PachaParameters;
import pachasketch.experiments.runners.Scalability;
import pachasketch.experiments.utils.PreparePachaSketch;
import pachasketch.experiments.utils.Utils;
import pachasketch.omni.OmniSketch;
import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.OmniSketchFactory;
import pachasketch.utils.PachaSketchFactory;
import pachasketch.utils.QuerySetEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

public class Playground {
    public static void main(String[] args) throws IOException {
//        runTpchOmni("src/main/resources/results/omni");
//        runTpchPacha("src/main/resources/results/pacha");

//        String datasetPath = "../pacha_experiments/pacha_data/lineitem_0.1.csv";
//        PachaSketch pachaSketch = PreparePachaSketch.standardValues(datasetPath);
////        pachaSketch.limitToSingleCombination();
//        int nElements = Utils.countRowsInCsv(datasetPath);
//        DataLoaders.loadCSV(pachaSketch, datasetPath, nElements / 10);


//        long[][] logRanges = negativeGetLogRanges(-7, 4);
//        for (int i = 0; i < logRanges[0].length; i++) {
//            System.out.println("i: " + i + ", coeff: " + logRanges[0][i] + ", lower: " + logRanges[1][i] + ", upper: " + logRanges[2][i]);
//        }

//        ArrayList<long[]> logRangesArrList = getLogRangesArrListNegative(-1, -16, 4);
//        for (long[] range : logRangesArrList) {
//            System.out.println("Range: [" + range[0] + ", " + range[1] + "]");
//        }
//        String datasetPath = "../pacha_experiments/pacha_data/acs_folktables.csv";
        String datasetPath = "../pacha_experiments/pacha_data/lineitem_0.1.csv";
//        String datasetPath = "../pacha_experiments/pacha_data/bank_marketing.csv";
//        String datasetDir = "../pacha_experiments/pacha_data";
//
//        String resultsBaseDir = "../pacha_experiments/pacha_results";
//        PachaParameters.runDifferentLevels(7, datasetPath, resultsBaseDir);

//        DifferentDatasets.runPacha(datasetPath, resultsBaseDir+"/pacha");
//        double sizeInMB = PreparePachaSketch.standardValues(datasetPath).getSizeInMB();
//        DifferentDatasets.runOmni(sizeInMB, datasetPath, resultsBaseDir+"/omni");

//        Scalability.run(datasetDir, resultsBaseDir);


        System.out.println("PachaSketch varying error guarantees experiment...");
        double[] falsePositiveRates = new double[]{0.0025, 0.005, 0.01, 0.02, 0.04};
        double[] epsilons = new double[]{0.000025, 0.00005, 0.0001, 0.0002, 0.0004};
        double delta = 0.01;
        int levels = 5;

//        String datasetPath = dataDir+"/lineitem_0.1.csv";
        for(int i = 0; i < falsePositiveRates.length; i++) {
            PachaSketch pachaSketch = PreparePachaSketch.forTpch(falsePositiveRates[i], epsilons[i], delta, levels, 599_934);
            System.out.println("p: " + falsePositiveRates[i] + ", eps: " + epsilons[i] + ", size: " + pachaSketch.getSizeInMB() + " MB");
        }
    }

    public static ArrayList<long[]> getLogRangesArrListNegative(long startInclusive, long stopInclusive, int dyadicRangeBits) {
        ArrayList<long[]> logRangesArrList = getLogRangesArrList(-1*startInclusive, -1*stopInclusive, dyadicRangeBits);
        for (long[] range: logRangesArrList) {
            long temp = range[0];
            range[0] = -1*range[1];
            range[1] = -1*temp;
        }
        return logRangesArrList;
    }

    public static ArrayList<long[]> getLogRangesArrList(long startInclusive, long stopInclusive, int dyadicRangeBits) {
        startInclusive--;stopInclusive--;
        long initDiff=stopInclusive-startInclusive+1;
        ArrayList<long[]> result = new ArrayList<>();
        long totalSum = 0;
        long pow = 1;
        for (int j = 0; j <= dyadicRangeBits - 1; j++) {
            if (startInclusive+pow-1>stopInclusive)
                break;
            else if (startInclusive%(pow*2)==0 && startInclusive+pow-1<=stopInclusive) ;
                // do nothing, increase the power further
            else {
                result.add(new long[]{1+startInclusive, 1+startInclusive + pow - 1});
                totalSum+=pow;
                startInclusive+=pow;
            }
            pow*=2;
        }

        pow= (long) Math.pow(2,dyadicRangeBits);
        for (int j=dyadicRangeBits;j>=0;j--) {
            if (startInclusive%pow==0L && startInclusive+pow-1<=stopInclusive) {
                result.add(new long[]{1+startInclusive, 1+startInclusive + pow - 1});
                totalSum+=pow;
                startInclusive+=pow;
            }
            pow/=2;
        }

        if (totalSum != initDiff) {
            System.err.println("Error - no full coverage");
        }
        return result;
    }



    public static long[][] negativeGetLogRanges(long inputKey, int dyadicRangeBits) {
        long[][] logRanges = getLogRanges(-1 * inputKey, dyadicRangeBits);
        for (int i = 0; i < logRanges.length; i++) {
            for (int j = 0; j < logRanges[i].length; j++) {
                logRanges[i][j] = -1 * logRanges[i][j];
            }
        }
        for (int i = 0; i < logRanges[0].length; i++) {
            logRanges[0][i] = logRanges[0][i] - 1L ;//- (long) Math.pow(2, Main.dyadicRangeBits - 1);
        }
        long[] upper = Arrays.copyOf(logRanges[2], logRanges[2].length);
        logRanges[2] = logRanges[1];
        logRanges[1] = upper;
        return logRanges;
    }

    public static long[][] getLogRanges(long inputKey, int dyadicRangeBits) {
        long[] coeff      = new long[dyadicRangeBits];
        long[] lowerBound = new long[dyadicRangeBits]; // at pos i we store the x for ranges of size 2^(maxSize-i)
        long[] upperBound = new long[dyadicRangeBits]; // at pos i we store the x for ranges of size 2^(maxSize-i)

        long halfPoint = (long) Math.pow(2, dyadicRangeBits -1);
        if (inputKey < halfPoint) {
            coeff[0]=0;
            lowerBound[0] = 1; // inclusive, starting from 1
            upperBound[0] = halfPoint; // inclusive
        } else {
            coeff[0]=1;
            lowerBound[0] = halfPoint; //inclusive, starting from 1
            upperBound[0] = halfPoint*2; // inclusive
        }

        long pow = halfPoint;
        for (int i = 1; i< dyadicRangeBits -1; i++) {
            long prevCoeff = coeff[i-1];
            long newCoeffLower = prevCoeff*2;
            pow/=2;
            if ((newCoeffLower + 1) *pow < inputKey) {
                newCoeffLower++;
                coeff[i] = newCoeffLower;
            } else {
                coeff[i] = newCoeffLower;
            }
            lowerBound[i] = coeff[i]*pow+1;
            upperBound[i] = (coeff[i]+1)*pow;
        }
        lowerBound[dyadicRangeBits -1] = inputKey;
        upperBound[dyadicRangeBits -1] = inputKey;
        coeff[dyadicRangeBits -1] = inputKey;

        long[][] result = new long[3][];
        result[0] = coeff;
        result[1] = lowerBound;
        result[2] = upperBound;
        return result;
    }

    public static void runTpchOmni(String baseDir) throws IOException {
        // Using Lineitem table from TPC-H SF 0.1
        // Size of Lineitem 0.1
        int nElements = 599_934;

        double memBudget = 374.24; // In MB
        double eps = 0.1;
        double delta = 0.1;
        int dyadicRangeBits = 16;
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        OmniSketch omniSketch = OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);

        DataLoaders.loadCSV(omniSketch, "src/main/resources/data/lineitem_0.1.csv", nElements / 10);

        // Random queries
        System.out.println("Evaluating random queries...");
        QuerySetEvaluator.evaluateQuerySetOmniFromJson(omniSketch, "src/main/resources/queries/tpch/tpch_random.json", baseDir+"/tpch/tpch_random.csv");


        // Selectivity-based queries
        System.out.println("Evaluating selectivity-based queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/selectivities"));
        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
        for (double sel : selectivities) {
            String queryFile = String.format("src/main/resources/queries/tpch/selectivities/tpch_sel_%.2f.json", sel);
            String resultFile = String.format(baseDir+"/tpch/selectivities/tpch_sel_%.2f.csv", sel);
            QuerySetEvaluator.evaluateQuerySetOmniFromJson(omniSketch, queryFile, resultFile);
        }

        // Categorical queries
        System.out.println("Evaluating categorical queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/categorical"));
        int nCat = catColMap.length;
        int[] nCats = new int[nCat];
        for (int i = 1; i < nCat+1; i++) {
            nCats[i-1] = i;
        }
        for (int nC : nCats) {
            String queryFile = String.format("src/main/resources/queries/tpch/categorical/tpch_cat_%d.json", nC);
            String resultFile = String.format(baseDir+"/tpch/categorical/tpch_cat_%d.csv", nC);
            QuerySetEvaluator.evaluateQuerySetOmniFromJson(omniSketch, queryFile, resultFile);
        }

        // Numerical queries
        System.out.println("Evaluating numerical queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/numerical"));
        int nNum = numColMap.length;
        int[] nNums = new int[nNum];
        for (int i = 1; i < nNum+1; i++) {
            nNums[i-1] = i;
        }
        for (int nN : nNums) {
            String queryFile = String.format("src/main/resources/queries/tpch/numerical/tpch_num_%d.json", nN);
            String resultFile = String.format(baseDir+"/tpch/numerical/tpch_num_%d.csv", nN);
            QuerySetEvaluator.evaluateQuerySetOmniFromJson(omniSketch, queryFile, resultFile);
        }

        // Mixed queries
        System.out.println("Evaluating mixed queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/mixed"));
        int nDominant = Math.max(nCat, nNum);
        int[] nMixes = new int[nDominant];
        for (int i = 1; i < nDominant+1; i++) {
            nMixes[i-1] = i;
        }
        for (int nM : nMixes) {
            String queryFile = String.format("src/main/resources/queries/tpch/mixed/tpch_mix_%d.json", nM);
            String resultFile = String.format(baseDir+"/tpch/mixed/tpch_mix_%d.csv", nM);
            QuerySetEvaluator.evaluateQuerySetOmniFromJson(omniSketch, queryFile, resultFile);
        }

        System.out.println("All experiments completed.");
    }

    public static void runTpchPacha(String baseDir) throws IOException {
        // Using Lineitem table from TPC-H SF 0.1
        // Size of Lineitem 0.1
        int nElements = 599_934;

        ADTree adTree = ADTree.fromFile("src/main/resources/ad_trees/tpch_lineitem.json");
        MaterializedCombinations materialized = MaterializedCombinations.fromFile("src/main/resources/relevantCombinations/tpch_lineitem.json");

        // Error parameters
        double falsePositiveRate = 0.01;
        double eps = 0.0001;
        double delta = 0.01;

        // Other parameters
        int levels = 5;
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int[] bases = new int[]{5, 5, 5, 10, 2};

        PachaSketch pachaSketch = PachaSketchFactory.buildWithErrorParameters(
                catColMap, numColMap, levels, bases,
                adTree, materialized,
                falsePositiveRate, eps, delta,
                nElements
        );

        DataLoaders.loadCSV(pachaSketch, "src/main/resources/data/lineitem_0.1.csv", nElements / 10);

        // Random queries
        System.out.println("Evaluating random queries...");
        QuerySetEvaluator.evaluateQuerySetPachaFromJson(pachaSketch, "src/main/resources/queries/tpch/tpch_random.json", baseDir+"/tpch/tpch_random.csv");

        // Selectivity-based queries
        System.out.println("Evaluating selectivity-based queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/selectivities"));
        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
        for (double sel : selectivities) {
            String queryFile = String.format("src/main/resources/queries/tpch/selectivities/tpch_sel_%.2f.json", sel);
            String resultFile = String.format(baseDir+"/tpch/selectivities/tpch_sel_%.2f.csv", sel);
            QuerySetEvaluator.evaluateQuerySetPachaFromJson(pachaSketch, queryFile, resultFile);
        }

        // Categorical queries
        System.out.println("Evaluating categorical queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/categorical"));
        int nCat = catColMap.length;
        int[] nCats = new int[nCat];
        for (int i = 1; i < nCat+1; i++) {
            nCats[i-1] = i;
        }
        for (int nC : nCats) {
            String queryFile = String.format("src/main/resources/queries/tpch/categorical/tpch_cat_%d.json", nC);
            String resultFile = String.format(baseDir+"/tpch/categorical/tpch_cat_%d.csv", nC);
            QuerySetEvaluator.evaluateQuerySetPachaFromJson(pachaSketch, queryFile, resultFile);
        }

        // Numerical queries
        System.out.println("Evaluating numerical queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/numerical"));
        int nNum = numColMap.length;
        int[] nNums = new int[nNum];
        for (int i = 1; i < nNum+1; i++) {
            nNums[i-1] = i;
        }
        for (int nN : nNums) {
            String queryFile = String.format("src/main/resources/queries/tpch/numerical/tpch_num_%d.json", nN);
            String resultFile = String.format(baseDir+"/tpch/numerical/tpch_num_%d.csv", nN);
            QuerySetEvaluator.evaluateQuerySetPachaFromJson(pachaSketch, queryFile, resultFile);
        }

        // Mixed queries
        System.out.println("Evaluating mixed queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/mixed"));
        int nDominant = Math.max(nCat, nNum);
        int[] nMixes = new int[nDominant];
        for (int i = 1; i < nDominant+1; i++) {
            nMixes[i-1] = i;
        }
        for (int nM : nMixes) {
            String queryFile = String.format("src/main/resources/queries/tpch/mixed/tpch_mix_%d.json", nM);
            String resultFile = String.format(baseDir+"/tpch/mixed/tpch_mix_%d.csv", nM);
            QuerySetEvaluator.evaluateQuerySetPachaFromJson(pachaSketch, queryFile, resultFile);
        }

        System.out.println("All experiments completed.");
    }
}
