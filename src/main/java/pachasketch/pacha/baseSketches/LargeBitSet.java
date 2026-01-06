package pachasketch.pacha.baseSketches;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class LargeBitSet {
    private static final int CHUNK_SIZE = Integer.MAX_VALUE; // Max size for each BitSet
    private final List<BitSet> bitSets = new ArrayList<>();
    private final long size;

    public LargeBitSet(long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive.");
        }
        this.size = size;

        // Create enough BitSet chunks to cover the required size
        long remainingBits = size;
        while (remainingBits > 0) {
            bitSets.add(new BitSet((int) Math.min(CHUNK_SIZE, remainingBits)));
            remainingBits -= CHUNK_SIZE;
        }
    }

    public void set(long index) {
        checkIndex(index);
        int chunkIndex = (int) (index / CHUNK_SIZE);
        int bitIndex = (int) (index % CHUNK_SIZE);
        bitSets.get(chunkIndex).set(bitIndex);
    }

    public boolean get(long index) {
        checkIndex(index);
        int chunkIndex = (int) (index / CHUNK_SIZE);
        int bitIndex = (int) (index % CHUNK_SIZE);
        return bitSets.get(chunkIndex).get(bitIndex);
    }

    public void clear(long index) {
        checkIndex(index);
        int chunkIndex = (int) (index / CHUNK_SIZE);
        int bitIndex = (int) (index % CHUNK_SIZE);
        bitSets.get(chunkIndex).clear(bitIndex);
    }

    public long size() {
        return size;
    }

    private void checkIndex(long index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    public void or(LargeBitSet bitArray) {
        if (this.size != bitArray.size) {
            throw new IllegalArgumentException("BitSets must be of the same size to perform OR operation.");
        }
        for (int i = 0; i < this.bitSets.size(); i++) {
            this.bitSets.get(i).or(bitArray.bitSets.get(i));
        }
    }
}