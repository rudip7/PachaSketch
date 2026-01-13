package pachasketch.utils;

public class SimpleQueryResult {
    double estimate;
    double runtime;

    public SimpleQueryResult(double estimate, double runtime) {
        this.estimate = estimate;
        this.runtime = runtime;
    }

    public SimpleQueryResult(double estimate) {
        this.estimate = estimate;
        this.runtime = -1;
    }

    public double getEstimate() {
        return estimate;
    }

    public void setEstimate(double estimate) {
        this.estimate = estimate;
    }

    public double getRuntime() {
        return runtime;
    }

    public void setRuntime(double runtime) {
        this.runtime = runtime;
    }

}
