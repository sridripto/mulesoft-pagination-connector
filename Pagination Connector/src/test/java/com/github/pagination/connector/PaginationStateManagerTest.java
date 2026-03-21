package com.github.pagination.connector;

import com.github.pagination.connector.internal.config.PaginationConfig;
import com.github.pagination.connector.internal.model.PaginationState;
import com.github.pagination.connector.internal.model.PaginationStrategy;
import com.github.pagination.connector.internal.util.PaginationStateManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaginationStateManager Tests")
class PaginationStateManagerTest {

    @Test
    @DisplayName("PAGE_BASED: first request params are correct")
    void pageBasedFirstRequestParams() {
        PaginationConfig config = mockConfig(PaginationStrategy.PAGE_BASED, 10);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        assertEquals("1", state.getNextRequestParams().get("page"));
        assertEquals("10", state.getNextRequestParams().get("size"));
    }

    @Test
    @DisplayName("PAGE_BASED: increments page on advance")
    void pageBasedIncrementsPage() {
        PaginationConfig config = mockConfig(PaginationStrategy.PAGE_BASED, 10);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        String json = "{\"data\":[{\"id\":1},{\"id\":2}]}";
        List<Object> records = List.of("r1", "r2");
        manager.evaluateAndAdvance(state, json, null, records);

        assertFalse(state.isDone());
        assertEquals("2", state.getNextRequestParams().get("page"));
    }

    @Test
    @DisplayName("PAGE_BASED: stops on empty response")
    void pageBasedStopsOnEmptyResponse() {
        PaginationConfig config = mockConfig(PaginationStrategy.PAGE_BASED, 10);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        manager.evaluateAndAdvance(state, "{\"data\":[]}", null, List.of());

        assertTrue(state.isDone());
        assertTrue(state.getStopReason().startsWith("EMPTY_RESPONSE"));
    }

    @Test
    @DisplayName("PAGE_BASED: stops when max pages reached")
    void pageBasedStopsAtMaxPages() {
        PaginationConfig config = mockConfig(PaginationStrategy.PAGE_BASED, 10);

        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();


        for (int i = 0; i < 999; i++) {
            state.incrementPagesFetched();
        }

        manager.evaluateAndAdvance(state, "{\"data\":[{\"id\":1}]}", null, List.of("r1"));

        assertTrue(state.isDone());
        assertTrue(state.getStopReason().startsWith("MAX_PAGES"));
    }


    @Test
    @DisplayName("OFFSET_BASED: first request params are correct")
    void offsetBasedFirstRequestParams() {
        PaginationConfig config = mockConfig(PaginationStrategy.OFFSET_BASED, 25);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        assertEquals("0", state.getNextRequestParams().get("offset"));
        assertEquals("25", state.getNextRequestParams().get("limit"));
    }

    @Test
    @DisplayName("OFFSET_BASED: increments offset by pageSize")
    void offsetBasedIncrementsOffset() {
        PaginationConfig config = mockConfig(PaginationStrategy.OFFSET_BASED, 25);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        manager.evaluateAndAdvance(state, "{\"items\":[1,2,3]}", null, List.of("r1","r2","r3"));
        assertEquals("25", state.getNextRequestParams().get("offset"));

        manager.evaluateAndAdvance(state, "{\"items\":[4,5,6]}", null, List.of("r4","r5","r6"));
        assertEquals("50", state.getNextRequestParams().get("offset"));
    }

    @Test
    @DisplayName("OFFSET_BASED: stops when total count reached")
    void offsetBasedStopsWhenTotalCountReached() {
        PaginationConfig config = mockConfigWithTotal(PaginationStrategy.OFFSET_BASED, 10, "total");
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        manager.evaluateAndAdvance(state, "{\"items\":[1,2],\"total\":2}", null, List.of("r1","r2"));

        assertTrue(state.isDone());
        assertTrue(state.getStopReason().startsWith("TOTAL_COUNT"));
    }

    @Test
    @DisplayName("CURSOR_BASED: stops when cursor is null")
    void cursorBasedStopsWhenCursorNull() {
        PaginationConfig config = mockConfigCursor(PaginationStrategy.CURSOR_BASED, 10, "next_cursor");
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        manager.evaluateAndAdvance(state, "{\"data\":[1,2],\"next_cursor\":null}", null, List.of("r1","r2"));

        assertTrue(state.isDone());
        assertTrue(state.getStopReason().startsWith("NULL_CURSOR"));
    }

    @Test
    @DisplayName("CURSOR_BASED: continues when cursor is present")
    void cursorBasedContinuesWhenCursorPresent() {
        PaginationConfig config = mockConfigCursor(PaginationStrategy.CURSOR_BASED, 10, "next_cursor");
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        manager.evaluateAndAdvance(state, "{\"data\":[1,2],\"next_cursor\":\"token_abc\"}", null, List.of("r1","r2"));

        assertFalse(state.isDone());
        assertEquals("token_abc", state.getNextCursor());
    }

