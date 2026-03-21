package com.github.pagination.connector.internal.util;

import com.github.pagination.connector.internal.config.PaginationConfig;
import com.github.pagination.connector.internal.model.PaginationState;
import com.github.pagination.connector.internal.model.PaginationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaginationStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaginationStateManager.class);

    private final PaginationConfig config;

    public PaginationStateManager(PaginationConfig config) {
        this.config = config;
    }

    public PaginationState initialize() {
        PaginationState state = new PaginationState();
        state.setNextRequestParams(buildFirstRequestParams());
        return state;
    }

    public void evaluateAndAdvance(PaginationState state,
                                   String responseBody,
                                   String linkHeader,
                                   List<Object> records) {

        state.incrementPagesFetched();
        state.addRecordsFetched(records.size());

        if (config.getTotalCountFieldPath() != null && !config.getTotalCountFieldPath().isBlank()) {
            Long total = ResponseParser.extractTotalCount(responseBody, config.getTotalCountFieldPath());
            if (total != null) {
                state.setTotalRecords(total);
                LOGGER.debug("Total records from API: {}", total);
            }
        }

        if (config.getStrategy() == PaginationStrategy.CURSOR_BASED) {
            String cursor = ResponseParser.extractCursor(responseBody, config.getCursorResponseFieldPath());
            state.setNextCursor(cursor);
            LOGGER.debug("Next cursor: {}", cursor);
        }

        if (config.getStrategy() == PaginationStrategy.LINK_HEADER) {
            String nextUrl = ResponseParser.extractNextLinkFromHeader(linkHeader);
            state.setNextUrl(nextUrl);
            LOGGER.debug("Next URL from Link header: {}", nextUrl);
        }

        if (checkMaxPages(state)) return;
        if (checkMaxRecords(state)) return;
        if (checkEmptyResponse(state, records)) return;
        if (checkTotalCount(state)) return;
        if (checkNullCursor(state)) return;
        if (checkNoNextLink(state)) return;

        if (config.getStrategy() == PaginationStrategy.PAGE_BASED) {
            state.incrementPage();
        } else if (config.getStrategy() == PaginationStrategy.OFFSET_BASED) {
            state.incrementOffset(config.getPageSize());
        }

        state.setNextRequestParams(buildNextRequestParams(state));
        LOGGER.debug("Advanced state: {}", state);
    }

    private boolean checkMaxPages(PaginationState state) {
        if (config.getMaxPages() > 0 && state.getPagesFetched() >= config.getMaxPages()) {
            state.markDone("MAX_PAGES: fetched " + state.getPagesFetched() + " pages (limit: " + config.getMaxPages() + ")");
            LOGGER.info("Pagination stopped: {}", state.getStopReason());
            return true;
        }
        return false;
    }

    private boolean checkMaxRecords(PaginationState state) {
        if (config.getMaxTotalRecords() > 0 && state.getTotalRecordsFetched() >= config.getMaxTotalRecords()) {
            state.markDone("MAX_RECORDS: fetched " + state.getTotalRecordsFetched() + " records (limit: " + config.getMaxTotalRecords() + ")");
            LOGGER.info("Pagination stopped: {}", state.getStopReason());
            return true;
        }
        return false;
    }

    private boolean checkEmptyResponse(PaginationState state, List<Object> records) {
        if (ResponseParser.isEmpty(records)) {
            state.markDone("EMPTY_RESPONSE: page " + state.getPagesFetched() + " returned 0 records");
            LOGGER.info("Pagination stopped: {}", state.getStopReason());
            return true;
        }
        return false;
    }

    private boolean checkTotalCount(PaginationState state) {
        Long total = state.getTotalRecords();
        if (total != null && state.getTotalRecordsFetched() >= total) {
            state.markDone("TOTAL_COUNT: fetched " + state.getTotalRecordsFetched() + " of " + total + " total records");
            LOGGER.info("Pagination stopped: {}", state.getStopReason());
            return true;
        }
        return false;
    }

    private boolean checkNullCursor(PaginationState state) {
        if (config.getStrategy() == PaginationStrategy.CURSOR_BASED) {
            if (state.getNextCursor() == null) {
                state.markDone("NULL_CURSOR: next cursor is null/absent after page " + state.getPagesFetched());
                LOGGER.info("Pagination stopped: {}", state.getStopReason());
                return true;
            }
        }
        return false;
    }

    private boolean checkNoNextLink(PaginationState state) {
        if (config.getStrategy() == PaginationStrategy.LINK_HEADER) {
            if (state.getNextUrl() == null) {
                state.markDone("NO_NEXT_LINK: no 'next' Link header after page " + state.getPagesFetched());
                LOGGER.info("Pagination stopped: {}", state.getStopReason());
                return true;
            }
        }
        return false;
    }

    private Map<String, String> buildFirstRequestParams() {
        Map<String, String> params = new HashMap<>();
        switch (config.getStrategy()) {
            case PAGE_BASED -> {
                params.put(config.getPageParamName(), String.valueOf(config.getStartPage()));
                params.put(config.getPageSizeParamName(), String.valueOf(config.getPageSize()));
            }
            case OFFSET_BASED -> {
                params.put(config.getOffsetParamName(), String.valueOf(config.getStartOffset()));
                params.put(config.getLimitParamName(), String.valueOf(config.getPageSize()));
            }
            case CURSOR_BASED -> {
                params.put(config.getLimitParamName(), String.valueOf(config.getPageSize()));
            }
            case LINK_HEADER -> {
            }
        }
        return params;
    }

    private Map<String, String> buildNextRequestParams(PaginationState state) {
        Map<String, String> params = new HashMap<>();
        switch (config.getStrategy()) {
            case PAGE_BASED -> {
                params.put(config.getPageParamName(), String.valueOf(state.getCurrentPage()));
                params.put(config.getPageSizeParamName(), String.valueOf(config.getPageSize()));
            }
            case OFFSET_BASED -> {
                params.put(config.getOffsetParamName(), String.valueOf(state.getCurrentOffset()));
                params.put(config.getLimitParamName(), String.valueOf(config.getPageSize()));
            }
            case CURSOR_BASED -> {
                params.put(config.getCursorRequestParamName(), state.getNextCursor());
                params.put(config.getLimitParamName(), String.valueOf(config.getPageSize()));
            }
            case LINK_HEADER -> {

            }
        }
        return params;
    }

    public String getNextUrl(PaginationState state) {
        return state.getNextUrl();
    }
}
