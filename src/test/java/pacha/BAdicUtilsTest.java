package pacha;

import org.junit.jupiter.api.Test;
import pachasketch.pacha.utils.BAdicUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BAdicUtilsTest {

    private int[][] sortByIndexAndLevel(List<int[]> input) {
        // Sort the input array by index first, then by level
        input.sort((a, b) -> {
            if (a[1] != b[1]) return Integer.compare(a[1], b[1]);
            return Integer.compare(a[0], b[0]);
        });

        return input.toArray(new int[0][]);
    }

    @Test
    void baseOneCreatesUnitIntervals() {
        int[][] expected = {{0,0}, {0,1}, {0,2}};
        assertArrayEquals(expected, sortByIndexAndLevel(BAdicUtils.minimalBAdicCover(1, 0, 2)));
    }

    @Test
    void minimalCoverForBaseTwoWithSingleInterval() {
        int[][] expected = {{1,0}};
        assertArrayEquals(expected, sortByIndexAndLevel(BAdicUtils.minimalBAdicCover(2, 0, 1)));
    }

    @Test
    void minimalCoverWithMultipleLevels() {
        int[][] expected = {{2,2}, {1,3}, {0,5}};
        assertArrayEquals(expected, sortByIndexAndLevel(BAdicUtils.minimalBAdicCover(2, 5, 11)));
    }

    @Test
    void minimalCoverWithNegativeRange() {
        int[][] expected = {{0,-2}, {1,-2}};
        assertArrayEquals(expected, sortByIndexAndLevel(BAdicUtils.minimalBAdicCover(2, -4, -2)));
    }


    @Test
    void minimalCoverWithLargeBase() {
        int[][] expected = {{1,0}, {0,10}, {0,11}, {0,12}};
        assertArrayEquals(expected, sortByIndexAndLevel(BAdicUtils.minimalBAdicCover(10, 0, 12)));
    }

    @Test
    void downgradeToSameLevelReturnsSingleIndex() {
        assertArrayEquals(new int[]{5}, BAdicUtils.downgradeBAdicRangeIndices(2, 3, 5, 3));
    }

    @Test
    void downgradeThrowsExceptionForHigherLevel() {
        assertThrows(IllegalArgumentException.class, () ->
            BAdicUtils.downgradeBAdicRangeIndices(2, 1, 5, 2));
    }

    @Test
    void downgradeBaseTwoByOneLevel() {
        assertArrayEquals(new int[]{10, 11}, BAdicUtils.downgradeBAdicRangeIndices(2, 1, 5, 0));
    }

    @Test
    void downgradeBaseTwoByTwoLevels() {
        assertArrayEquals(new int[]{8, 9, 10, 11}, BAdicUtils.downgradeBAdicRangeIndices(2, 2, 2, 0));
    }

    @Test
    void downgradeBaseThreeByOneLevel() {
        assertArrayEquals(new int[]{9, 10, 11}, BAdicUtils.downgradeBAdicRangeIndices(3, 1, 3, 0));
    }

    @Test
    void downgradeNegativeIndex() {
        assertArrayEquals(new int[]{-8, -7}, BAdicUtils.downgradeBAdicRangeIndices(2, 1, -4, 0));
    }

    @Test
    void downgradeZeroIndex() {
        assertArrayEquals(new int[]{0, 1, 2, 3}, BAdicUtils.downgradeBAdicRangeIndices(2, 2, 0, 0));
    }
}
