package pachasketch.pacha.components;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MaterializedCombinations {
    private final List<String> attributeNames;
    private final List<List<String>> relevantCombinations;
    private final int[][] bits;
    private final int[][] invertedBits;

    public MaterializedCombinations(List<String> attributeNames, List<List<String>> relevantCombinations) {
        this.attributeNames = attributeNames;
        this.relevantCombinations = relevantCombinations;
        this.bits = combinationsToBits(attributeNames, relevantCombinations);
        this.invertedBits = invertBits(bits);
    }

    public static MaterializedCombinations createSingleCombination(List<String> attributeNames){
        List<List<String>> singleCombination = new ArrayList<>();
        singleCombination.add(attributeNames);
        return new MaterializedCombinations(attributeNames, singleCombination);
    }

    public static MaterializedCombinations createAllCombinations(List<String> attributeNames) {
        List<List<String>> allCombinations = new ArrayList<>();
        int n = attributeNames.size();
        for (int i = 0; i < (1 << n); i++) {
            List<String> combination = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) {
                    combination.add(attributeNames.get(j));
                }
            }
            if (!combination.isEmpty()) {
                allCombinations.add(combination);
            }
        }
        return new MaterializedCombinations(attributeNames, allCombinations);
    }

    /**
     * Finds the best matching combination based on the given list of predicate indices.
     * The method identifies rows in the `bits` array that match the specified predicates
     * and selects the row with the minimum number of `1`s. If multiple rows have the same
     * minimum number of `1`s, the first one is picked.
     *
     * @param numPredicates A list of indices representing the predicates to match.
     *                      Each index corresponds to an attribute in `attributeNames`.
     * @return A boolean array representing the best matching combination. If no match
     *         is found, an empty boolean array is returned.
     */
    public boolean[] findBestMatch(List<Integer> numPredicates) {
        if (numPredicates.isEmpty()) {
            return new boolean[attributeNames.size()];
        }
        int[] mask = new int[attributeNames.size()];
        for (int index : numPredicates) {
            mask[index] = 1;
        }

        List<Integer> matchingRows = new ArrayList<>();
        int minOnes = Integer.MAX_VALUE;

        for (int i = 0; i < bits.length; i++) {
            boolean matches = true;
            int onesCount = 0;

            for (int j = 0; j < mask.length; j++) {
                if (mask[j] == 1 && bits[i][j] != 1) {
                    matches = false;
                    break;
                }
                if (bits[i][j] == 1) {
                    onesCount++;
                }
            }

            if (matches) {
                if (onesCount < minOnes) {
                    matchingRows.clear();
                    minOnes = onesCount;
                }
                if (onesCount == minOnes) {
                    matchingRows.add(i);
                }
            }
        }

        if (matchingRows.isEmpty()) {
            return new boolean[0];
        }

        int[] bestMatch = bits[matchingRows.get(0)];
        boolean[] result = new boolean[bestMatch.length];
        for (int i = 0; i < bestMatch.length; i++) {
            result[i] = bestMatch[i] == 1;
        }
        return result;
    }

    public Object[][] latticeExpand(int[][] cubes) {
        String fill = "*";
        int n = cubes.length;
        int d = cubes[0].length;
        int c = invertedBits.length;

        Object[][] expanded = new Object[n * c][d];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < c; j++) {
                for (int k = 0; k < d; k++) {
                    expanded[i * c + j][k] = invertedBits[j][k] == 1 ? fill : cubes[i][k];
                }
            }
        }
        return expanded;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("attribute_names", attributeNames);
        json.put("relevant_combinations", relevantCombinations);
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MaterializedCombinations that = (MaterializedCombinations) obj;
        return Objects.equals(attributeNames, that.attributeNames) &&
                Objects.equals(relevantCombinations, that.relevantCombinations) &&
                Arrays.deepEquals(bits, that.bits) &&
                Arrays.deepEquals(invertedBits, that.invertedBits);
    }

    public static MaterializedCombinations fromJson(String jsonContent) {
        // Parse the JSON content into a Map using Gson
        Gson gson = new Gson();
        Map<String, Object> json = gson.fromJson(jsonContent, Map.class);

        List<String> colNames = (List<String>) json.get("attribute_names");
        List<List<String>> relevantCombinations = (List<List<String>>) json.get("relevant_combinations");
        return new MaterializedCombinations(colNames, relevantCombinations);
    }

    private static int[][] combinationsToBits(List<String> attributeNames, List<List<String>> relevantCombinations) {
        int[][] bits = new int[relevantCombinations.size()][attributeNames.size()];
        Map<String, Integer> colIndexMap = new HashMap<>();
        for (int i = 0; i < attributeNames.size(); i++) {
            colIndexMap.put(attributeNames.get(i), i);
        }

        for (int i = 0; i < relevantCombinations.size(); i++) {
            for (String col : relevantCombinations.get(i)) {
                bits[i][colIndexMap.get(col)] = 1;
            }
        }
        return bits;
    }

    private static int[][] invertBits(int[][] bits) {
        int[][] inverted = new int[bits.length][bits[0].length];
        for (int i = 0; i < bits.length; i++) {
            for (int j = 0; j < bits[i].length; j++) {
                inverted[i][j] = 1 - bits[i][j];
            }
        }
        return inverted;
    }

    public List<String> getAttributeNames() {
        return Collections.unmodifiableList(attributeNames);
    }

    public List<List<String>> getRelevantCombinations() {
        return Collections.unmodifiableList(relevantCombinations);
    }

    public int[][] getBits() {
        return Arrays.stream(bits)
                .map(int[]::clone)
                .toArray(int[][]::new);
    }

    public int[][] getInvertedBits() {
        return Arrays.stream(invertedBits)
                .map(int[]::clone)
                .toArray(int[][]::new);
    }

    public int getNumCombinations() {
        return relevantCombinations.size();
    }

    public static MaterializedCombinations fromFile(String jsonFilePath) throws IOException {
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
        return MaterializedCombinations.fromJson(jsonContent);
    }


}