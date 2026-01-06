package pachasketch.pacha.baseSketches;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class CountSketch {
    private final int width;
    private final int depth;
    private final int[][] counters;
    private transient HashFunction[] hashFunctions;
    private transient HashFunction[] signFunctions;
    private final int[] seeds;
    private final int[] signSeeds;
    private int processedElements = 0;


    public CountSketch(int width, int depth) {
        this.width = width;
        this.depth = depth;
        this.counters = new int[depth][width];

        this.hashFunctions = new HashFunction[this.depth];
        this.signFunctions = new HashFunction[this.depth];
        this.seeds = new int[this.depth];
        this.signSeeds = new int[this.depth];
        long masterSeed = 123456789L; // Base seed for hash functions
        long signSeedBase = 987654321L; // Base seed for sign functions

        for (int i = 0; i < this.depth; i++) {
            seeds[i] = (int) (masterSeed + i * 7919); // Use a large prime offset
            signSeeds[i] = (int) (signSeedBase + i * 7919);
            hashFunctions[i] = Hashing.murmur3_32_fixed(seeds[i]);
            signFunctions[i] = Hashing.murmur3_32_fixed(signSeeds[i]);
        }
    }

    public static CountSketch buildFromGuarantees(double epsilon, double delta) {
        // Calculate width and depth based on epsilon and delta
        int width = (int) Math.ceil(3.0 / (epsilon * epsilon));
        int depth = (int) Math.ceil(Math.log(1 / delta));
        return new CountSketch(width, depth);
    }

    private int[] hash(String element) {
        int[] indices = new int[depth];
        for (int i = 0; i < depth; i++) {
            int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
            indices[i] = Math.floorMod(hash, width);
        }
        return indices;
    }

    private int[] getSigns(String element) {
        int[] signs = new int[depth];
        for (int i = 0; i < depth; i++) {
            int signHash = signFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
            signs[i] = (signHash % 2 == 0) ? 1 : -1; // Map to +1 or -1
        }
        return signs;
    }

    public void updateBatch(List<String> elements, int increment) {
        int[] indices = new int[depth];
        int[] signs = new int[depth];
        for (String element : elements) {
            indices = hash(element);
            signs = getSigns(element);
            for (int i = 0; i < depth; i++) {
                counters[i][indices[i]] += signs[i] * increment;
            }
            processedElements++;
        }
    }
    public void update(String element, int increment) {
        int[] indices = hash(element);
        int[] signs = getSigns(element);
        for (int i = 0; i < depth; i++) {
            counters[i][indices[i]] += signs[i] * increment;
        }
    }
    public void update(String element) {
        update(element, 1);
    }

    public int query(String element) {
        int[] indices = hash(element);
        int[] signs = getSigns(element);
        int[] estimates = new int[depth];
        for (int i = 0; i < depth; i++) {
            estimates[i] = counters[i][indices[i]] * signs[i];
        }
        Arrays.sort(estimates);
        return estimates[depth / 2]; // Return the median estimate
    }

    public int queryBatch(List<String> elements) {
        int[] indices = new int[depth];
        int[] signs = new int[depth];
        int estimate = 0;
        for (String element : elements) {
            indices = hash(element);
            signs = getSigns(element);

            int[] estimates = new int[depth];
            for (int i = 0; i < depth; i++) {
                estimates[i] = counters[i][indices[i]] * signs[i];
            }
            Arrays.sort(estimates);
            estimate += estimates[depth / 2];
        }

        return estimate; // Return the median estimate
    }

    public void merge(CountSketch other) {
        if (this.width != other.width || this.depth != other.depth) {
            throw new IllegalArgumentException("Cannot merge CountSketch instances with different dimensions.");
        }
        for (int i = 0; i < depth; i++) {
            if (this.seeds[i] != other.seeds[i] || this.signSeeds[i] != other.signSeeds[i]) {
                throw new IllegalArgumentException("Cannot merge CountSketch instances with different seeds.");
            }
        }
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                this.counters[i][j] += other.counters[i][j];
            }
        }
    }

    public double getSizeInMB() {
        int byteSize = 4 * depth * width;
        return byteSize / (1024.0 * 1024.0);
    }
}