package pachasketch.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class QueryFactory {

    public static List<List<Object>> fromJSON(String pathToFile) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(pathToFile)) {
            Type mapType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> jsonData = gson.fromJson(reader, mapType);
            List<List<Object>> queries = (List<List<Object>>) jsonData.get("queries");
            return queries;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to read JSON file: " + pathToFile, e);
        }
    }

    public static List<Object> fromString(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Object> result = new ArrayList<>();
        String[] elements = query.split(",(?=(?:[^\\[\\]]*\\[[^\\[\\]]*\\])*[^\\[\\]]*$)");

        for (String element : elements) {
            element = element.trim();
            if (element.equals("*")) {
                result.add("*");
            } else if (element.startsWith("[") && element.endsWith("]")) {
                String content = element.substring(1, element.length() - 1);
                String[] values = content.split(",");
                if(values.length == 2){
                    try {
                        List<Double> nestedList = new ArrayList<>();
                        for (String number : values) {
                            nestedList.add(Double.parseDouble(number.trim()));
                        }
                        result.add(nestedList);
                    } catch (NumberFormatException e) {
                        List<String> nestedList = Stream.of(values).map(String::trim).toList();
                        result.add(nestedList);
                    }
                } else {
                    List<String> nestedList = Stream.of(values).map(String::trim).toList();
                    result.add(nestedList);
                }
            } else {
                result.add(List.of(element.trim()));
            }
        }

        return result;
    }

}