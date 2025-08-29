package pachasketch.pacha.baseSketches;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BloomFilter implements Filter{
    private final int size;
    private final int numHashFunctions;
    private final boolean[] bitArray;
    private transient HashFunction[] hashFunctions;
    private final int[] seeds;

    private int processedElements = 0;

    public BloomFilter( int numHashFunctions, int size) {
        this.size = size;
        this.numHashFunctions = numHashFunctions;
        this.bitArray = new boolean[size];

        this.hashFunctions = new HashFunction[this.numHashFunctions];
        this.seeds = new int[numHashFunctions];
        long masterSeed = 123456789L; // You can change this for a different hash family
        for (int i = 0; i < numHashFunctions; i++) {
            seeds[i] = (int) (masterSeed + i * 7919); // Use a large prime offset
            hashFunctions[i] = Hashing.murmur3_32_fixed(seeds[i]);
        }
    }

    public static BloomFilter buildFromGuarantees(double falsePositiveRate, int expectedElements) {
        // Calculate size and number of hash functions based on false positive rate and expected elements
        int size = (int) Math.ceil(-expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
        int numHashFunctions = (int) Math.ceil(Math.log(2) * size / expectedElements);
        return new BloomFilter(numHashFunctions, size);
    }

    private int[] hash(String element){
        int[] indices = new int[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
            indices[i] = Math.floorMod(hash, size);
        }
        return indices;
    }

    @Override
    public void update(String element) {
        int[] indices = hash(element);
        for (int i = 0; i < numHashFunctions; i++) {
            bitArray[indices[i]] = true;
        }
        processedElements++;
    }

    @Override
    public void updateBatch(Collection<String> elements) {
        int[] indices = new int[numHashFunctions];
        for (String element : elements) {
            for (int i = 0; i < numHashFunctions; i++) {
                int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
                indices[i] = Math.floorMod(hash, size);
            }
            for (int index : indices) {
                bitArray[index] = true;
            }
            processedElements++;
        }
    }

    @Override
    public boolean query(String element) {
        int[] indices = hash(element);
        for (int i = 0; i < numHashFunctions; i++) {
            if (!bitArray[indices[i]]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> filterBatch(Collection<String> elements) {
        ArrayList<String> filtered = new ArrayList<>(elements.size());
        int[] indices = new int[numHashFunctions];
        for (String element : elements) {
            boolean found = true;
            for (int i = 0; i < numHashFunctions; i++) {
                int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
                indices[i] = Math.floorMod(hash, size);
                if (!bitArray[indices[i]]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                filtered.add(element);
            }
        }
        return filtered;
    }

    @Override
    public void merge(Filter other) {
        if (!(other instanceof BloomFilter)) {
            throw new IllegalArgumentException("Cannot merge with a non-BloomFilter instance.");
        }
        BloomFilter otherBloomFilter = (BloomFilter) other;

        if (this.size != otherBloomFilter.size || this.numHashFunctions != otherBloomFilter.numHashFunctions) {
            throw new IllegalArgumentException("Cannot merge BloomFilter instances with different sizes or hash functions.");
        }

        for (int i = 0; i < this.numHashFunctions; i++) {
            if (this.seeds[i] != otherBloomFilter.seeds[i]) {
                throw new IllegalArgumentException("Cannot merge BloomFilter instances with different seeds.");
            }
        }

        for (int i = 0; i < this.size; i++) {
            this.bitArray[i] |= otherBloomFilter.bitArray[i];
        }
    }

    @Override
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Filter fromJson(String json) {
        Gson gson = new Gson();
        BloomFilter bloomFilter = gson.fromJson(json, BloomFilter.class);

        // Rebuild hashFunctions using the seeds
        bloomFilter.hashFunctions = new HashFunction[bloomFilter.numHashFunctions];
        for (int i = 0; i < bloomFilter.numHashFunctions; i++) {
            bloomFilter.hashFunctions[i] = Hashing.murmur3_32_fixed(bloomFilter.seeds[i]);
        }
        return bloomFilter;
    }

    public int getProcessedElements() {
        return processedElements;
    }

}