    @Test
    @DisplayName("LINK_HEADER: stops when no next link header")
    void linkHeaderStopsWhenNoNextLink() {
        PaginationConfig config = mockConfig(PaginationStrategy.LINK_HEADER, 10);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        manager.evaluateAndAdvance(state, "[1,2,3]", null, List.of("r1","r2","r3"));

        assertTrue(state.isDone());
        assertTrue(state.getStopReason().startsWith("NO_NEXT_LINK"));
    }

    @Test
    @DisplayName("LINK_HEADER: continues when next link header present")
    void linkHeaderContinuesWhenNextLinkPresent() {
        PaginationConfig config = mockConfig(PaginationStrategy.LINK_HEADER, 10);
        PaginationStateManager manager = new PaginationStateManager(config);
        PaginationState state = manager.initialize();

        String linkHeader = "<https://api.example.com/items?page=2>; rel=\"next\"";
        manager.evaluateAndAdvance(state, "[1,2,3]", linkHeader, List.of("r1","r2","r3"));

        assertFalse(state.isDone());
        assertEquals("https://api.example.com/items?page=2", state.getNextUrl());
    }

    private PaginationConfig mockConfig(PaginationStrategy strategy, int pageSize) {
        return new PaginationConfig() {
            @Override public PaginationStrategy getStrategy() { return strategy; }
            @Override public int getPageSize() { return pageSize; }
            @Override public int getStartPage() { return 1; }
            @Override public long getStartOffset() { return 0; }
            @Override public String getPageParamName() { return "page"; }
            @Override public String getPageSizeParamName() { return "size"; }
            @Override public String getOffsetParamName() { return "offset"; }
            @Override public String getLimitParamName() { return "limit"; }
            @Override public String getCursorRequestParamName() { return "cursor"; }
            @Override public String getDataFieldPath() { return "data"; }
            @Override public String getTotalCountFieldPath() { return null; }
            @Override public String getCursorResponseFieldPath() { return null; }
            @Override public String getLinkHeaderName() { return "Link"; }
            @Override public int getMaxPages() { return 1000; }
            @Override public long getMaxTotalRecords() { return -1; }
            @Override public String getCustomStopExpression() { return null; }
            @Override public boolean isFailOnEmptyFirstPage() { return false; }
            @Override public boolean isIncludePaginationState() { return false; }
            @Override public boolean isFlattenPages() { return true; }
        };
    }

    private PaginationConfig mockConfigWithTotal(PaginationStrategy strategy, int pageSize, String totalField) {
        PaginationConfig base = mockConfig(strategy, pageSize);
        return new PaginationConfig() {
            @Override public PaginationStrategy getStrategy() { return strategy; }
            @Override public int getPageSize() { return pageSize; }
            @Override public int getStartPage() { return 1; }
            @Override public long getStartOffset() { return 0; }
            @Override public String getPageParamName() { return "page"; }
            @Override public String getPageSizeParamName() { return "size"; }
            @Override public String getOffsetParamName() { return "offset"; }
            @Override public String getLimitParamName() { return "limit"; }
            @Override public String getCursorRequestParamName() { return "cursor"; }
            @Override public String getDataFieldPath() { return "items"; }
            @Override public String getTotalCountFieldPath() { return totalField; }
            @Override public String getCursorResponseFieldPath() { return null; }
            @Override public String getLinkHeaderName() { return "Link"; }
            @Override public int getMaxPages() { return 1000; }
            @Override public long getMaxTotalRecords() { return -1; }
            @Override public String getCustomStopExpression() { return null; }
            @Override public boolean isFailOnEmptyFirstPage() { return false; }
            @Override public boolean isIncludePaginationState() { return false; }
            @Override public boolean isFlattenPages() { return true; }
        };
    }

    private PaginationConfig mockConfigCursor(PaginationStrategy strategy, int pageSize, String cursorField) {
        return new PaginationConfig() {
            @Override public PaginationStrategy getStrategy() { return strategy; }
            @Override public int getPageSize() { return pageSize; }
            @Override public int getStartPage() { return 1; }
            @Override public long getStartOffset() { return 0; }
            @Override public String getPageParamName() { return "page"; }
            @Override public String getPageSizeParamName() { return "size"; }
            @Override public String getOffsetParamName() { return "offset"; }
            @Override public String getLimitParamName() { return "limit"; }
            @Override public String getCursorRequestParamName() { return "cursor"; }
            @Override public String getDataFieldPath() { return "data"; }
            @Override public String getTotalCountFieldPath() { return null; }
            @Override public String getCursorResponseFieldPath() { return cursorField; }
            @Override public String getLinkHeaderName() { return "Link"; }
            @Override public int getMaxPages() { return 1000; }
            @Override public long getMaxTotalRecords() { return -1; }
            @Override public String getCustomStopExpression() { return null; }
            @Override public boolean isFailOnEmptyFirstPage() { return false; }
            @Override public boolean isIncludePaginationState() { return false; }
            @Override public boolean isFlattenPages() { return true; }
        };
    }
}
