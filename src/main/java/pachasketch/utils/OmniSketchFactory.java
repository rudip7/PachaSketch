package pachasketch.utils;

import pachasketch.omni.DetermineB;
import pachasketch.omni.OmniSketch;

import java.util.List;

public class OmniSketchFactory {
    public static int computeMaxB(long memBudget, int w, int d, int nCat, int nNum, int dyadicLevels, double delta) {
        long factor = (long) w * d * (nCat + nNum * dyadicLevels);
        int left = 1, right = (int) 1e8;
        int maxB = 0;

        while (left <= right) {
            int B = (left + right) / 2;
            int ceilLog = (int) Math.ceil(Math.log(4 * Math.pow(B, 2.5) / delta));
            long rhs = factor * (32L + B * ceilLog + 3 * 32 + 1);

            if (rhs <= memBudget) {
                maxB = B;
                left = B + 1;
            } else {
                right = B - 1;
            }
        }
        return maxB;
    }

    public static OmniSketch buildWithMemoryBudget(double memBudget, int[] catColMap, int[] numColMap,
                                                   double delta, double eps, int dyadicRangeBits) {
        // Convert memory budget from MB to bits
        long memBudgetBits = (long) (memBudget * 1024 * 1024 * 8);

        // Compute d and w
        int d = (int) Math.ceil(Math.log(2 / delta) / Math.log(2));
        int w = 1 + (int) Math.ceil(Math.exp(1) * Math.pow((eps + 1) / eps, 1.0 / d));

        // Compute the number of categorical and numerical columns
        int nCat = catColMap.length;
        int nNum = numColMap.length;

        // Compute the maximum sample size
//        int maxSampleSize = computeMaxB(memBudgetBits, w, d, nCat, nNum, dyadicRangeBits, delta);
        DetermineB sampleSizeCalculator = new DetermineB(memBudgetBits);
        int maxSampleSize = sampleSizeCalculator.determineB(d, w, delta, nCat, nNum, dyadicRangeBits);

        // Return a new OmniSketch object
        return new OmniSketch(catColMap, numColMap, delta, eps, maxSampleSize, dyadicRangeBits);
    }

}
