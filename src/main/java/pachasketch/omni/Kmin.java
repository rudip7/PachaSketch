package pachasketch.omni;

import java.util.Collections;
import java.util.Random;
import java.util.TreeSet;

public class Kmin{
    public TreeSet<Long> sketch;
    int K; // number of lowest values to store (max size of the sample)
    public int n = 0;
    public int b;
    public int curSampleSize;
    double delta;
    Random rn = new Random();
    long maxHash;
    long curTreeRoot = Long.MAX_VALUE;


    public Kmin(double delta, int maxSize) {
        this.K = maxSize;
        this.sketch = new TreeSet<>(Collections.reverseOrder());
        this.delta = delta;

        this.b = (int) Math.ceil(Math.log(4 * Math.pow(K, 2.5) / this.delta));
        this.maxHash = Math.min((int) Math.pow(2, b), Integer.MAX_VALUE);
    }

    public long hash(int x) {
        long k;
        rn.setSeed(x);
        // Hash function hashing x to [0, 1]^b with base = log(2m^2/delta)

        k = rn.nextLong(maxHash);

        return k;
    }


    public void add(long hx) {
        n++;
        if (curSampleSize < K) {
            sketch.add(hx);
            curSampleSize++;
            curTreeRoot = sketch.first();
        } else {
            if (hx < curTreeRoot) { // get tree root
                sketch.pollFirst();
                sketch.add(hx);
                curTreeRoot = sketch.first();
            }
        }
    }

    public long getMemoryUsage() {
        return (sketch.size() * (b + 32*3 + 1) + 32);
    }

    public boolean contains(long hx) {
        return sketch.contains(hx);
    }
}
