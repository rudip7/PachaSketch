package pacha;

import org.junit.jupiter.api.Test;
import pachasketch.pacha.PachaSketch;
import pachasketch.utils.PachaSketchFactory;
import pachasketch.pacha.components.ADTree;
import pachasketch.pacha.components.MaterializedCombinations;
import pachasketch.pacha.utils.PachaQueryResult;
import pachasketch.pacha.utils.QueryStats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

public class PachaSketchTest {
    private PachaSketch buildSketchToTest(){
        int levels = 3;
        int[] catColMap = {0, 1, 2};
        int[] numColMap = {3, 4};
        int[] bases = {2, 10};

        ADTree adTree = new ADTree();
        adTree.addDimension(Set.of("a1", "a2", "a3"), "A");
        adTree.addDimension(Set.of("b1", "b2"), "B");
        adTree.addDimension(Set.of("c1", "c2", "c3"), "C");

        // Assuming MaterializedCombinations is already defined and has a suitable constructor
        MaterializedCombinations materialized = MaterializedCombinations.createAllCombinations(List.of("N1", "N2"));

        int catIndexK = 3;
        int catIndexM = 100;
        int numIndexK = 3;
        int numIndexM = 100;
        int regionIndexK = 3;
        int regionIndexM = 100;

        int width = 100;
        int depth = 3;

        return PachaSketchFactory.buildWithSizeParameters(catColMap, numColMap, levels, bases, adTree,
                materialized, catIndexK, catIndexM, numIndexK, numIndexM, regionIndexK, regionIndexM,
                width, depth);
    }

    private void disableNumericalBitmaps(PachaSketch sketch) {
        for (int i = 0; i < sketch.getNumColMap().length; i++) {
            sketch.numericalBitmaps[i].setAllTrue();
        }
    }

    private void addFakeData(PachaSketch sketch) {
        sketch.update(new String[]{"a1", "b1", "c1", "0", "20"});
        sketch.update(new String[]{"a1", "b1", "c1", "0", "20"});
        sketch.update(new String[]{"a1", "b1", "c1", "0", "20"});
        sketch.update(new String[]{"a1", "b1", "c1", "0", "21"});
        sketch.update(new String[]{"a1", "b1", "c1", "0", "22"});
        sketch.update(new String[]{"a1", "b1", "c1", "1", "22"});
        sketch.update(new String[]{"a1", "b1", "c1", "1", "20"});
        sketch.update(new String[]{"a1", "b1", "c1", "1", "21"});
        sketch.update(new String[]{"a1", "b1", "c1", "1", "19"});
        sketch.update(new String[]{"a1", "b1", "c1", "1", "18"});
    }


