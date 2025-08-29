package pacha;

import org.junit.jupiter.api.Test;
import pachasketch.pacha.components.MaterializedCombinations;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.*;

class MaterializedCombinationsTest {

    @Test
    void fromJsonCreatesCorrectInstance() {
        Map<String, Object> json = new HashMap<>();
        json.put("col_names", Arrays.asList("A", "B", "C"));
        json.put("relevant_combinations", Arrays.asList(
            Arrays.asList("A", "B"),
            Arrays.asList("B", "C")
        ));

        MaterializedCombinations result = MaterializedCombinations.fromJson(json);

        assertEquals(Arrays.asList("A", "B", "C"), result.toJson().get("col_names"));
        assertEquals(Arrays.asList(
            Arrays.asList("A", "B"),
            Arrays.asList("B", "C")
        ), result.toJson().get("relevant_combinations"));
    }

    @Test
    void findBestMatchReturnsCorrectMatch() {
        List<String> colNames = Arrays.asList("A", "B", "C");
        List<List<String>> relevantCombinations = Arrays.asList(
            Arrays.asList("A", "B"),
            Arrays.asList("B", "C"),
            Arrays.asList("A", "B", "C")
        );
        MaterializedCombinations mc = new MaterializedCombinations(colNames, relevantCombinations);

        boolean[] result = mc.findBestMatch(Arrays.asList(0, 1));

        assertArrayEquals(new boolean[]{true, true, false}, result);

        result = mc.findBestMatch(Arrays.asList(0, 2));
        assertArrayEquals(new boolean[]{true, true, true}, result);
    }

    @Test
    void findBestMatchHandlesNoMatches() {
        List<String> colNames = Arrays.asList("A", "B", "C");
        List<List<String>> relevantCombinations = Arrays.asList(
            Arrays.asList("A", "B"),
            Arrays.asList("B", "C")
        );
        MaterializedCombinations mc = new MaterializedCombinations(colNames, relevantCombinations);

        boolean[] result = mc.findBestMatch(Arrays.asList(0, 2));

        assertArrayEquals(new boolean[0], result);
    }

    @Test
    void latticeExpandReturnsExpandedLattice() {
        List<String> colNames = Arrays.asList("A", "B", "C");
        List<List<String>> relevantCombinations = Arrays.asList(
            Arrays.asList("A", "B"),
            Arrays.asList("B", "C"),
            Arrays.asList("A", "B", "C")
        );
        MaterializedCombinations mc = new MaterializedCombinations(colNames, relevantCombinations);

        int[][] cubes = {
            {1, 1, 1},
            {2, 2, 2}
        };
        Object[][] result = mc.latticeExpand(cubes);

        assertEquals(6, result.length);

        assertArrayEquals(new Object[]{1, 1, "*"}, result[0]);
        assertArrayEquals(new Object[]{"*", 1, 1}, result[1]);
        assertArrayEquals(new Object[]{1, 1, 1}, result[2]);
        assertArrayEquals(new Object[]{2, 2, "*"}, result[3]);
        assertArrayEquals(new Object[]{"*", 2, 2}, result[4]);
        assertArrayEquals(new Object[]{2, 2, 2}, result[5]);
    }

    @Test
    void createSingleCombinationCreatesOneCombination() {
        List<String> attributes = Arrays.asList("A", "B", "C");
        MaterializedCombinations mc = MaterializedCombinations.createSingleCombination(attributes);

        assertEquals(attributes, mc.getAttributeNames());
        assertEquals(Collections.singletonList(attributes), mc.getRelevantCombinations());
    }

    @Test
    void createAllCombinationsGeneratesAllPossibleCombinations() {
        List<String> attributes = Arrays.asList("A", "B", "C");
        MaterializedCombinations mc = MaterializedCombinations.createAllCombinations(attributes);

        List<List<String>> combinations = mc.getRelevantCombinations();
        assertEquals(7, combinations.size());
        assertTrue(combinations.contains(Arrays.asList("A")));
        assertTrue(combinations.contains(Arrays.asList("B")));
        assertTrue(combinations.contains(Arrays.asList("C")));
        assertTrue(combinations.contains(Arrays.asList("A", "B")));
        assertTrue(combinations.contains(Arrays.asList("B", "C")));
        assertTrue(combinations.contains(Arrays.asList("A", "C")));
        assertTrue(combinations.contains(Arrays.asList("A", "B", "C")));
    }


    @Test
    void fromJsonCreatesCorrectInstanceFromFile() throws IOException {
        // Path to the JSON file
        String jsonFilePath = "src/main/resources/relevantCombinations/tpch_lineitem.json";

        // Create MaterializedCombinations from the JSON file
        MaterializedCombinations result = MaterializedCombinations.fromJson(jsonFilePath);

        // Validate the instance
        assertEquals(Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity"), result.getAttributeNames());
        assertTrue(result.getRelevantCombinations().contains(Arrays.asList("n_shipdate")));
        assertTrue(result.getRelevantCombinations().contains(Arrays.asList("n_commitdate", "n_receiptdate", "n_extendedprice")));
        assertTrue(result.getRelevantCombinations().contains(Arrays.asList("n_shipdate", "n_commitdate", "n_receiptdate", "n_extendedprice", "n_quantity")));
    }

}