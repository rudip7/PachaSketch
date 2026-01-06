package pachasketch.pacha.baseSketches;

import com.google.common.hash.HashFunction;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class ScalableBloomFilter implements Filter {
    private final long size;
    private final int numHashFunctions;
    private final LargeBitSet bitArray;
    private transient HashFunction[] hashFunctions;
    private final int[] seeds;
    private int processedElements = 0;

    public ScalableBloomFilter(long size, int numHashFunctions) {
        this.size = size;
        this.numHashFunctions = numHashFunctions;
        this.bitArray = new LargeBitSet(size);

        this.hashFunctions = new HashFunction[this.numHashFunctions];
        this.seeds = new int[numHashFunctions];
        long masterSeed = 123456789L; // You can change this for a different hash family
        for (int i = 0; i < numHashFunctions; i++) {
            seeds[i] = (int) (masterSeed + i * 7919); // Use a large prime offset
            hashFunctions[i] = com.google.common.hash.Hashing.murmur3_128(seeds[i]);
        }
    }

    public static ScalableBloomFilter buildFromGuarantees(double falsePositiveRate, long expectedElements) {
        // Calculate size and number of hash functions based on false positive rate and expected elements
        long size = (long) Math.ceil(-expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
        int numHashFunctions = (int) Math.ceil(Math.log(2) * size / expectedElements);
        return new ScalableBloomFilter(size, numHashFunctions);
    }

    private long[] hash(String element){
        long[] indices = new long[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            long hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asLong();
            indices[i] = Math.floorMod(hash, size);
        }
        return indices;
    }

    @Override
    public void update(String element) {
        long[] indices = hash(element);
        for (int i = 0; i < numHashFunctions; i++) {
            bitArray.set(indices[i]);
        }
        processedElements++;
    }

    @Override
    public void updateBatch(Collection<String> elements) {
        for (String element : elements) {
            update(element);
        }
    }

    @Override
    public boolean query(String element) {
        long[] indices = hash(element);
        for (int i = 0; i < numHashFunctions; i++) {
            if (!bitArray.get(indices[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> filterBatch(Collection<String> elements) {
        return elements.stream().filter(this::query).toList();
    }

    @Override
    public void merge(Filter other) {
        if (!(other instanceof ScalableBloomFilter)) {
            throw new IllegalArgumentException("Can only merge with another ScalableBloomFilter");
        }
        ScalableBloomFilter o = (ScalableBloomFilter) other;
        if (this.size != o.size || this.numHashFunctions != o.numHashFunctions) {
            throw new IllegalArgumentException("Cannot merge ScalableBloomFilters with different sizes or number of hash functions");
        }
        this.bitArray.or(o.bitArray);
        this.processedElements += o.processedElements;
    }

    @Override
    public String toJson() {
        return null;
    }

    @Override
    public double getSizeInMB() {
        long byteSize = (long) Math.ceil(size / 8.0);
        return byteSize / (1024.0 * 1024.0);
    }
}
