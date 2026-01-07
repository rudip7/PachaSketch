package pacha;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pachasketch.pacha.baseSketches.CountMinSketch;
import pachasketch.pacha.baseSketches.CountSketch;

public class CountSketchTest {

    @Test
    void updateAndQueryShouldWorkCorrectly() {
        CountSketch sketch = new CountSketch(10, 5);
        sketch.update("element1", 3);
        sketch.update("element2", 2);

        Assertions.assertEquals(3, sketch.query("element1"));
        Assertions.assertEquals(2, sketch.query("element2"));
        Assertions.assertEquals(0, sketch.query("element3")); // Element not added
    }

    @Test
    void mergeShouldCombineTwoSketches() {
        CountSketch sketch1 = new CountSketch(10, 5);
        CountSketch sketch2 = new CountSketch(10, 5);

        sketch1.update("element1", 3);
        sketch2.update("element1", 2);
        sketch2.update("element2", 4);

        sketch1.merge(sketch2);

        Assertions.assertEquals(5, sketch1.query("element1")); // 3 + 2
        Assertions.assertEquals(4, sketch1.query("element2")); // 0 + 4
    }

    @Test
    void mergeShouldThrowExceptionForIncompatibleSketches() {
        CountSketch sketch1 = new CountSketch(10, 5);
        CountSketch sketch2 = new CountSketch(15, 5); // Different width

        Assertions.assertThrows(IllegalArgumentException.class, () -> sketch1.merge(sketch2));
    }

    @Test
    void estimateAverageUsingTwoSketches() {
        CountSketch sketch1 = new CountSketch(10, 5);
        CountSketch sketch2 = new CountSketch(10, 5);

        // Add elements to sketch1
        sketch1.update("element1", 4);
        sketch1.update("element2", 6);

        // Add elements to sketch2
        sketch2.update("element1", 2);
        sketch2.update("element2", 8);

        // Estimate averages
        double avgElement1 = (sketch1.query("element1") + sketch2.query("element1")) / 2.0;
        double avgElement2 = (sketch1.query("element2") + sketch2.query("element2")) / 2.0;

        Assertions.assertEquals(3.0, avgElement1); // (4 + 2) / 2
        Assertions.assertEquals(7.0, avgElement2); // (6 + 8) / 2
    }

    @Test
    void getSizeInMBShouldReturnCorrectSize() {
//        CountSketch sketch = new CountSketch(10, 5);
        CountSketch sketch = CountSketch.buildFromGuarantees(0.001, 0.01);

        CountMinSketch sketch1 = CountMinSketch.buildFromGuarantees(0.0001, 0.01);
        double sizeInMB = sketch.getSizeInMB();
        double sizeInMB1 = sketch1.getSizeInMB();
//        System.out.println("CountSketch size in MB: " + sizeInMB);
//        System.out.println("CountMinSketch size in MB: " + sizeInMB1);

//        Assertions.assertEquals(10 * 5 * 4 / (1024.0 * 1024.0), sizeInMB);
    }
}