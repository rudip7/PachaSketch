package pacha;

import org.junit.jupiter.api.Test;
import pachasketch.pacha.components.NumericalBitmap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NumericalBitmapTest {

    @Test
    void updateSetsCorrectBitForPositiveValue() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(25);
        assertTrue(bitmap.query(25));
    }

    @Test
    void updateSetsCorrectBitForNegativeValue() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(-25);
        assertTrue(bitmap.query(-25));
    }

    @Test
    void queryReturnsFalseForValueOutsideLimit() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        assertFalse(bitmap.query(1000));
    }

    @Test
    void mergeCombinesTwoBitmapsCorrectly() {
        NumericalBitmap bitmap1 = new NumericalBitmap(10, 100);
        NumericalBitmap bitmap2 = new NumericalBitmap(10, 100);

        bitmap1.update(25);
        bitmap2.update(-25);

        bitmap1.merge(bitmap2);

        assertTrue(bitmap1.query(25));
        assertTrue(bitmap1.query(-25));
    }

    @Test
    void increaseExponentHandlesLargeValues() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(1000);
        assertTrue(bitmap.query(1000));
    }

    @Test
    void toJsonAndFromJsonPreserveState() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(25);
        bitmap.update(-25);

        String json = bitmap.toJson();
        NumericalBitmap restoredBitmap = NumericalBitmap.fromJson(json);

        assertEquals(bitmap.query(25), restoredBitmap.query(25));
        assertEquals(bitmap.query(-25), restoredBitmap.query(-25));
    }

    @Test
    void queryReturnsFalseForUnsetValue() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        assertFalse(bitmap.query(50));
    }

    @Test
    void updateHandlesZeroValue() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(0);
        assertTrue(bitmap.query(0));
    }

    @Test
    void mergeThrowsExceptionForDifferentBase() {
        NumericalBitmap bitmap1 = new NumericalBitmap(10, 100);
        NumericalBitmap bitmap2 = new NumericalBitmap(5, 100);

        assertThrows(IllegalArgumentException.class, () -> bitmap1.merge(bitmap2));
    }

    @Test
    void mergeThrowsExceptionForDifferentSizePerSide() {
        NumericalBitmap bitmap1 = new NumericalBitmap(10, 100);
        NumericalBitmap bitmap2 = new NumericalBitmap(10, 200);

        assertThrows(IllegalArgumentException.class, () -> bitmap1.merge(bitmap2));
    }

    @Test
    void increaseExponentIncreasesExponentCorrectly() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        int initialExponent = bitmap.getExponent();
        bitmap.update(120);
        assertEquals(initialExponent + 1, bitmap.getExponent());
        assertTrue(bitmap.query(120));
        assertTrue(bitmap.query(121));
    }

    @Test
    void pruneBAdicArrayRemovesUnsetIndices() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(25);
        bitmap.update(-15);

        List<int[]> bAdicArray = new ArrayList<>();
        bAdicArray.add(new int[]{0, 25});
        bAdicArray.add(new int[]{0, -15});
        bAdicArray.add(new int[]{0, 50});

        List<int[]> prunedArray = bitmap.pruneBAdicArray(bAdicArray);

        assertEquals(2, prunedArray.size());
        assertArrayEquals(new int[]{0, 25}, prunedArray.get(0));
        assertArrayEquals(new int[]{0, -15}, prunedArray.get(1));
    }

    @Test
    void pruneBAdicArrayHandlesEmptyInput() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);

        List<int[]> bAdicArray = new ArrayList<>();
        List<int[]> prunedArray = bitmap.pruneBAdicArray(bAdicArray);

        assertTrue(prunedArray.isEmpty());
    }

    @Test
    void pruneBAdicArrayHandlesLevelGreaterThanExponent() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(250);

        List<int[]> bAdicArray = new ArrayList<>();
        bAdicArray.add(new int[]{1, 25});
        bAdicArray.add(new int[]{1, 50});

        List<int[]> prunedArray = bitmap.pruneBAdicArray(bAdicArray);

        assertEquals(1, prunedArray.size());
        assertArrayEquals(new int[]{1, 25}, prunedArray.get(0));
    }

    @Test
    void pruneBAdicArrayHandlesLevelLessThanExponent() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(25);
        bitmap.update(35);

        List<int[]> bAdicArray = new ArrayList<>();
        bAdicArray.add(new int[]{1, 2});
        bAdicArray.add(new int[]{1, 3});
        bAdicArray.add(new int[]{1, 5});

        List<int[]> prunedArray = bitmap.pruneBAdicArray(bAdicArray);

        assertEquals(2, prunedArray.size());
        assertArrayEquals(new int[]{1, 2}, prunedArray.get(0));
        assertArrayEquals(new int[]{1, 3}, prunedArray.get(1));
    }

    @Test
    void pruneBAdicArrayHandlesNegativeIndices() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(-250);

        List<int[]> bAdicArray = new ArrayList<>();
        bAdicArray.add(new int[]{1, -25});
        bAdicArray.add(new int[]{1, -50});

        List<int[]> prunedArray = bitmap.pruneBAdicArray(bAdicArray);

        assertEquals(1, prunedArray.size());
        assertArrayEquals(new int[]{1, -25}, prunedArray.get(0));
    }

    @Test
    void pruneBAdicIndicesRemovesUnsetIndicesForPositiveValues() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(25);
        bitmap.update(35);

        int[] bAdicIndices = {2, 3, 5};
        int[] prunedIndices = bitmap.pruneBAdicIndices(1, bAdicIndices);

        assertArrayEquals(new int[]{2, 3}, prunedIndices);
    }

    @Test
    void pruneBAdicIndicesRemovesUnsetIndicesForNegativeValues() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(-25);
        bitmap.update(-35);

        int[] bAdicIndices = {-2, -3, -5};
        int[] prunedIndices = bitmap.pruneBAdicIndices(1, bAdicIndices);

        assertArrayEquals(new int[]{-2, -3}, prunedIndices);
    }

    @Test
    void pruneBAdicIndicesHandlesEmptyInput() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);

        int[] bAdicIndices = {};
        int[] prunedIndices = bitmap.pruneBAdicIndices(1, bAdicIndices);

        assertArrayEquals(new int[]{}, prunedIndices);
    }

    @Test
    void pruneBAdicIndicesHandlesLevelGreaterThanExponent() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(25);

        int[] bAdicIndices = {2, 5};
        int[] prunedIndices = bitmap.pruneBAdicIndices(1, bAdicIndices);

        assertArrayEquals(new int[]{2}, prunedIndices);
    }

    @Test
    void pruneBAdicIndicesHandlesLevelLessThanExponent() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);
        bitmap.update(250);
        bitmap.update(350);

        int[] bAdicIndices = {250, 255, 300};
        int[] prunedIndices = bitmap.pruneBAdicIndices(0, bAdicIndices);

        assertArrayEquals(new int[]{250, 255}, prunedIndices);
    }

    @Test
    void pruneBAdicIndicesThrowsExceptionForMixedPositiveAndNegativeValues() {
        NumericalBitmap bitmap = new NumericalBitmap(10, 100);

        int[] bAdicIndices = {2, -3, 5};
        assertThrows(IllegalArgumentException.class, () -> bitmap.pruneBAdicIndices(1, bAdicIndices));
    }
}