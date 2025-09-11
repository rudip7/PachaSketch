package pacha;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import pachasketch.pacha.baseSketches.CountMinSketch;

public class CountMinSketchTest {

    @Test
    void fromJsonShouldRecreateObjectWithSameProperties() {
        CountMinSketch original = new CountMinSketch(10, 5);
        original.update("element1");
        original.update("element2");

        String json = original.toJson();
        CountMinSketch recreated = CountMinSketch.fromJson(json);

        Assertions.assertEquals(original.query("element1"), recreated.query("element1"));
        Assertions.assertEquals(original.query("element2"), recreated.query("element2"));
        Assertions.assertEquals(original.getProcessedElements(), recreated.getProcessedElements());
    }

    @Test
    void fromJsonShouldHandleEmptyJson() {
        String emptyJson = "{}";
        CountMinSketch recreated = CountMinSketch.fromJson(emptyJson);

        Assertions.assertNotNull(recreated);
        Assertions.assertEquals(0, recreated.getProcessedElements());
    }

    @Test
    void fromJsonShouldThrowExceptionForInvalidJson() {
        String invalidJson = "{invalid}";

        Assertions.assertThrows(com.google.gson.JsonSyntaxException.class, () -> {
            CountMinSketch.fromJson(invalidJson);
        });
    }

    @Test
    void fromJsonShouldRebuildHashFunctionsCorrectly() {
        CountMinSketch original = new CountMinSketch(10, 5);
        original.update("element1");
        original.update("element2");
        String json = original.toJson();
        CountMinSketch recreated = CountMinSketch.fromJson(json);

        Assertions.assertNotNull(recreated);
        Assertions.assertEquals(original, recreated);
    }
}