package pachasketch.omni.utils;

public class OmniQueryResult{
    int estimate;
    double runtime;
    int estimateCase; // 1: too little witnesses, 2: normal estimate

    public OmniQueryResult(int estimate, double runtime, int estimateCase) {
        this.estimate = estimate;
        this.runtime = runtime;
        this.estimateCase = estimateCase;
    }

    public OmniQueryResult(int estimate, int estimateCase) {
        this.estimate = estimate;
        this.runtime = -1;
        this.estimateCase = estimateCase;
    }

    public int getEstimate() {
        return estimate;
    }

    public void setEstimate(int estimate) {
        this.estimate = estimate;
    }

    public double getRuntime() {
        return runtime;
    }

    public void setRuntime(double runtime) {
        this.runtime = runtime;
    }

    public int getEstimateCase() {
        return estimateCase;
    }

    public void setEstimateCase(int estimateCase) {
        this.estimateCase = estimateCase;
    }
}
