package omni;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import pachasketch.omni.CountMin;
import pachasketch.omni.Kmin;

public class CountMinTest {

    @Test
    void addAndQueryShouldReturnConsistentResults() {
        CountMin countMin = new CountMin(1, 10, 5, 0.1, 100);
        long attrValue = 12345L;
        long hx = 67890L;

        countMin.add(attrValue, hx);
        Kmin[] result = countMin.query(attrValue);

        for (Kmin kmin : result) {
            assertTrue(kmin.contains(hx));
        }
    }

    @Test
    void queryOnEmptySketchShouldReturnEmptyResults() {
        CountMin countMin = new CountMin(1, 10, 5, 0.1, 100);
        long attrValue = 12345L;

        Kmin[] result = countMin.query(attrValue);

        for (Kmin kmin : result) {
            assertTrue(kmin.sketch.isEmpty());
        }
    }

    @Test
    void getMemoryUsageShouldReturnCorrectValue() {
        CountMin countMin = new CountMin(1, 10, 5, 0.1, 100);
        long initialMemoryUsage = countMin.getMemoryUsage();

        countMin.add(12345L, 67890L);
        long updatedMemoryUsage = countMin.getMemoryUsage();

        assertTrue(updatedMemoryUsage > initialMemoryUsage);
    }

    @Test
    void addWithDifferentAttributesShouldNotInterfere() {
        CountMin countMin = new CountMin(1, 10, 5, 0.1, 100);
        long attrValue1 = 12345L;
        long attrValue2 = 54321L;
        long hx1 = 67890L;
        long hx2 = 9876L;

        countMin.add(attrValue1, hx1);
        countMin.add(attrValue2, hx2);

        Kmin[] result1 = countMin.query(attrValue1);
        Kmin[] result2 = countMin.query(attrValue2);

        for (Kmin kmin : result1) {
            assertTrue(kmin.contains(hx1));
            assertFalse(kmin.contains(hx2));
        }

        for (Kmin kmin : result2) {
            assertTrue(kmin.contains(hx2));
            assertFalse(kmin.contains(hx1));
        }
    }

    @Test
    void addWithSameAttributeAndDifferentIdsShouldNotConflict() {
        CountMin countMin = new CountMin(1, 10, 5, 0.1, 100);
        long attrValue = 12345L;
        long hx1 = 67890L;
        long hx2 = 9876L;

        countMin.add(attrValue, hx1);
        countMin.add(attrValue, hx2);

        Kmin[] result = countMin.query(attrValue);

        for (Kmin kmin : result) {
            assertTrue(kmin.contains(hx1));
            assertTrue(kmin.contains(hx2));
        }
    }
}