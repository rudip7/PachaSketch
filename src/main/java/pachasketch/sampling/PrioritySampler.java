package pachasketch.sampling;

import java.util.*;
import java.util.stream.Collectors;

public class PrioritySampler {
    /** One sampled item */
    private static class Entry {
        final String key;          // canonical key for the element
        final String[] element;    // full multidimensional record
        final double alpha;        // U(0,1]
        double weight;             // accumulated weight
        final double priority;     // weight / alpha (fixed)

        Entry(String key, String[] element, double alpha, double weight) {
            this.key = key;
            this.element = element;
            this.alpha = alpha;
            this.weight = weight;
            this.priority = weight / alpha;
        }
    }

    private final int k;

    /** Min-heap by priority */
    private final PriorityQueue<Entry> heap;

    /** Fast lookup for already-sampled items */
    private final Map<String, Entry> index;

    private final Random random;
    public int numAttributes;
    public int[] catColMap;
    public int[] numColMap;


    public PrioritySampler(int k, int[] catColMap, int[] numColMap) {
        this.k = k;
        this.heap = new PriorityQueue<>(Comparator.comparingDouble(e -> e.priority));
        this.index = new HashMap<>();
        this.random = new Random(7);

        this.numAttributes = catColMap.length + numColMap.length;
        this.catColMap = catColMap;
        this.numColMap = numColMap;
    }

    public static PrioritySampler buildWithMemoryBudget(double memoryBudgetMB, int[] catColMap, int[] numColMap) {
        // Rough estimate of entry size
        int entrySize = 8 + 8 + 8;
        entrySize += catColMap.length * 20; // average string size
        entrySize += numColMap.length * 4;  // integer size

        int mapEntrySize = 20 + 8 + 8; // key ref + entry ref

        int totalEntries = (int) ((memoryBudgetMB * 1024 * 1024) / (entrySize + mapEntrySize));
        return new PrioritySampler(totalEntries, catColMap, numColMap);
    }

    /* ------------------ Public update API ------------------ */

    /** Unweighted update (increment = 1) */
    public void update(String[] element) {
        update(element, 1);
    }

    /** Weighted update */
    public void update(String[] element, int increment) {
        if (increment <= 0) return;

        String key = canonicalKey(element);

        // Case 1: already sampled
        Entry existing = index.get(key);
        if (existing != null) {
            existing.weight += increment;
            return;
        }

        // Case 2: new element
        double alpha = random.nextDouble();
        double weight = increment;
        double priority = weight / alpha;

        // If heap not full, insert directly
        if (heap.size() < k) {
            Entry e = new Entry(key, element, alpha, weight);
            heap.add(e);
            index.put(key, e);
            return;
        }

        // Heap full: compare against current threshold
        Entry min = heap.peek();
        if (priority > min.priority) {
            heap.poll();
            index.remove(min.key);

            Entry e = new Entry(key, element, alpha, weight);
            heap.add(e);
            index.put(key, e);
        }
        // else: drop item
    }

    /**
     * Returns the current threshold τ.
     * If fewer than k items seen, τ = 0.
     */
    public double threshold() {
        if (heap.size() < k) return 0.0;
        return heap.peek().priority;
    }

    /**
     * Returns an unbiased estimate of the total weight
     * of sampled items satisfying the predicate.
     */
    public double query(List<Object> query) {
        if (query.size() != numAttributes) {
            throw new IllegalArgumentException("Query must have the same number of dimensions as the sketch. Expected: " + numAttributes + ", got: " + query.size());
        }

        // Process categorical predicates
        ArrayList<Set<String>> catPredicates = new ArrayList<>(catColMap.length);
        for(int idx : catColMap) {
            Object catPredicate = query.get(idx);
            if (catPredicate instanceof List<?>) {
                catPredicates.add(new HashSet<>(((List<?>) catPredicate).stream()
                        .map(Object::toString)
                        .collect(Collectors.toSet())));
            } else if (catPredicate instanceof String && catPredicate.equals("*")) {
                catPredicates.add(new HashSet<>(Collections.singleton("*")));
            } else {
                throw new IllegalArgumentException("Query predicate at index " + idx + " expected to be a list or '*'.");
            }
        }

        // Process numerical predicates
        ArrayList<int[]> numPredicates = new ArrayList<>(numColMap.length);
        ArrayList<Integer> numDimensions = new ArrayList<>();
        int numIdx = 0;
        for(int idx : numColMap) {
            Object numPredicate = query.get(idx);
            if ((numPredicate instanceof List<?> bounds) && bounds.size() == 2) {
                int lower = ((Number) bounds.get(0)).intValue();
                int upper = ((Number) bounds.get(1)).intValue();
                if (lower > upper) {
                    throw new IllegalArgumentException("Lower bound cannot be greater than upper bound.");
                }
                numPredicates.add(new int[]{lower, upper});
                numDimensions.add(numIdx);
            } else if (numPredicate instanceof String && numPredicate.equals("*")) {
                numPredicates.add(new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE});
            } else {
                throw new IllegalArgumentException("Query predicate at index " + idx +
                        " expected to be a numerical list of size 2 with [lower_bound, upper_bound] or '*'.");
            }
            numIdx++;
        }

        double tau = threshold();
        double sum = 0.0;

        for (Entry e : heap) {
            if (evaluatePredicates(e.element, catPredicates, numPredicates)) {
                sum += Math.max(e.weight, tau);
            }
        }
        return sum;
    }

    public boolean evaluatePredicates(String[] element, ArrayList<Set<String>> catPredicates, ArrayList<int[]> numPredicates) {
        // Check categorical predicates
        for (int i = 0; i < catColMap.length; i++) {
            int idx = catColMap[i];
            Set<String> predicateSet = catPredicates.get(i);
            String value = element[idx];
            if (!predicateSet.contains("*") && !predicateSet.contains(value)) {
                return false;
            }
        }

        // Check numerical predicates
        for (int i = 0; i < numColMap.length; i++) {
            int idx = numColMap[i];
            int[] bounds = numPredicates.get(i);
            int value;
            try {
                value = Integer.parseInt(element[idx]);
            } catch (NumberFormatException ex) {
                return false; // Non-numeric value in numeric column
            }
            if (value < bounds[0] || value > bounds[1]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the raw sample (for custom analysis).
     */
    public Collection<String[]> sample() {
        List<String[]> result = new ArrayList<>();
        for (Entry e : heap) {
            result.add(e.element);
        }
        return result;
    }

    /* ------------------ Helpers ------------------ */

    private static String canonicalKey(String[] element) {
        // Simple, deterministic encoding
        return String.join("\u001F", element);
    }

    public double getSizeInMB(){
        // Rough estimate:
        int entrySize = 8 + 8 + 8;
        entrySize += catColMap.length * 20; // average string size
        entrySize += numColMap.length * 4;  // integer size

        int totalSize = heap.size() * entrySize;

        int mapEntrySize = 20 + 8 + 8; // key ref + entry ref
        totalSize += index.size() * mapEntrySize;

        return totalSize / (1024.0 * 1024.0);
    }
}