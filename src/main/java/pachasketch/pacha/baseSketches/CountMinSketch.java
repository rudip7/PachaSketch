package pachasketch.pacha.baseSketches;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CountMinSketch {
    private final int width;
    private final int depth;
    private final int[][] counters;
    private transient HashFunction[] hashFunctions;
    private final int[] seeds;

    private int processedElements = 0;

    public CountMinSketch(int width, int depth) {
        this.width = width;
        this.depth = depth;
        this.counters = new int[depth][width];

        this.hashFunctions = new HashFunction[this.depth];
        this.seeds = new int[this.depth];
        long masterSeed = 123456789L; // You can change this for a different hash family
        for (int i = 0; i < this.depth; i++) {
            seeds[i] = (int) (masterSeed + i * 7919); // Use a large prime offset
            hashFunctions[i] = Hashing.murmur3_32_fixed(seeds[i]);
        }
    }

    public static CountMinSketch buildFromGuarantees(double epsilon, double delta) {
        // Calculate width and depth based on epsilon and delta
        int width = (int) Math.ceil(Math.E / epsilon);
        int depth = (int) Math.ceil(Math.log(1 / delta));
        return new CountMinSketch(width, depth);
    }

    private int[] hash(String element){
        int[] indices = new int[depth];
        for (int i = 0; i < depth; i++) {
            int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
            indices[i] = Math.floorMod(hash, width);
        }
        return indices;
    }

    public void update(String element) {
        int[] indices = hash(element);
        for (int i = 0; i < depth; i++) {
            counters[i][indices[i]]++;
        }
        processedElements++;
    }

    public void updateBatch(List<String> elements) {
        int[] indices = new int[depth];
        for (String element : elements) {
            for (int i = 0; i < depth; i++) {
                int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
                indices[i] = Math.floorMod(hash, width);
            }
            for (int i = 0; i < depth; i++) {
                counters[i][indices[i]]++;
            }
            processedElements++;
        }
    }

    public int query(String element) {
        int[] indices = hash(element);
        int minEstimate = Integer.MAX_VALUE;
        for (int i = 0; i < depth; i++) {
            minEstimate = Math.min(minEstimate, counters[i][indices[i]]);
        }
        return minEstimate;
    }

    public int queryBatch(List<String> elements) {
        int totalEstimate = 0;
        int[] indices = new int[depth];
        for (String element : elements) {
            for (int i = 0; i < depth; i++) {
                int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
                indices[i] = Math.floorMod(hash, width);
            }
            int minEstimate = Integer.MAX_VALUE;
            for (int i = 0; i < depth; i++) {
                minEstimate = Math.min(minEstimate, counters[i][indices[i]]);
            }
            totalEstimate += minEstimate;
        }
        return totalEstimate;
    }

    public void merge(CountMinSketch other) {
        if (this.width != other.width || this.depth != other.depth) {
            throw new IllegalArgumentException("Cannot merge CountMinSketch instances with different dimensions.");
        }
        for (int i = 0; i < depth; i++) {
            if (this.seeds[i] != other.seeds[i]) {
                throw new IllegalArgumentException("Cannot merge CountMinSketch instances with different seeds.");
            }
        }
        for (int i = 0; i < depth; i++) {
            for (int j = 0; j < width; j++) {
                this.counters[i][j] += other.counters[i][j];
            }
        }
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static CountMinSketch fromJson(String json) {
        Gson gson = new Gson();
        CountMinSketch sketch = gson.fromJson(json, CountMinSketch.class);

        // Rebuild hashFunctions using the seeds
        sketch.hashFunctions = new HashFunction[sketch.depth];
        for (int i = 0; i < sketch.depth; i++) {
            sketch.hashFunctions[i] = Hashing.murmur3_32_fixed(sketch.seeds[i]);
        }

        return sketch;
    }

    public int getProcessedElements() {
        return processedElements;
    }

}
