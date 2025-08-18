package pachasketch.pacha.components;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class NumericalBitmap {
    private final int base;
    private int exponent;
    private int bucketSize;
    private final int sizePerSide;
    private int limit;
    private BitSet positiveBitmap;
    private BitSet negativeBitmap;

    public NumericalBitmap(int base, int sizePerSide) {
        this.base = base;
        this.exponent = 0;
        this.bucketSize = 1;
        this.sizePerSide = sizePerSide + (sizePerSide % base);
        this.limit = this.sizePerSide;
        this.positiveBitmap = new BitSet(this.sizePerSide);
        this.negativeBitmap = new BitSet(this.sizePerSide);
    }

    private void increaseExponent() {
        this.exponent++;
        this.bucketSize *= this.base;
        this.limit *= this.base;

        // Compress positive bitmap
        BitSet compressedPositiveBitmap = new BitSet();
        for (int i = 0; i < this.positiveBitmap.length(); i += this.base) {
            boolean anySet = false;
            for (int j = 0; j < this.base && (i + j) < this.positiveBitmap.length(); j++) {
                if (this.positiveBitmap.get(i + j)) {
                    anySet = true;
                    break;
                }
            }
            if (anySet) {
                compressedPositiveBitmap.set(i / this.base);
            }
        }
        this.positiveBitmap = compressedPositiveBitmap;

        // Compress negative bitmap
        BitSet compressedNegativeBitmap = new BitSet();
        for (int i = 0; i < this.negativeBitmap.length(); i += this.base) {
            boolean anySet = false;
            for (int j = 0; j < this.base && (i + j) < this.negativeBitmap.length(); j++) {
                if (this.negativeBitmap.get(i + j)) {
                    anySet = true;
                    break;
                }
            }
            if (anySet) {
                compressedNegativeBitmap.set(i / this.base);
            }
        }
        this.negativeBitmap = compressedNegativeBitmap;
    }

    public void update(int value) {
        while (Math.abs(value) >= this.limit) {
            increaseExponent();
        }
        if (value >= 0) {
            int idx = value / this.bucketSize;
            this.positiveBitmap.set(idx);
        } else {
            int idx = -value / this.bucketSize - 1;
            this.negativeBitmap.set(idx);
        }
    }

    public boolean query(int value) {
        if (Math.abs(value) >= this.limit) {
            return false;
        }
        if (value >= 0) {
            int idx = value / this.bucketSize;
            return this.positiveBitmap.get(idx);
        } else {
            int idx = -value / this.bucketSize - 1;
            return this.negativeBitmap.get(idx);
        }
    }

    public void merge(NumericalBitmap other) {
        if (this.base != other.base || this.sizePerSide != other.sizePerSide) {
            throw new IllegalArgumentException("Base and sizePerSide must match for merging.");
        }

        while (this.exponent < other.exponent) {
            this.increaseExponent();
        }
        while (other.exponent < this.exponent) {
            other.increaseExponent();
        }

        this.positiveBitmap.or(other.positiveBitmap);
        this.negativeBitmap.or(other.negativeBitmap);
    }

    public List<int[]> pruneBAdicArray(List<int[]> bAdicArray) {
        List<int[]> prunedArray = new ArrayList<>(bAdicArray.size());

        for (int[] bAdic : bAdicArray) {
            int level = bAdic[0];
            int index = bAdic[1];
            boolean isSet = false;

            if (level == this.exponent) {
                if (index >= 0) {
                    if (index < this.positiveBitmap.length()) {
                        isSet = this.positiveBitmap.get(index);
                    }
                } else {
                    int idx = -index - 1;
                    if (idx < this.negativeBitmap.length()) {
                        isSet = this.negativeBitmap.get(idx);
                    }
                }
            } else if (level > this.exponent) {
                int levelDiff = level - this.exponent;
                int scale = (int) Math.pow(this.base, levelDiff);
                if (index >= 0) {
                    int start = index * scale;
                    int end = start + scale;
                    for (int i = start; i < end && i < this.positiveBitmap.length(); i++) {
                        if (this.positiveBitmap.get(i)) {
                            isSet = true;
                            break;
                        }
                    }
                } else {
                    int start = -index * scale - 1;
                    int end = start + scale;
                    for (int i = start; i < end && i < this.negativeBitmap.length(); i++) {
                        if (this.negativeBitmap.get(i)) {
                            isSet = true;
                            break;
                        }
                    }
                }
            } else { // level < this.exponent
                int levelDiff = this.exponent - level;
                int scale = (int) Math.pow(this.base, levelDiff);
                int idx = index / scale;
                if (index >= 0) {
                    if (idx < this.positiveBitmap.length()) {
                        isSet = this.positiveBitmap.get(idx);
                    }
                } else {
                    idx = -idx - 1;
                    if (idx < this.negativeBitmap.length()) {
                        isSet = this.negativeBitmap.get(idx);
                    }
                }
            }

            if (isSet) {
                prunedArray.add(bAdic);
            }
        }

        return prunedArray;
    }

    public int[] pruneBAdicIndices(int level, int[] bAdicIndices) {
        boolean positive = false;

        // Check if all values in bAdicIndices are >= 0 or < 0
        boolean allPositive = true;
        boolean allNegative = true;
        for (int index : bAdicIndices) {
            if (index >= 0) {
                allNegative = false;
            } else {
                allPositive = false;
            }
        }

        if (allPositive) {
            positive = true;
        } else if (allNegative) {
            positive = false;
        } else {
            throw new IllegalArgumentException("B-adic indices must be all positive or all negative.");
        }

        List<Integer> prunedIndices = new ArrayList<>(bAdicIndices.length);

        if (level == this.exponent) {
            if (positive) {
                for (int i = 0; i < bAdicIndices.length; i++) {
                    if (bAdicIndices[i] < this.positiveBitmap.length() && this.positiveBitmap.get(bAdicIndices[i])) {
                        prunedIndices.add(bAdicIndices[i]);
                    }
                }
            } else {
                for (int i = 0; i < bAdicIndices.length; i++) {
                    int idx = -bAdicIndices[i] - 1;
                    if (idx < this.negativeBitmap.length() && this.negativeBitmap.get(idx)) {
                        prunedIndices.add(bAdicIndices[i]);
                    }
                }
            }
        } else if (level > this.exponent) {
            int levelDiff = level - this.exponent;
            int scale = (int) Math.pow(this.base, levelDiff);

            if (positive) {
                for (int i = 0; i < bAdicIndices.length; i++) {
                    int startIdx = bAdicIndices[i] * scale;
                    boolean anySet = false;
                    for (int offset = 0; offset < scale; offset++) {
                        int idx = startIdx + offset;
                        if (idx < this.positiveBitmap.length() && this.positiveBitmap.get(idx)) {
                            anySet = true;
                            break;
                        }
                    }
                    if (anySet) {
                        prunedIndices.add(bAdicIndices[i]);
                    }
                }
            } else {
                for (int i = 0; i < bAdicIndices.length; i++) {
                    int startIdx = (-bAdicIndices[i]) * scale - 1;
                    boolean anySet = false;
                    for (int offset = 0; offset < scale; offset++) {
                        int idx = startIdx + offset;
                        if (idx < this.negativeBitmap.length() && this.negativeBitmap.get(idx)) {
                            anySet = true;
                            break;
                        }
                    }
                    if (anySet) {
                        prunedIndices.add(bAdicIndices[i]);
                    }
                }
            }
        } else {
            int levelDiff = this.exponent - level;
            int scale = (int) Math.pow(this.base, levelDiff);

            if (positive) {
                for (int i = 0; i < bAdicIndices.length; i++) {
                    int idx = bAdicIndices[i] / scale;
                    if (idx < this.positiveBitmap.length() && this.positiveBitmap.get(idx)) {
                        prunedIndices.add(bAdicIndices[i]);
                    }
                }
            } else {
                for (int i = 0; i < bAdicIndices.length; i++) {
                    int idx = -bAdicIndices[i] / scale - 1;
                    if (idx < this.negativeBitmap.length() && this.negativeBitmap.get(idx)) {
                        prunedIndices.add(bAdicIndices[i]);
                    }
                }
            }
        }

        return prunedIndices.stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }

    public void setAllTrue(){
        this.positiveBitmap.set(0, this.sizePerSide);
        this.negativeBitmap.set(0, this.sizePerSide);
    }

    public double getSize(String unit) {
        double nBytes = Math.ceil((double) this.sizePerSide * 2 / 8.0);
        switch (unit) {
            case "MB":
                return nBytes / (1024 * 1024);
            case "KB":
                return nBytes / 1024;
            case "B":
                return nBytes;
            default:
                throw new IllegalArgumentException("Unit must be 'MB', 'KB', or 'B'.");
        }
    }

    public double getSize() {
        return getSize("MB");
    }


    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static NumericalBitmap fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, NumericalBitmap.class);
    }

    public int getExponent() {
        return exponent;
    }
}
