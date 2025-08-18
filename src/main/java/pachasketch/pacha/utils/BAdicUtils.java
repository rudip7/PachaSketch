package pachasketch.pacha.utils;

import java.util.ArrayList;
import java.util.List;

public class BAdicUtils {

    /**
     * Computes the minimal b-adic cover of the range [low, high].
     *
     * @param base The base for the b-adic range
     * @param low  The start of the range
     * @param high The end of the range (inclusive)
     * @return A 2D array where each row contains [level, index] pairs
     * @throws IllegalArgumentException if base is less than 1
     */
    public static List<int[]> minimalBAdicCover(int base, int low, int high) {
        if (base < 1) {
            throw new IllegalArgumentException("Base must be greater than or equal to 1");
        }

        if (base == 1) {
            List<int[]> result = new ArrayList<>();
            for (int i = low; i <= high; i++) {
                result.add(new int[]{0, i});
            }
            return result;
        }

        List<int[]> D = new ArrayList<>();
        int level = 0;

        while (low <= high) {
            int lowLevelLimit = (int) (Math.floor(low / Math.pow(base, level + 1)) * Math.pow(base, level + 1));
            if (lowLevelLimit != low) {
                lowLevelLimit += (int) Math.pow(base, level + 1);
                while (lowLevelLimit != low) {
                    if (low > high) break;
                    int index = (int) Math.floor(low / Math.pow(base, level));
                    D.add(new int[]{level, index});
                    low = low + (int) Math.pow(base, level);
                }
            }

            int highLevelLimit = (int) ((Math.floor(high / Math.pow(base, level + 1)) + 1) * Math.pow(base, level + 1) - 1);
            if (highLevelLimit != high) {
                highLevelLimit -= (int) Math.pow(base, level + 1);
                while (highLevelLimit != high) {
                    if (low > high) break;
                    int index = (int) Math.floor(high / Math.pow(base, level));
                    D.add(new int[]{level, index});
                    high = high - (int) Math.pow(base, level);
                }
            }

            level++;
        }

//        // Sort by index first, then by level
//        D.sort((a, b) -> {
//            if (a[1] != b[1]) return Integer.compare(a[1], b[1]);
//            return Integer.compare(a[0], b[0]);
//        });

        return D;
    }

    /**
     * Downgrades a b-adic range index to a lower level.
     *
     * @param base      The base for the b-adic range
     * @param level     The current level of the index
     * @param idx       The index to downgrade
     * @param newLevel  The target level to downgrade to
     * @return An array of indices at the new level
     * @throws IllegalArgumentException if attempting to downgrade to a higher level
     */
    public static int[] downgradeBAdicRangeIndices(int base, int level, int idx, int newLevel) {
        if (newLevel == level) {
            return new int[]{idx};
        }
        if (newLevel > level) {
            throw new IllegalArgumentException("Cannot downgrade to a higher level.");
        }

        int levelDiff = level - newLevel;
        int scale = (int) Math.pow(base, levelDiff);
        int tempIndex = idx * scale;

        int[] result = new int[scale];
        for (int i = 0; i < scale; i++) {
            result[i] = tempIndex + i;
        }
        return result;
    }

}