package pachasketch.omni;

import java.util.ArrayList;
import java.util.Random;

public class CountMin {

    public int attr;

    public int width;
    public int depth;
    public double deltaDS;
    public int maxSize;
    public Kmin[][] CM;

    final Random rn = new Random();

    public CountMin(int attr, int width, int depth, double deltaDS, int maxSize) {
        this.attr = attr;
        this.width = width;
        this.depth = depth;
        this.CM = new Kmin[depth][width];

        this.deltaDS = deltaDS;
        this.maxSize = maxSize;
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
        for (int i = 0; i < depth; i++) hash[i] = rn.nextInt(width);
        return hash;
    }

    public void add(long attrValue, long hx) {
        // Test if all element in A and B are consistent
        int[] hashes = hash(attrValue);
        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            CM[j][w].add(hx); // Hash in Sample based on id
        }
    }

    public Kmin[] query(long attrValue) {
        int[] hashes = hash(attrValue);

        Kmin[] result= new Kmin[depth];

        for (int j = 0; j < depth; j++) {
            int w = hashes[j];
            result[j] = CM[j][w];
        }
        return result;
    }

    public long getMemoryUsage() {
        long memoryUsage = 0;
        for (int j = 0; j < depth; j++) {
            for (int i = 0; i < width; i++) {
                //System.out.println("CM[" + j + "][" + i + "].curSampleSize = " + CM.get(j).get(i).curSampleSize);
                memoryUsage += CM[j][i].getMemoryUsage();
            }
        }
        return memoryUsage;
    }

}