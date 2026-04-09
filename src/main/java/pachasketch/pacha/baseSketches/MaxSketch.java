package pachasketch.pacha.baseSketches;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class MaxSketch {
    public final int width;
    public final int depth;
    private final int[][] matrix;
    private transient HashFunction[] hashFunctions;
    private final int[] seeds;

    private int processedElements = 0;

    public MaxSketch(int width, int depth) {
        this.width = width;
        this.depth = depth;
        this.matrix = new int[depth][width];

        for (int i = 0; i < depth; i++) {
            Arrays.fill(this.matrix[i], Integer.MIN_VALUE);
        }

        this.hashFunctions = new HashFunction[this.depth];
        this.seeds = new int[this.depth];
        long masterSeed = 123456789L; // You can change this for a different hash family
        for (int i = 0; i < this.depth; i++) {
            seeds[i] = (int) (masterSeed + i * 7919); // Use a large prime offset
            hashFunctions[i] = Hashing.murmur3_32_fixed(seeds[i]);
        }
    }

    private int[] hash(String element){
        int[] indices = new int[depth];
        for (int i = 0; i < depth; i++) {
            int hash = hashFunctions[i].hashString(element, StandardCharsets.UTF_8).asInt();
            indices[i] = Math.floorMod(hash, width);
        }
        return indices;
    }


    public void update(String element, int value) {
        int[] indices = hash(element);
//        int maxEstimate = Integer.MAX_VALUE;
//        for (int i = 0; i < depth; i++) {
//            maxEstimate = Math.min(maxEstimate, matrix[i][indices[i]]);
//        }
//        processedElements++;
//
//        if (maxEstimate != Integer.MAX_VALUE && value < maxEstimate) {
//            return; // No update needed
//        }

        for (int i = 0; i < depth; i++) {
            matrix[i][indices[i]] = Math.max(matrix[i][indices[i]], value);
        }

    }

    public void updateBatch(List<String> elements, int value) {
        for (String element : elements) {
            update(element, value);
        }
    }

    public int query(String element) {
        int[] indices = hash(element);
        int maxEstimate = Integer.MAX_VALUE;
        for (int i = 0; i < depth; i++) {
            maxEstimate = Math.min(maxEstimate, matrix[i][indices[i]]);
        }
        return maxEstimate;
    }

    public int queryBatch(List<String> elements) {
        int maxEstimate = Integer.MAX_VALUE;
        for (String element : elements) {
            int estimate = query(element);
            maxEstimate = Math.min(maxEstimate, estimate);
        }
        return maxEstimate;
    }

    public void merge(MaxSketch other) {
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
                this.matrix[i][j] += other.matrix[i][j];
            }
        }
    }

    public double getSizeInMB(){
        int byteSize = 4 * depth * width;
        return byteSize / (1024.0 * 1024.0);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MaxSketch other = (MaxSketch) obj;
        if (this.width != other.width || this.depth != other.depth) {
            return false;
        }
        if (this.processedElements != other.processedElements) {
            return false;
        }
        if (!Arrays.equals(this.seeds, other.seeds)) {
            return false;
        }
        return Arrays.deepEquals(this.matrix, other.matrix);
    }


    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static MaxSketch fromJson(String json) {
        Gson gson = new Gson();
        MaxSketch sketch = gson.fromJson(json, MaxSketch.class);

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
