package pachasketch.utils;

import pachasketch.pacha.PachaSketch;
import pachasketch.pacha.utils.QueryResult;

import java.util.ArrayList;
import java.util.List;

public class QuerySetEvaluator {
    public static void evaluateQuerySet(PachaSketch pachaSketch, String querySetFile, String resultFile){
        List<List<Object>> queries = QueryFactory.fromJSON(querySetFile);
        List<QueryResult> results = new ArrayList<>(queries.size());
        for(List<Object> query : queries){
            QueryResult result = pachaSketch.query(query, true, false);
            results.add(result);
        }

        writePachaResultsToFile(results, resultFile);
    }

    private static void writePachaResultsToFile(List<QueryResult> results, String resultFile) {

    }
}
