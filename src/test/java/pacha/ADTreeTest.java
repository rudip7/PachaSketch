package pacha;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import pachasketch.pacha.components.ADTree;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        String jsonString = adTree.toJson();
        Gson gson = new Gson();
        Map<String, Object> json = gson.fromJson(jsonString, Map.class);
        assertEquals(2.0, json.get("num_dimensions"));
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

    @Test
    void fromJsonCreatesCorrectADTree() throws IOException {
        // Load the JSON file
        String jsonFilePath = "src/main/resources/ad_trees/online_retail.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

        // Create the ADTree from the JSON
        ADTree adTree = ADTree.fromJson(jsonContent);

        // Validate the ADTree structure
        assertEquals(3, adTree.getNumDimensions());
        assertEquals(Set.of("category", "region", "gender"), Set.copyOf(adTree.attributeNames));
        assertFalse(adTree.isCollapsed());
        String jsonString = adTree.toJson();
        Gson gson = new Gson();
        Map<String, Object> json = gson.fromJson(jsonString, Map.class);

        List<List<String>> possibleValues = (List<List<String>>) json.get("possible_values");
        assertEquals(3, possibleValues.size());
        assertTrue(possibleValues.get(0).contains("842"));
        assertTrue(possibleValues.get(1).contains("Italy"));
        assertTrue(possibleValues.get(2).contains("d"));
    }

    @Test
    void fromJsonCreatesCorrectADTreeWithTpchLineitem() throws IOException {
        // Load the JSON file
        String jsonFilePath = "src/main/resources/ad_trees/tpch_lineitem.json";
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

        // Create the ADTree from the JSON
        ADTree adTree = ADTree.fromJson(jsonContent);

        // Validate the ADTree structure
        assertEquals(5, adTree.getNumDimensions());
        assertEquals(Set.of("c_shipmode", "c_returnflag", "c_linestatus", "c_discount", "c_tax"), Set.copyOf(adTree.attributeNames));
        assertTrue(adTree.isCollapsed());

        String jsonString = adTree.toJson();
        Gson gson = new Gson();
        Map<String, Object> json = gson.fromJson(jsonString, Map.class);

        List<List<String>> possibleValues = (List<List<String>>) json.get("possible_values");
        assertEquals(5, possibleValues.size());
        assertTrue(possibleValues.get(0).contains("FOB"));
        assertTrue(possibleValues.get(1).contains("A"));
        assertTrue(possibleValues.get(2).contains("O"));
        assertTrue(possibleValues.get(3).contains("0.05"));
        assertTrue(possibleValues.get(4).contains("0.06"));
    }

    @Test
    void toJsonAndFromJsonAreConsistent() {
        ADTree adTree = new ADTree();
        adTree.addDimension(new HashSet<>(Arrays.asList("A", "B", "C")), "Attribute1");
        adTree.addDimension(new HashSet<>(Arrays.asList("X", "Y")), "Attribute2");
        adTree.collapseLastDimension();

        String jsonString = adTree.toJson();
        ADTree restoredAdTree = ADTree.fromJson(jsonString);

        assertEquals(adTree, restoredAdTree);
    }

}