package com.github.pagination.connector;

import com.github.pagination.connector.internal.util.ResponseParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResponseParser Tests")
class ResponseParserTest {

    @Nested
    @DisplayName("extractRecords")
    class ExtractRecords {

        @Test
        @DisplayName("extracts from nested data field")
        void extractsFromNestedDataField() {
            String json = "{\"data\":[{\"id\":1},{\"id\":2}],\"total\":100}";
            List<Object> records = ResponseParser.extractRecords(json, "data");
            assertEquals(2, records.size());
        }

        @Test
        @DisplayName("extracts from deeply nested path")
        void extractsFromDeeplyNestedPath() {
            String json = "{\"response\":{\"items\":[{\"id\":1},{\"id\":2},{\"id\":3}]}}";
            List<Object> records = ResponseParser.extractRecords(json, "response.items");
            assertEquals(3, records.size());
        }

        @Test
        @DisplayName("returns empty list for empty array")
        void returnsEmptyListForEmptyArray() {
            String json = "{\"data\":[]}";
            List<Object> records = ResponseParser.extractRecords(json, "data");
            assertTrue(records.isEmpty());
        }

        @Test
        @DisplayName("extracts from root array (no dataFieldPath)")
        void extractsFromRootArray() {
            String json = "[{\"id\":1},{\"id\":2}]";
            List<Object> records = ResponseParser.extractRecords(json, null);
            assertEquals(2, records.size());
        }

        @Test
        @DisplayName("returns empty list for null payload")
        void returnsEmptyListForNullPayload() {
            assertTrue(ResponseParser.extractRecords(null, "data").isEmpty());
        }

        @Test
        @DisplayName("returns empty list for missing field")
        void returnsEmptyListForMissingField() {
            String json = "{\"other\":[1,2,3]}";
            assertTrue(ResponseParser.extractRecords(json, "data").isEmpty());
        }
    }

    @Nested
    @DisplayName("extractTotalCount")
    class ExtractTotalCount {

        @Test
        @DisplayName("extracts numeric total")
        void extractsNumericTotal() {
            String json = "{\"data\":[],\"total\":500}";
            assertEquals(500L, ResponseParser.extractTotalCount(json, "total"));
        }

        @Test
        @DisplayName("extracts total from nested path")
        void extractsTotalFromNestedPath() {
            String json = "{\"meta\":{\"pagination\":{\"total\":1234}}}";
            assertEquals(1234L, ResponseParser.extractTotalCount(json, "meta.pagination.total"));
        }

        @Test
        @DisplayName("returns null when field absent")
        void returnsNullWhenFieldAbsent() {
            String json = "{\"data\":[]}";
            assertNull(ResponseParser.extractTotalCount(json, "total"));
        }

        @Test
        @DisplayName("returns null for blank fieldPath")
        void returnsNullForBlankFieldPath() {
            assertNull(ResponseParser.extractTotalCount("{\"total\":100}", ""));
        }
    }

    @Nested
    @DisplayName("extractCursor")
    class ExtractCursor {

        @Test
        @DisplayName("extracts valid cursor")
        void extractsValidCursor() {
            String json = "{\"data\":[],\"next_cursor\":\"abc123\"}";
            assertEquals("abc123", ResponseParser.extractCursor(json, "next_cursor"));
        }

        @Test
        @DisplayName("returns null for null cursor value")
        void returnsNullForNullCursorValue() {
            String json = "{\"data\":[],\"next_cursor\":null}";
            assertNull(ResponseParser.extractCursor(json, "next_cursor"));
        }

        @Test
        @DisplayName("returns null for empty string cursor")
        void returnsNullForEmptyStringCursor() {
            String json = "{\"next_cursor\":\"\"}";
            assertNull(ResponseParser.extractCursor(json, "next_cursor"));
        }

        @Test
        @DisplayName("extracts cursor from nested path")
        void extractsCursorFromNestedPath() {
            String json = "{\"paging\":{\"cursors\":{\"after\":\"cursor_xyz\"}}}";
            assertEquals("cursor_xyz", ResponseParser.extractCursor(json, "paging.cursors.after"));
        }
    }

    @Nested
    @DisplayName("extractNextLinkFromHeader")
    class ExtractNextLinkFromHeader {

        @Test
        @DisplayName("extracts simple next link")
        void extractsSimpleNextLink() {
            String header = "<https://api.example.com/items?page=2>; rel=\"next\"";
            assertEquals("https://api.example.com/items?page=2",
                    ResponseParser.extractNextLinkFromHeader(header));
        }

        @Test
        @DisplayName("extracts next from multi-relation header")
        void extractsNextFromMultiRelationHeader() {
            String header = "<https://api.example.com/items?page=1>; rel=\"prev\", " +
                            "<https://api.example.com/items?page=3>; rel=\"next\", " +
                            "<https://api.example.com/items?page=10>; rel=\"last\"";
            assertEquals("https://api.example.com/items?page=3",
                    ResponseParser.extractNextLinkFromHeader(header));
        }

        @Test
        @DisplayName("returns null when no next link")
        void returnsNullWhenNoNextLink() {
            String header = "<https://api.example.com/items?page=10>; rel=\"last\"";
            assertNull(ResponseParser.extractNextLinkFromHeader(header));
        }

        @Test
        @DisplayName("returns null for null header value")
        void returnsNullForNullHeaderValue() {
            assertNull(ResponseParser.extractNextLinkFromHeader(null));
        }

        @Test
        @DisplayName("returns null for empty header value")
        void returnsNullForEmptyHeaderValue() {
            assertNull(ResponseParser.extractNextLinkFromHeader(""));
        }
    }

    @Test
    @DisplayName("isEmpty returns true for null list")
    void isEmptyReturnsTrueForNull() {
        assertTrue(ResponseParser.isEmpty(null));
    }

    @Test
    @DisplayName("isEmpty returns true for empty list")
    void isEmptyReturnsTrueForEmpty() {
        assertTrue(ResponseParser.isEmpty(List.of()));
    }

    @Test
    @DisplayName("isEmpty returns false for non-empty list")
    void isEmptyReturnsFalseForNonEmpty() {
        assertFalse(ResponseParser.isEmpty(List.of("a")));
    }
}
