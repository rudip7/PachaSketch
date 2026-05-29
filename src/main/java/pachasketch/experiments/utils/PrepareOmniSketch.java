package pachasketch.experiments.utils;

import pachasketch.omni.OmniSketch;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.OmniSketchFactory;

import java.io.IOException;

public class PrepareOmniSketch {

    public static OmniSketch standardValues(double memBudget, String filePath) throws IOException {
        double eps = 0.1;
        double delta = 0.1;

        if (filePath.contains("lineitem")) {
            return forTpch(eps, delta, memBudget);
        } else if (filePath.contains("acs")) {
            return forUSCensus(eps, delta, memBudget);
        } else if (filePath.contains("online_retail")) {
            return forOnlineRetail(eps, delta, memBudget);
        } else if (filePath.contains("bank_marketing")) {
            return forBankMarketing(eps, delta, memBudget);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static OmniSketch loadWithStandardValues(double memBudget, String filePath) throws IOException {
        //TODO: change eps to default 0.1
//        double eps = 0.05;
//        double delta = 0.01;

        double eps = 0.1;
        double delta = 0.1;

        if (filePath.contains("lineitem")) {
            return loadTpch(eps, delta, memBudget, filePath);
        } else if (filePath.contains("acs")) {
            return loadUSCensus(eps, delta, memBudget, filePath);
        } else if (filePath.contains("online_retail")) {
            return loadOnlineRetail(eps, delta, memBudget, filePath);
        } else if (filePath.contains("bank_marketing")) {
            return loadBankMarketing(eps, delta, memBudget, filePath);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static OmniSketch forTpch(double eps, double delta, double memBudget) throws IOException {
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};
        int dyadicRangeBits = 17;
//        int dyadicRangeBits = 16;

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }

    public static OmniSketch loadTpch(double eps, double delta, double memBudget, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forTpch(eps, delta, memBudget);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }

    public static OmniSketch forUSCensus(double eps, double delta, double memBudget) throws IOException {
        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5, 6};
        int[] numColMap = new int[]{7, 8, 9};
        int dyadicRangeBits = 21;

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }

    public static OmniSketch loadUSCensus(double eps, double delta, double memBudget, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forUSCensus(eps, delta, memBudget);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }

    public static OmniSketch forOnlineRetail(double eps, double delta, double memBudget) throws IOException {
        int[] catColMap = new int[]{0, 1, 2};
        int[] numColMap = new int[]{3, 4, 5};
        int dyadicRangeBits = 18;

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }

    public static OmniSketch loadOnlineRetail(double eps, double delta, double memBudget, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forOnlineRetail(eps, delta, memBudget);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }

    public static OmniSketch forBankMarketing(double eps, double delta, double memBudget) throws IOException {
        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5};
        int[] numColMap = new int[]{6, 7, 8, 9};
        int dyadicRangeBits = 17;

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }
    
    public static OmniSketch loadBankMarketing(double eps, double delta, double memBudget, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forBankMarketing(eps, delta, memBudget);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }
}
