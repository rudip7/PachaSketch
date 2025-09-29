package pachasketch.experiments.utils;

import pachasketch.omni.OmniSketch;
import pachasketch.utils.DataLoaders;
import pachasketch.utils.OmniSketchFactory;

import java.io.IOException;

public class PrepareOmniSketch {

    public static OmniSketch standardValues(double memBudget, String filePath) throws IOException {
        double eps = 0.1;
        double delta = 0.1;
        int dyadicRangeBits = 16;

        if (filePath.contains("lineitem")) {
            return forTpch(eps, delta, memBudget, dyadicRangeBits);
        } else if (filePath.contains("acs")) {
            return forUSCensus(eps, delta, memBudget, dyadicRangeBits);
        } else if (filePath.contains("online_retail")) {
            return forOnlineRetail(eps, delta, memBudget, dyadicRangeBits);
        } else if (filePath.contains("bank_marketing")) {
            return forBankMarketing(eps, delta, memBudget, dyadicRangeBits);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static OmniSketch loadWithStandardValues(double memBudget, String filePath) throws IOException {
        double eps = 0.1;
        double delta = 0.1;
        int dyadicRangeBits = 16;

        if (filePath.contains("lineitem")) {
            return loadTpch(eps, delta, memBudget, dyadicRangeBits, filePath);
        } else if (filePath.contains("acs")) {
            return loadUSCensus(eps, delta, memBudget, dyadicRangeBits, filePath);
        } else if (filePath.contains("online_retail")) {
            return loadOnlineRetail(eps, delta, memBudget, dyadicRangeBits, filePath);
        } else if (filePath.contains("bank_marketing")) {
            return loadBankMarketing(eps, delta, memBudget, dyadicRangeBits, filePath);
        } else {
            throw new IllegalArgumentException("Unknown dataset in file path: " + filePath);
        }
    }

    public static OmniSketch forTpch(double eps, double delta, double memBudget, int dyadicRangeBits) throws IOException {
        int[] catColMap = new int[]{0, 1, 2, 3, 4};
        int[] numColMap = new int[]{5, 6, 7, 8, 9};

        OmniSketch omniSketch = OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);

        return omniSketch;
    }

    public static OmniSketch loadTpch(double eps, double delta, double memBudget, int dyadicRangeBits, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forTpch(eps, delta, memBudget, dyadicRangeBits);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }

    public static OmniSketch forUSCensus(double eps, double delta, double memBudget, int dyadicRangeBits) throws IOException {
        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5, 6};
        int[] numColMap = new int[]{7, 8, 9};

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }

    public static OmniSketch loadUSCensus(double eps, double delta, double memBudget, int dyadicRangeBits, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forUSCensus(eps, delta, memBudget, dyadicRangeBits);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }

    public static OmniSketch forOnlineRetail(double eps, double delta, double memBudget, int dyadicRangeBits) throws IOException {
        int[] catColMap = new int[]{0, 1, 2};
        int[] numColMap = new int[]{3, 4, 5};

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }

    public static OmniSketch loadOnlineRetail(double eps, double delta, double memBudget, int dyadicRangeBits, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forOnlineRetail(eps, delta, memBudget, dyadicRangeBits);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }

    public static OmniSketch forBankMarketing(double eps, double delta, double memBudget, int dyadicRangeBits) throws IOException {
        int[] catColMap = new int[]{0, 1, 2, 3, 4, 5};
        int[] numColMap = new int[]{6, 7, 8, 9};

        return OmniSketchFactory.buildWithMemoryBudget(memBudget, catColMap, numColMap, delta, eps, dyadicRangeBits);
    }
    
    public static OmniSketch loadBankMarketing(double eps, double delta, double memBudget, int dyadicRangeBits, String filePath) throws IOException {
        int nElements = Utils.countRowsInCsv(filePath);

        OmniSketch omniSketch = forBankMarketing(eps, delta, memBudget, dyadicRangeBits);
        DataLoaders.loadCSV(omniSketch, filePath, nElements / 10);

        return omniSketch;
    }
}