    @Test
    void numericalMappingsForSimpleValuesShouldReturnCorrectMapping() {
        PachaSketch sketch = buildSketchToTest();
        int levels = sketch.getLevels();
        int nMaterialized = sketch.getMaterializedCombinations().getRelevantCombinations().size();

        int[] values = {5, 20};

        Object[][] result = sketch.getNumericalMappings(values);

        assertThat(result.length).isEqualTo(levels * nMaterialized + 1);

        // Convert 2D array to List<List<Object>>
        List<List<Object>> resultList = Arrays.stream(result)
                .map(row -> Arrays.asList(row))
                .toList();

        // Check specific level mappings
        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(0, 5, 20);
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(1, 2, 2);
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(2, 1, 0);
        });

        // Check materialized lattice expansion
        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(0, "*", 20);
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(0, 5, "*");
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(1, "*", 2);
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(1, 2, "*");
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(2, "*", 0);
        });

        assertThat(resultList).anySatisfy(row -> {
            assertThat(row).containsSequence(2, 1, "*");
        });

        // Check all-wildcards row
        assertThat(resultList).last().satisfies(row -> {
            assertThat(row).containsSequence(levels - 1, "*", "*");
        });
    }

    @Test
    void updateShouldUpdateAllComponents() {
        PachaSketch sketch = buildSketchToTest();
        String[] element = {"a1", "b1", "c1", "5", "20"};

        sketch.update(element);

        assertEquals(1, sketch.getProcessedElements());

        assertTrue(sketch.catIndex.query("*, *, *"));
        assertTrue(sketch.catIndex.query("a1, *, *"));
        assertTrue(sketch.catIndex.query("a1, b1, *"));
        assertTrue(sketch.catIndex.query("a1, b1, c1"));

        assertEquals(10, sketch.numIndex.getProcessedElements());
        assertTrue(sketch.numIndex.query("0, 5, 20"));
        assertTrue(sketch.numIndex.query("1, 2, 2"));
        assertTrue(sketch.numIndex.query("2, 1, 0"));
        assertTrue(sketch.numIndex.query("0, 5, *"));
        assertTrue(sketch.numIndex.query("1, 2, *"));
        assertTrue(sketch.numIndex.query("2, 1, *"));
        assertTrue(sketch.numIndex.query("0, *, 20"));
        assertTrue(sketch.numIndex.query("1, *, 2"));
        assertTrue(sketch.numIndex.query("2, *, 0"));
        assertTrue(sketch.numIndex.query("2, *, *"));

        assertEquals(40, sketch.regionIndex.getProcessedElements());
        assertTrue(sketch.regionIndex.query("a1, b1, c1, 0, 5, 20"));
        assertTrue(sketch.regionIndex.query("a1, b1, c1, 0, *, 20"));
        assertTrue(sketch.regionIndex.query("a1, b1, c1, 1, 2, 2"));

        assertEquals(12, sketch.baseSketches[0].getProcessedElements());
        assertEquals(12, sketch.baseSketches[1].getProcessedElements());
        assertEquals(16, sketch.baseSketches[2].getProcessedElements());
        assertEquals(1, sketch.baseSketches[0].query("a1, b1, c1, 0, 5, 20"));
        assertEquals(1, sketch.baseSketches[1].query("a1, b1, c1, 1, 2, 2"));
        assertEquals(1, sketch.baseSketches[2].query("a1, b1, c1, 2, 1, 0"));
    }

    @Test
    void emptyListProducesEmptyCombinations() {
        PachaSketch sketch = buildSketchToTest();
        List<List<int[]>> emptyList = new ArrayList<>();

        List<int[][]> result = sketch.generateCombinations(emptyList);

        assertThat(result).isEmpty();
    }

    @Test
    void singleListProducesSingleElementCombinations() {
        PachaSketch sketch = buildSketchToTest();
        List<List<int[]>> input = List.of(
            List.of(new int[]{1, 2}, new int[]{3, 4})
        );

        List<int[][]> result = sketch.generateCombinations(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)[0]).containsExactly(1, 2);
        assertThat(result.get(1)[0]).containsExactly(3, 4);
    }

    @Test
    void twoListsProduceCartesianProduct() {
        PachaSketch sketch = buildSketchToTest();
        List<List<int[]>> input = List.of(
            List.of(new int[]{1, 2}),
            List.of(new int[]{3, 4}, new int[]{5, 6})
        );

        List<int[][]> result = sketch.generateCombinations(input);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new int[][]{new int[]{1, 2}, new int[]{3, 4}});
        assertThat(result.get(1)).isEqualTo(new int[][]{new int[]{1, 2}, new int[]{5, 6}});
    }

    @Test
    void threeListsProduceCartesianProduct() {
        PachaSketch sketch = buildSketchToTest();
        List<List<int[]>> input = List.of(
                List.of(new int[]{0, 3}, new int[]{1, 2}),
                List.of(new int[]{3, 4}, new int[]{5, 6}),
                List.of(new int[]{7, 8})
        );

        List<int[][]> result = sketch.generateCombinations(input);

        assertThat(result).hasSize(4);
        assertThat(result.get(0)).isEqualTo(new int[][]{new int[]{0, 3}, new int[]{3, 4}, new int[]{7, 8}});
        assertThat(result.get(1)).isEqualTo(new int[][]{new int[]{0, 3}, new int[]{5, 6}, new int[]{7, 8}});
        assertThat(result.get(2)).isEqualTo(new int[][]{new int[]{1, 2}, new int[]{3, 4}, new int[]{7, 8}});
        assertThat(result.get(3)).isEqualTo(new int[][]{new int[]{1, 2}, new int[]{5, 6}, new int[]{7, 8}});
    }

    @Test
    void listWithEmptySublistProducesNoCombinations() {
        PachaSketch sketch = buildSketchToTest();
        List<List<int[]>> input = List.of(
            List.of(new int[]{1, 2}),
            List.of(),
            List.of(new int[]{3, 4})
        );

        List<int[][]> result = sketch.generateCombinations(input);

        assertThat(result).isEmpty();
    }

    @Test
    void minimalSpatialBAdicCoverForSingleDimensionReturnsCorrectRanges() {
        PachaSketch sketch = buildSketchToTest();
        disableNumericalBitmaps(sketch);

        int[] numDimensions = {0};
        int[][] numPredicates = {{4, 5}};

        int[][] result = sketch.minimalSpatialBAdicCover(numDimensions, numPredicates, -1);

        assertEquals(1, result.length);
        assertThat(result[0]).containsExactly(1, 2);
    }

    @Test
    void minimalSpatialBAdicCoverForTwoDimensionsReturnsCorrectRanges() {
        PachaSketch sketch = buildSketchToTest();
        disableNumericalBitmaps(sketch);

        int[] numDimensions = {0, 1};
        int[][] numPredicates = {{4, 5}, {20, 29}};

        int[][] result = sketch.minimalSpatialBAdicCover(numDimensions, numPredicates, -1);

        assertEquals(1, result.length);
        assertThat(result[0]).containsExactly(1, 2, 2);
    }

    @Test
    void minimalSpatialBAdicCoverDowngradedRanges() {
        PachaSketch sketch = buildSketchToTest();
        disableNumericalBitmaps(sketch);

        int[] numDimensions = {0, 1};
        int[][] numPredicates = {{4, 7}, {20, 29}};

        int[][] result = sketch.minimalSpatialBAdicCover(numDimensions, numPredicates, -1);

        assertEquals(2, result.length);
        assertThat(result[0]).containsExactly(1, 2, 2);
        assertThat(result[1]).containsExactly(1, 3, 2);
    }


    @Test
    void minimalSpatialBAdicCoverExceedingMaxNCubesReducesLevel() {
        PachaSketch sketch = buildSketchToTest();
        disableNumericalBitmaps(sketch);
        int[] numDimensions = {0, 1};
        int[][] numPredicates = {{0, 3}, {0, 29}};

        int[][] result = sketch.minimalSpatialBAdicCover(numDimensions, numPredicates, -1);
        assertEquals(6, result.length);
        assertThat(result[0]).containsExactly(1, 0, 2);
        assertThat(result[1]).containsExactly(1, 1, 2);
        assertThat(result[2]).containsExactly(1, 0, 1);
        assertThat(result[3]).containsExactly(1, 1, 1);
        assertThat(result[4]).containsExactly(1, 0, 0);
        assertThat(result[5]).containsExactly(1, 1, 0);

        sketch.setMaxNCubes(2);
        result = sketch.minimalSpatialBAdicCover(numDimensions, numPredicates, -1);
        assertEquals(1, result.length);
        assertThat(result[0]).containsExactly(2, 0, 0);
    }

    @Test
    void simpleQueryReturnsMatchingSubQueries() {
        PachaSketch sketch = buildSketchToTest();
        addFakeData(sketch);
        List<Object> query = Arrays.asList(
            new HashSet<>(List.of("a1")),
            new HashSet<>(List.of("b1")),
            new HashSet<>(List.of("c1")),
            new int[]{0, 0},
            new int[]{20, 20}
        );

        PachaQueryResult result = sketch.query(query, true, true);

        QueryStats stats = result.stats();

        assertEquals(1, stats.relevantNodes());
        assertEquals(1, stats.catRegions());
        assertEquals(1, stats.bAdicCubes());
        assertEquals(1, stats.numRegions());
        assertEquals(1, stats.candidateRegions());
        assertEquals(1, stats.queryRegions());

        assertEquals(3, result.estimate());
    }

    @Test
    void wildcardQueryReturnsAllMatchingRegions() {
        PachaSketch sketch = buildSketchToTest();
        addFakeData(sketch);
        List<Object> query = Arrays.asList(
            "*",
            "*",
            "*",
            "*",
            "*"
        );

        PachaQueryResult result = sketch.query(query, true, true);

        assertEquals(10, result.estimate());
    }

    @Test
    void queryReturnsMatchingSubQueries() {
        PachaSketch sketch = buildSketchToTest();
        addFakeData(sketch);
        List<Object> query = Arrays.asList(
                "*",
                new HashSet<>(List.of("b1")),
                new HashSet<>(List.of("c1")),
                new int[]{0, 7},
                new int[]{20, 20}
        );

        PachaQueryResult result = sketch.query(query, true, true);

        QueryStats stats = result.stats();

        assertEquals(3, stats.relevantNodes());
        assertEquals(1, stats.catRegions());
        assertEquals(2, stats.bAdicCubes());
        assertEquals(2, stats.numRegions());
        assertEquals(2, stats.candidateRegions());
        assertEquals(2, stats.queryRegions());

        assertEquals(4, result.estimate());
    }

    @Test
    void toJsonAndFromJsonAreConsistent() {
        PachaSketch sketch = buildSketchToTest();
        String json = sketch.toJson();
        PachaSketch restoredSketch = PachaSketch.fromJson(json);

        assertEquals(sketch, restoredSketch);
    }

}
