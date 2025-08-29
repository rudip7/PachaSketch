package pachasketch.pacha.components;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ADTree {
    private int numDimensions;
    private List<Set<String>> possibleValues;
    public List<String> attributeNames;
    private boolean collapsed;

    public ADTree() {
        this.numDimensions = 0;
        this.possibleValues = new ArrayList<>();
        this.attributeNames = new ArrayList<>();
        this.collapsed = false;
    }

    public void addDimension(Set<String> possibleValues, String attributeName) {
        if (possibleValues == null) {
            throw new IllegalArgumentException("Possible values must not be null.");
        }
        this.possibleValues.add(possibleValues);
        this.attributeNames.add(attributeName != null ? attributeName : "Dimension " + (this.numDimensions + 1));
        this.numDimensions++;
    }

    public int computeDistinctValues() {
        return this.possibleValues.stream().mapToInt(Set::size).reduce(1, (a, b) -> a * b);
    }

    public void collapseLastDimension() {
        if (this.numDimensions < 2) {
            System.out.println("Cannot collapse the last dimension.");
            return;
        }
        this.collapsed = true;
    }

    public List<List<String>> getMapping(List<String> element) {
        if (element.size() != this.numDimensions) {
            throw new IllegalArgumentException("Element length does not match the number of dimensions.");
        }
        List<List<String>> mappings = new ArrayList<>();
        mappings.add(Collections.nCopies(this.numDimensions, "*"));
        List<String> template = new ArrayList<>();
        for (int i = 0; i < element.size(); i++) {
            String value = element.get(i);
            if (!this.possibleValues.get(i).contains(value)) {
                throw new IllegalArgumentException("Value " + value + " at index " + i + " is not in the possible values.");
            }
            template.add(value);
            List<String> mapping = new ArrayList<>(template);
            for (int j = i + 1; j < this.numDimensions; j++) {
                mapping.add("*");
            }
            mappings.add(mapping);
        }
        if (this.collapsed) {
            mappings.remove(mappings.size() - 2);
        }
        return mappings;
    }

    public int getLevel(List<String> mapping) {
        return this.numDimensions - mapping.indexOf("*");
    }

    public List<List<String>> getRelevantNodes(List<Set<String>> predicates, boolean forQuery) {
        if (predicates.size() != this.numDimensions) {
            throw new IllegalArgumentException("Predicates length does not match the number of dimensions.");
        }

        if (predicates.stream().allMatch(p -> p.equals(Collections.singleton("*")))) {
            return Collections.singletonList(Collections.nCopies(this.numDimensions, "*"));
        }

        int lastPredicate = this.numDimensions - 1;
        for (int i = predicates.size() - 1; i >= 0; i--) {
            if (!predicates.get(i).equals(Collections.singleton("*"))) {
                break;
            }
            lastPredicate--;
        }

        for (int i = 0; i < this.numDimensions; i++) {
            if (predicates.get(i).equals(Collections.singleton("*"))) {
                if (i < lastPredicate) {
                    predicates.set(i, this.possibleValues.get(i));
                }
            } else if (!this.possibleValues.get(i).containsAll(predicates.get(i))) {
                throw new IllegalArgumentException("Predicate " + predicates.get(i) + " at index " + i + " is not in the possible values.");
            }
        }

        if (this.collapsed && lastPredicate == this.numDimensions - 2) {
            predicates.set(this.numDimensions - 1, this.possibleValues.get(this.numDimensions - 1));
        }

        List<List<String>> cartesianProduct = cartesianProduct(predicates);
        if (!forQuery) {
            cartesianProduct.add(0, Collections.nCopies(this.numDimensions, "*"));
        }
        return cartesianProduct;
    }

    public int getNumberUpdates(){
        if(this.collapsed){
            return this.numDimensions;
        } else {
            return this.numDimensions + 1;
        }
    }



    private List<List<String>> cartesianProduct(List<Set<String>> sets) {
        List<List<String>> result = new ArrayList<>();
        cartesianProductHelper(sets, 0, new ArrayList<>(), result);
        return result;
    }

    private void cartesianProductHelper(List<Set<String>> sets, int index, List<String> current, List<List<String>> result) {
        if (index == sets.size()) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (String value : sets.get(index)) {
            current.add(value);
            cartesianProductHelper(sets, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    public int getNumDimensions() {
        return this.numDimensions;
    }

    public boolean isCollapsed() {
        return this.collapsed;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new HashMap<>();
        json.put("num_dimensions", this.numDimensions);
        json.put("possible_values", this.possibleValues.stream().map(ArrayList::new).collect(Collectors.toList()));
        json.put("names", this.attributeNames);
        json.put("collapsed", this.collapsed);
        return json;
    }

    public static ADTree fromJson(Map<String, Object> json) {
        ADTree adTree = new ADTree();
        adTree.numDimensions = ((Double) json.get("num_dimensions")).intValue();
        adTree.possibleValues = ((List<?>) json.get("possible_values"))
            .stream()
            .map(list -> ((List<?>) list).stream().map(Object::toString).collect(Collectors.toSet()))
            .collect(Collectors.toList());
        adTree.attributeNames = (List<String>) json.get("names");
        if (json.get("collapsed") != null) {
            adTree.collapsed = (boolean) json.get("collapsed");
        }
        return adTree;
    }

    public static ADTree fromJson(String jsonFilePath) throws IOException {
        String jsonContent = new String(Files.readAllBytes(Paths.get(jsonFilePath)));

        // Parse the JSON content into a Map using Gson
        Gson gson = new Gson();
        Map<String, Object> json = gson.fromJson(jsonContent, Map.class);

        return ADTree.fromJson(json);
    }
}