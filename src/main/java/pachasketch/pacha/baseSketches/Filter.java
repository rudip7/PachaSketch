package pachasketch.pacha.baseSketches;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface Filter {
    void update(String element);
    void updateBatch(Collection<String> elements);
    boolean query(String element);

    public List<String> filterBatch(Collection<String> elements);

    void merge(Filter other);

    String toJson();
    double getSizeInMB();
}
