package utils;

import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;
import pachasketch.utils.QueryFactory;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryFactoryTest {

    @Test
    void fromJSON_ReturnsListOfQueries_WhenFileIsValid() {
        String validFilePath = "src/main/resources/queries/tpch/tpch_random.json";
        List<List<Object>> queries = QueryFactory.fromJSON(validFilePath);
        assertNotNull(queries);
        assertFalse(queries.isEmpty());
    }

    @Test
    void fromJSON_ThrowsRuntimeException_WhenFileDoesNotExist() {
        String invalidFilePath = "src/main/resources/queries/tpch/non_existent.json";
        assertThrows(RuntimeException.class, () -> QueryFactory.fromJSON(invalidFilePath));
    }

    @Test
    void fromString_ReturnsListOfObjects_WhenQueryContainsValidElements() {
        String query = "*, [1.0, 2.0], text, [val1, val2, val3]";
        List<Object> result = QueryFactory.fromString(query);
        assertEquals(4, result.size());
        assertEquals("*", result.get(0));
        List<Double> rangeQuery = (List<Double>) result.get(1);
        assertTrue(rangeQuery instanceof List);
        assertEquals(2, rangeQuery.size());
        assertEquals(1.0, rangeQuery.get(0));
        assertEquals(2.0, rangeQuery.get(1));
        assertEquals("text", ((List<?>) result.get(2)).get(0));
        List<String> catPredicate = (List<String>) result.get(3);
        assertTrue(catPredicate instanceof List);
        assertEquals(3, catPredicate.size());
        assertEquals("val1", catPredicate.get(0));
        assertEquals("val2", catPredicate.get(1));
        assertEquals("val3", catPredicate.get(2));

    }

    @Test
    void fromString_ReturnsEmptyList_WhenQueryIsEmpty() {
        String query = "";
        List<Object> result = QueryFactory.fromString(query);
        assertTrue(result.isEmpty());
    }

    @Test
    void fromString_HandlesNestedLists_WhenQueryContainsMultipleNestedLists() {
        String query = "[1.0, 2.0], [3.5, 4.5]";
        List<Object> result = QueryFactory.fromString(query);
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof List);
        assertTrue(result.get(1) instanceof List);
    }

    @Test
    void fromString_HandlesWhitespaceCorrectly_WhenQueryContainsExtraSpaces() {
        String query = "  *,   [ 1.0 , 2.0 ] ,  text  ";
        List<Object> result = QueryFactory.fromString(query);
        assertEquals(3, result.size());
        assertEquals("*", result.get(0));
        assertTrue(result.get(1) instanceof List);
        assertEquals("text", ((List<?>) result.get(2)).get(0));
    }
}