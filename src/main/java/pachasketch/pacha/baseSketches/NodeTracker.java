package pachasketch.pacha.baseSketches;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class NodeTracker implements Filter {
    private final HashSet<String> elements;

    public NodeTracker() {
        this.elements = new HashSet<>();
    }

    @Override
    public void update(String element) {
        elements.add(element);
    }

    @Override
    public void updateBatch(Collection<String> elements) {
        this.elements.addAll(elements);
    }

    @Override
    public boolean query(String element) {
        return elements.contains(element);
    }

    @Override
    public List<String> filterBatch(Collection<String> elements){
        ArrayList<String> filtered = new ArrayList<>();
        for (String element : elements) {
            if (this.elements.contains(element)) {
                filtered.add(element);
            }
        }
        return filtered;
    }

    @Override
    public void merge(Filter other) {
        if (!(other instanceof NodeTracker)) {
            throw new IllegalArgumentException("Cannot merge with a non-NodeTracker instance.");
        }
        NodeTracker otherTracker = (NodeTracker) other;
        this.elements.addAll(otherTracker.elements);
    }

    @Override
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this.elements);
    }

    public static NodeTracker fromJson(String json) {
        Gson gson = new Gson();
        NodeTracker tracker = new NodeTracker();
        String[] elements = gson.fromJson(json, String[].class);
        for (String element : elements) {
            tracker.update(element);
        }
        return tracker;
    }

    @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            NodeTracker other = (NodeTracker) obj;
            return this.elements.equals(other.elements);
        }
}