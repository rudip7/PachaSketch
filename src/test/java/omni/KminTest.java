package omni;

import org.junit.jupiter.api.Test;
import pachasketch.omni.Kmin;

import java.util.Collections;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class KminTest {

    @Test
    void addShouldStoreValuesUpToMaxSize() {
        Kmin kmin = new Kmin(0.1, 3);
        kmin.add(10L);
        kmin.add(5L);
        kmin.add(20L);

        TreeSet<Long> expected = new TreeSet<>(Collections.reverseOrder());
        expected.add(10L);
        expected.add(5L);
        expected.add(20L);

        assertEquals(expected, kmin.sketch);
    }

    @Test
    void addShouldReplaceLargestValueWhenExceedingMaxSize() {
        Kmin kmin = new Kmin(0.1, 3);
        kmin.add(10L);
        kmin.add(5L);
        kmin.add(20L);
        kmin.add(3L);

        TreeSet<Long> expected = new TreeSet<>(Collections.reverseOrder());
        expected.add(5L);
        expected.add(3L);
        expected.add(10L);

        assertEquals(expected, kmin.sketch);
    }

    @Test
    void addShouldNotReplaceWhenValueIsLargerThanCurrentTreeRoot() {
        Kmin kmin = new Kmin(0.1, 3);
        kmin.add(10L);
        kmin.add(5L);
        kmin.add(15L);
        kmin.add(20L);

        TreeSet<Long> expected = new TreeSet<>(Collections.reverseOrder());
        expected.add(10L);
        expected.add(5L);
        expected.add(15L);

        assertEquals(expected, kmin.sketch);
        assertEquals(kmin.n, 4);
    }

    @Test
    void getMemoryUsageShouldReturnCorrectValue() {
        Kmin kmin = new Kmin(0.1, 3);
        kmin.add(10L);
        kmin.add(5L);
        kmin.add(20L);

        long expectedMemoryUsage = (3 * (kmin.b + 32 * 3 + 1) + 32);
        assertEquals(expectedMemoryUsage, kmin.getMemoryUsage());
    }

    @Test
    void hashShouldReturnConsistentValuesForSameInput() {
        Kmin kmin = new Kmin(0.1, 3);
        long hash1 = kmin.hash(42);
        long hash2 = kmin.hash(42);

        assertEquals(hash1, hash2);
    }

    @Test
    void hashShouldReturnDifferentValuesForDifferentInputs() {
        Kmin kmin = new Kmin(0.1, 3);
        long hash1 = kmin.hash(42);
        long hash2 = kmin.hash(43);

        assertNotEquals(hash1, hash2);
    }
}