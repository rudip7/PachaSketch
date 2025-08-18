package pacha;

import org.junit.jupiter.api.Test;
import pachasketch.pacha.components.ADTree;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class ADTreeTest {

    @Test
    void addDimensionIncreasesNumDimensions() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        assertEquals(3, adTree.computeDistinctValues());
        assertEquals(1, adTree.getNumDimensions());
    }

    @Test
    void collapseLastDimensionFailsForSingleDimension() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.collapseLastDimension();
        assertFalse(adTree.isCollapsed());
    }

    @Test
    void getMappingThrowsExceptionForInvalidElementLength() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        assertThrows(IllegalArgumentException.class, () -> adTree.getMapping(Arrays.asList("A", "B")));
    }

    @Test
    void getRelevantNodesHandlesAllWildcards() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        List<Set<String>> predicates = Arrays.asList(Collections.singleton("*"), Collections.singleton("*"));
        List<List<String>> relevantNodes = adTree.getRelevantNodes(predicates, false);
        assertEquals(1, relevantNodes.size());
        assertEquals(Arrays.asList("*", "*"), relevantNodes.get(0));
    }

    @Test
    void toJsonReturnsCorrectStructure() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        Map<String, Object> json = adTree.toJson();
        assertEquals(2, json.get("num_dimensions"));
        assertEquals(Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("X", "Y")), json.get("possible_values"));
        assertEquals(Arrays.asList("Attribute1", "Attribute2"), json.get("names"));
        assertFalse((Boolean) json.get("collapsed"));
    }

    @Test
    void getMappingReturnsCorrectMappingsForValidElement() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        List<List<String>> mappings = adTree.getMapping(Arrays.asList("A", "X"));
        assertEquals(3, mappings.size());
        assertEquals(Arrays.asList("*", "*"), mappings.get(0));
        assertEquals(Arrays.asList("A", "*"), mappings.get(1));
        assertEquals(Arrays.asList("A", "X"), mappings.get(2));
    }

    @Test
    void getMappingHandlesCollapsedDimension() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        adTree.collapseLastDimension();
        List<List<String>> mappings = adTree.getMapping(Arrays.asList("A", "X"));
        assertEquals(2, mappings.size());
        assertEquals(Arrays.asList("*", "*"), mappings.get(0));
        assertEquals(Arrays.asList("A", "X"), mappings.get(1));
    }

    @Test
    void getRelevantNodesReturnsAllNodesForWildcardPredicates() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        List<Set<String>> predicates = Arrays.asList(Collections.singleton("*"), Collections.singleton("*"));
        List<List<String>> relevantNodes = adTree.getRelevantNodes(predicates, false);
        assertEquals(1, relevantNodes.size());
        assertTrue(relevantNodes.contains(Arrays.asList("*", "*")));
    }

    @Test
    void getRelevantNodesReturnsFilteredNodesForPredicatesWithWildcards() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        List<Set<String>> predicates = Arrays.asList(Collections.singleton("A"), Collections.singleton("*"));
        List<List<String>> relevantNodes = adTree.getRelevantNodes(predicates, true);
        assertEquals(1, relevantNodes.size());
        assertTrue(relevantNodes.contains(Arrays.asList("A", "*")));

        predicates = Arrays.asList(Collections.singleton("*"), Collections.singleton("X"));
        relevantNodes = adTree.getRelevantNodes(predicates, true);
        assertEquals(3, relevantNodes.size());
        assertTrue(relevantNodes.contains(Arrays.asList("A", "X")));
        assertTrue(relevantNodes.contains(Arrays.asList("B", "X")));
        assertTrue(relevantNodes.contains(Arrays.asList("C", "X")));

        adTree.addDimension(new HashSet<>(Arrays.asList("D", "E")), "Attribute3");
        predicates = Arrays.asList(Collections.singleton("*"), Collections.singleton("X"), Collections.singleton("*"));
        relevantNodes = adTree.getRelevantNodes(predicates, true);
        assertEquals(3, relevantNodes.size());
        assertTrue(relevantNodes.contains(Arrays.asList("A", "X", "*")));
        assertTrue(relevantNodes.contains(Arrays.asList("B", "X", "*")));
        assertTrue(relevantNodes.contains(Arrays.asList("C", "X", "*")));

        predicates = Arrays.asList(Collections.singleton("*"), Collections.singleton("*"), Collections.singleton("E"));
        relevantNodes = adTree.getRelevantNodes(predicates, true);
        assertEquals(6, relevantNodes.size());
        assertTrue(relevantNodes.contains(Arrays.asList("A", "X", "E")));
        assertTrue(relevantNodes.contains(Arrays.asList("A", "Y", "E")));
        assertTrue(relevantNodes.contains(Arrays.asList("B", "X", "E")));
        assertTrue(relevantNodes.contains(Arrays.asList("B", "Y", "E")));
        assertTrue(relevantNodes.contains(Arrays.asList("C", "X", "E")));
        assertTrue(relevantNodes.contains(Arrays.asList("C", "Y", "E")));


    }

    @Test
    void getRelevantNodesReturnsFilteredNodesForSpecificPredicates() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        List<Set<String>> predicates = Arrays.asList(Collections.singleton("A"), Collections.singleton("X"));
        List<List<String>> relevantNodes = adTree.getRelevantNodes(predicates, false);
        assertEquals(2, relevantNodes.size());
        assertTrue(relevantNodes.contains(Arrays.asList("*", "*")));
        assertTrue(relevantNodes.contains(Arrays.asList("A", "X")));
    }
}