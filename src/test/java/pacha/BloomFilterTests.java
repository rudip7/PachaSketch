package pacha;

import org.junit.jupiter.api.Test;
import pachasketch.pacha.baseSketches.BloomFilter;
import pachasketch.pacha.baseSketches.Filter;

import static org.junit.jupiter.api.Assertions.*;

class BloomFilterTests {

    @Test
    void toJsonProducesValidJson() {
        BloomFilter bloomFilter = new BloomFilter(3, 100);
        bloomFilter.update("element1");
        bloomFilter.update("element2");

        String json = bloomFilter.toJson();

        assertNotNull(json);
        assertTrue(json.contains("\"size\":100"));
        assertTrue(json.contains("\"numHashFunctions\":3"));
        assertTrue(json.contains("\"processedElements\":2"));
    }

    @Test
    void toJsonAndFromJsonAreConsistent() {
        BloomFilter bloomFilter = new BloomFilter(3, 100);
        bloomFilter.update("element1");
        bloomFilter.update("element2");

        String json = bloomFilter.toJson();
        BloomFilter reconstructedBloomFilter = BloomFilter.fromJson(json);

        assertEquals(bloomFilter, reconstructedBloomFilter);
        assertEquals(bloomFilter.getProcessedElements(), reconstructedBloomFilter.getProcessedElements());
        assertTrue(reconstructedBloomFilter.query("element1"));
        assertTrue(reconstructedBloomFilter.query("element2"));
        assertFalse(reconstructedBloomFilter.query("nonexistent"));
    }
}