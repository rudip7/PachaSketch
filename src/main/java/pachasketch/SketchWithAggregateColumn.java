package pachasketch;

import pachasketch.pacha.utils.PachaQueryResult;

import java.util.List;

public interface SketchWithAggregateColumn extends Sketch{
    void update(String[] element, int value);

    PachaQueryResult query(List<Object> query, boolean detailed, boolean debug);
}
