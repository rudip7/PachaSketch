package pachasketch.experiments;

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

public class Playground {
    public static void main(String[] args) throws IOException {
        runTpchOmni("src/main/resources/results/omni");
//        runTpchPacha("src/main/resources/results/pacha");
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
        QuerySetEvaluator.evaluateQuerySet(omniSketch, "src/main/resources/queries/tpch/tpch_random.json", baseDir+"/tpch/tpch_random.csv");


        // Selectivity-based queries
        System.out.println("Evaluating selectivity-based queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/selectivities"));
        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
        for (double sel : selectivities) {
            String queryFile = String.format("src/main/resources/queries/tpch/selectivities/tpch_sel_%.2f.json", sel);
            String resultFile = String.format(baseDir+"/tpch/selectivities/tpch_sel_%.2f.csv", sel);
            QuerySetEvaluator.evaluateQuerySet(omniSketch, queryFile, resultFile);
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
            QuerySetEvaluator.evaluateQuerySet(omniSketch, queryFile, resultFile);
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
            QuerySetEvaluator.evaluateQuerySet(omniSketch, queryFile, resultFile);
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
            QuerySetEvaluator.evaluateQuerySet(omniSketch, queryFile, resultFile);
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
        QuerySetEvaluator.evaluateQuerySet(pachaSketch, "src/main/resources/queries/tpch/tpch_random.json", baseDir+"/tpch/tpch_random.csv");

        // Selectivity-based queries
        System.out.println("Evaluating selectivity-based queries...");
        Files.createDirectories(Path.of(baseDir+"/tpch/selectivities"));
        double[] selectivities = new double[]{0.01, 0.02, 0.04, 0.08, 0.16, 0.32, 0.64};
        for (double sel : selectivities) {
            String queryFile = String.format("src/main/resources/queries/tpch/selectivities/tpch_sel_%.2f.json", sel);
            String resultFile = String.format(baseDir+"/tpch/selectivities/tpch_sel_%.2f.csv", sel);
            QuerySetEvaluator.evaluateQuerySet(pachaSketch, queryFile, resultFile);
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
            QuerySetEvaluator.evaluateQuerySet(pachaSketch, queryFile, resultFile);
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
            QuerySetEvaluator.evaluateQuerySet(pachaSketch, queryFile, resultFile);
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
            QuerySetEvaluator.evaluateQuerySet(pachaSketch, queryFile, resultFile);
        }

        System.out.println("All experiments completed.");
    }
}
