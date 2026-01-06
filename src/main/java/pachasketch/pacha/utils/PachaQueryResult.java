package pachasketch.pacha.utils;

import java.util.List;
import java.util.Map;

public class PachaQueryResult {
    private Map<Integer, List<String>> regions;
    private QueryStats stats;
    private int forcedAlignment = -1;
    private double runtime;
    private double estimate;

    public PachaQueryResult(Map<Integer, List<String>> regions, QueryStats stats, int forcedAlignment, int estimate) {
        this.regions = regions;
        this.stats = stats;
        this.estimate = estimate;
    }

    public PachaQueryResult(Map<Integer, List<String>> regions, QueryStats stats, int forcedAlignment) {
        this.regions = regions;
        this.stats = stats;
        this.estimate = -1;
        this.forcedAlignment = forcedAlignment;
    }

    public PachaQueryResult(int estimate) {
        this.regions = null;
        this.stats = null;
        this.estimate = estimate;
    }
    public void setEstimate(double estimate) {
        this.estimate = estimate;
    }

    public Map<Integer, List<String>> regions() {
        return regions;
    }

    public QueryStats stats() {
        return stats;
    }

    public double estimate() {
        return estimate;
    }

    public void setForcedAlignment(int level){
        this.forcedAlignment = level;
    }

    public int getForcedAlignment(){
        return this.forcedAlignment;
    }

    public void setRuntime(double runtime){
        this.runtime = runtime;
    }

    public double getRuntime(){
        return this.runtime;
    }
}
