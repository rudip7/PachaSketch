package pachasketch.playgrounds;

import com.google.common.hash.Hashing;
import net.jpountz.xxhash.XXHashFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

public class HashBenchmark {

    static final int NUM_ITEMS = 10_000_000;
    static final int SEED = 0x9747b28c;
    static final int MODULO_LIMIT = 100000; // e.g., hash buckets

    public static void main(String[] args) {
        List<String> testData = generateTestStrings(NUM_ITEMS);

        testAndReport("String.hashCode() ^ seed", testData, HashBenchmark::basicHash);
        testAndReport("Guava MurmurHash3", testData, HashBenchmark::guavaMurmurHash);
        testAndReport("XXHash (lz4-java)", testData, HashBenchmark::xxHash32);
    }

    static List<String> generateTestStrings(int n) {
        List<String> data = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            data.add("item_" + i);
        }
        return data;
    }

    static void testAndReport(String name, List<String> data, HashFunction fn) {
        long start = System.nanoTime();
        int[] buckets = new int[MODULO_LIMIT];
        for (String s : data) {
            int hash = fn.hash(s);
            int index = Math.floorMod(hash, MODULO_LIMIT);
            buckets[index]++;
        }
        long duration = System.nanoTime() - start;

        double mean = Arrays.stream(buckets).average().orElse(0);
        double stdDev = Math.sqrt(Arrays.stream(buckets)
                .mapToDouble(count -> Math.pow(count - mean, 2))
                .average().orElse(0));

        System.out.printf("%-25s: %d ms | StdDev = %.2f | Mean = %.2f%n",
                name, duration / 1_000_000, stdDev, mean);
    }

    // --- Hash Functions ---

    static int basicHash(String input) {
        return input.hashCode() ^ SEED;
    }

    static int guavaMurmurHash(String input) {
        return Hashing.murmur3_32_fixed(SEED)
                .hashString(input, StandardCharsets.UTF_8)
                .asInt();
    }

    static final XXHashFactory xxFactory = XXHashFactory.fastestInstance();

    static int xxHash32(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        return xxFactory.hash32().hash(bytes, 0, bytes.length, SEED);
    }

    @FunctionalInterface
    interface HashFunction {
        int hash(String input);
    }
}
