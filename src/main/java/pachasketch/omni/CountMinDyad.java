package pachasketch.omni;

import java.util.ArrayList;
import java.util.Random;
import java.util.TreeSet;

public class CountMinDyad {

    public int width;
    public int depth;
    public double deltaDS;
    public int maxSize;
    public int dyadicRangeBits;
    public Kmin[][] CM;

    int attr;
    long intervalSize;
    final Random rn = new Random();
    int n = 0;

    public CountMinDyad(int attr, long intervalSize, int width, int depth, double deltaDS, int maxSize, int dyadicRangeBits) {
        this.attr = attr;
        this.intervalSize = intervalSize;

        this.width = width;
        this.depth = depth;
        this.deltaDS = deltaDS;
        this.maxSize = maxSize;
        this.dyadicRangeBits = dyadicRangeBits;

        this.CM = new Kmin[depth][width];
        initSketch();
    }


    public void initSketch() {
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                CM[j][i] = new Kmin(deltaDS, maxSize);
            }
        }
    }

    int[] hash(long attrValue) {
        int[] hash = new int[depth];
        rn.setSeed(attrValue);
        for (int i = 0; i < depth; i++) {
            hash[i] = rn.nextInt(width);
        }
        return hash;
    }
    public long getRangeSignature(long start, long stop) {
        long sum = start;
        sum<<=dyadicRangeBits;
        sum |= stop;
        return sum;
    }

    public void add(long lower, long higher, long hx) {
        // Test if all element in A and B are consistent
        long sig = getRangeSignature(lower, higher);
        //if (intervalSize == 34 && attr == 1) {
        //    System.out.println("sig: " + sig);
        //}

//        if (intervalSize == 1 && attr == 8) {
//            if (lower != 8594268983L) {
//                System.out.println("lower: " + lower);
//            }
//            System.out.println("sig: " + sig);
//        }
        int[] hashes = hash(sig);
        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            CM[j][w].add(hx); // Hash in Sample based on id
        }
        n += 1;
    }

    public TreeSet<Long>[] rangeQuery(long lower, long higher, int[] seenN) {
        TreeSet<Long>[] set = new TreeSet[depth];
        long sig = getRangeSignature(lower, higher);
        int[] hashes = hash(sig);
        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            set[j] = new TreeSet<>(CM[j][w].sketch);
            seenN[j] += CM[j][w].n;
            // Hash in Sample based on id
        }
        return set;
    }
    public long getMemoryUsage() {
        long memoryUsage = 0;
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                memoryUsage += CM[j][i].getMemoryUsage();
            }
        }
        return memoryUsage;
    }
}