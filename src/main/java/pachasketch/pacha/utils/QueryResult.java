package pachasketch.pacha.utils;

import java.util.List;
import java.util.Map;

public class QueryResult {
    private Map<Integer, List<String>> regions;
    private QueryStats stats;
    private int estimate;

    public QueryResult(Map<Integer, List<String>> regions, QueryStats stats, Integer estimate) {
        this.regions = regions;
        this.stats = stats;
        this.estimate = estimate;
    }

    public QueryResult(Map<Integer, List<String>> regions, QueryStats stats) {
        this.regions = regions;
        this.stats = stats;
        this.estimate = -1;
    }

    public QueryResult(int estimate) {
        this.regions = null;
        this.stats = null;
        this.estimate = estimate;
    }
    public void setEstimate(int estimate) {
        this.estimate = estimate;
    }

    public Map<Integer, List<String>> regions() {
        return regions;
    }

    public QueryStats stats() {
        return stats;
    }

    public int estimate() {
        return estimate;
    }
}
