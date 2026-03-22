package com.github.pagination.connector.internal.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagination.connector.internal.config.CursorBasedConfig;
import com.github.pagination.connector.internal.config.LinkHeaderConfig;
import com.github.pagination.connector.internal.config.OffsetBasedConfig;
import com.github.pagination.connector.internal.config.PageBasedConfig;
import com.github.pagination.connector.internal.model.PaginationState;
import com.github.pagination.connector.internal.util.ResponseParser;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

public class PaginationScopeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaginationScopeOperations.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DisplayName("Page Based Pagination")
    @Summary("Fetches all pages using a page number parameter. Payload inside scope contains the query params for the current page.")
    @org.mule.runtime.extension.api.annotation.param.MediaType(value = ANY, strict = false)
    public void paginatePageBased(Chain operations,
                                   @ParameterGroup(name = "Page Based Configuration") PageBasedConfig config,
                                   CompletionCallback<Object, Void> callback) {

        PaginationState state = new PaginationState();
        state.setNextRequestParams(buildPageBasedParams(config.getStartPage(), config.getPageSize(),
                config.getPageParamName(), config.getPageSizeParamName()));

        List<Object> allRecords = new ArrayList<>();
        List<Object> allPages = new ArrayList<>();

        LOGGER.info("Starting PAGE_BASED pagination. StartPage={}, PageSize={}", config.getStartPage(), config.getPageSize());
        executeLoop(operations, state, allRecords, allPages, config.getMaxPages(), config.getDataFieldPath(),
                config.getTotalCountFieldPath(), null, null, config.getCustomStopExpression(),
                config.isFlattenPages(), config.isIncludePaginationState(), "PAGE_BASED", callback,
                () -> {
                    state.incrementPage();
                    state.setNextRequestParams(buildPageBasedParams(state.getCurrentPage(), config.getPageSize(),
                            config.getPageParamName(), config.getPageSizeParamName()));
                });
    }

    @DisplayName("Offset Based Pagination")
    @Summary("Fetches all pages using an offset parameter. Payload inside scope contains the query params for the current page.")
    @org.mule.runtime.extension.api.annotation.param.MediaType(value = ANY, strict = false)
    public void paginateOffsetBased(Chain operations,
                                     @ParameterGroup(name = "Offset Based Configuration") OffsetBasedConfig config,
                                     CompletionCallback<Object, Void> callback) {

        PaginationState state = new PaginationState();
        state.setNextRequestParams(buildOffsetBasedParams(config.getStartOffset(), config.getPageSize(),
                config.getOffsetParamName(), config.getLimitParamName()));

        List<Object> allRecords = new ArrayList<>();
        List<Object> allPages = new ArrayList<>();

        LOGGER.info("Starting OFFSET_BASED pagination. StartOffset={}, PageSize={}", config.getStartOffset(), config.getPageSize());
        executeLoop(operations, state, allRecords, allPages, config.getMaxPages(), config.getDataFieldPath(),
                config.getTotalCountFieldPath(), null, null, config.getCustomStopExpression(),
                config.isFlattenPages(), config.isIncludePaginationState(), "OFFSET_BASED", callback,
                () -> {
                    state.incrementOffset(config.getPageSize());
                    state.setNextRequestParams(buildOffsetBasedParams(state.getCurrentOffset(), config.getPageSize(),
                            config.getOffsetParamName(), config.getLimitParamName()));
                });
    }

    @DisplayName("Cursor Based Pagination")
    @Summary("Fetches all pages using a cursor/token returned by the API. Payload inside scope contains the query params for the current page.")
    @org.mule.runtime.extension.api.annotation.param.MediaType(value = ANY, strict = false)
    public void paginateCursorBased(Chain operations,
                                     @ParameterGroup(name = "Cursor Based Configuration") CursorBasedConfig config,
                                     CompletionCallback<Object, Void> callback) {

        PaginationState state = new PaginationState();
        Map<String, String> firstParams = new HashMap<>();
        firstParams.put(config.getLimitParamName(), String.valueOf(config.getPageSize()));
        state.setNextRequestParams(firstParams);

        List<Object> allRecords = new ArrayList<>();
        List<Object> allPages = new ArrayList<>();

        LOGGER.info("Starting CURSOR_BASED pagination. PageSize={}", config.getPageSize());
        executeLoop(operations, state, allRecords, allPages, config.getMaxPages(), config.getDataFieldPath(),
                config.getTotalCountFieldPath(), config.getCursorResponseFieldPath(), config.getCursorParamName(),
                config.getCustomStopExpression(), config.isFlattenPages(), config.isIncludePaginationState(),
                "CURSOR_BASED", callback,
                () -> {
                    Map<String, String> nextParams = new HashMap<>();
                    nextParams.put(config.getCursorParamName(), state.getNextCursor());
                    nextParams.put(config.getLimitParamName(), String.valueOf(config.getPageSize()));
                    state.setNextRequestParams(nextParams);
                });
    }

    @DisplayName("Link Header Based Pagination")
    @Summary("Fetches all pages by following the URL in the Link response header. Use vars.nextPageUrl as the request URL inside the scope.")
    @org.mule.runtime.extension.api.annotation.param.MediaType(value = ANY, strict = false)
    public void paginateLinkHeader(Chain operations,
                                    @ParameterGroup(name = "Link Header Configuration") LinkHeaderConfig config,
                                    CompletionCallback<Object, Void> callback) {

        PaginationState state = new PaginationState();
        state.setNextRequestParams(new HashMap<>());

        List<Object> allRecords = new ArrayList<>();
        List<Object> allPages = new ArrayList<>();

        LOGGER.info("Starting LINK_HEADER pagination.");
        executeLoop(operations, state, allRecords, allPages, config.getMaxPages(), config.getDataFieldPath(),
                config.getTotalCountFieldPath(), null, null, config.getCustomStopExpression(),
                config.isFlattenPages(), config.isIncludePaginationState(), "LINK_HEADER", callback,
                () -> {

                });
    }

    private void executeLoop(Chain operations,
                              PaginationState state,
                              List<Object> allRecords,
                              List<Object> allPages,
                              int maxPages,
                              String dataFieldPath,
                              String totalCountFieldPath,
                              String cursorResponseFieldPath,
                              String cursorParamName,
                              String customStopExpression,
                              boolean flattenPages,
                              boolean includePaginationState,
                              String strategy,
                              CompletionCallback<Object, Void> callback,
                              Runnable advanceState) {

        LOGGER.info("Fetching page {} | params: {}", state.getPagesFetched() + 1, state.getNextRequestParams());

        operations.process(
                state.getNextRequestParams(),
                state.getNextRequestParams(),
                result -> {
                    try {
                        String responseBody = extractResponseBody(result);

                        List<Object> pageRecords = ResponseParser.extractRecords(responseBody, dataFieldPath);
                        LOGGER.info("Page {} returned {} records", state.getPagesFetched() + 1, pageRecords.size());

                        state.incrementPagesFetched();
                        state.addRecordsFetched(pageRecords.size());
                        state.addPage(responseBody);
                        allPages.add(responseBody);
                        allRecords.addAll(pageRecords);


                        if (totalCountFieldPath != null && !totalCountFieldPath.isBlank()) {
                            Long total = ResponseParser.extractTotalCount(responseBody, totalCountFieldPath);
                            if (total != null) state.setTotalRecords(total);
                        }


                        if (cursorResponseFieldPath != null && !cursorResponseFieldPath.isBlank()) {
                            state.setNextCursor(ResponseParser.extractCursor(responseBody, cursorResponseFieldPath));
                        }


                        if ("LINK_HEADER".equals(strategy)) {
                            String linkHeader = extractLinkHeader(result, "Link");
                            String nextUrl = ResponseParser.extractNextLinkFromHeader(linkHeader);
                            state.setNextUrl(nextUrl);
                            if (nextUrl != null) {
                                Map<String, String> p = new HashMap<>();
                                p.put("nextPageUrl", nextUrl);
                                state.setNextRequestParams(p);
                            }
                        }


                        String stopReason = evaluateStopConditions(state, pageRecords, maxPages,
                                cursorResponseFieldPath, strategy, customStopExpression, responseBody);

                        if (stopReason != null) {
                            state.markDone(stopReason);
                            LOGGER.info("Pagination complete. Pages={}, Records={}, Reason={}",
                                    state.getPagesFetched(), state.getTotalRecordsFetched(), stopReason);
                            callback.success(Result.<Object, Void>builder()
                                    .output(buildFinalPayload(flattenPages, includePaginationState,
                                            allRecords, allPages, state, strategy))
                                    .mediaType(MediaType.APPLICATION_JSON)
                                    .build());
                        } else {
                            advanceState.run();
                            executeLoop(operations, state, allRecords, allPages, maxPages, dataFieldPath,
                                    totalCountFieldPath, cursorResponseFieldPath, cursorParamName,
                                    customStopExpression, flattenPages, includePaginationState,
                                    strategy, callback, advanceState);
                        }

                    } catch (Exception e) {
                        LOGGER.error("Error on page {}: {}", state.getPagesFetched() + 1, e.getMessage(), e);
                        callback.error(new ModuleException(PaginationErrors.PROCESSING_ERROR, e));
                    }
                },
                (error, result) -> {
                    LOGGER.error("Chain failed on page {}: {}", state.getPagesFetched() + 1, error.getMessage());
                    callback.error(new ModuleException(PaginationErrors.CHAIN_ERROR,
                            new RuntimeException("Chain error on page " + (state.getPagesFetched() + 1) + ": " + error.getMessage())));
                }
        );
    }

    private String evaluateStopConditions(PaginationState state, List<Object> pageRecords,
                                           int maxPages, String cursorResponseFieldPath,
                                           String strategy, String customStopExpression,
                                           String responseBody) {
        if (maxPages > 0 && state.getPagesFetched() >= maxPages)
            return "MAX_PAGES: fetched " + state.getPagesFetched() + " pages";

        if (ResponseParser.isEmpty(pageRecords))
            return "EMPTY_RESPONSE: page " + state.getPagesFetched() + " returned 0 records";

        Long total = state.getTotalRecords();
        if (total != null && state.getTotalRecordsFetched() >= total)
            return "TOTAL_COUNT: fetched " + state.getTotalRecordsFetched() + " of " + total;

        if ("CURSOR_BASED".equals(strategy) && state.getNextCursor() == null)
            return "NULL_CURSOR: cursor is null after page " + state.getPagesFetched();

        if ("LINK_HEADER".equals(strategy) && state.getNextUrl() == null)
            return "NO_NEXT_LINK: no next Link header after page " + state.getPagesFetched();

        if (customStopExpression != null && !customStopExpression.isBlank()
                && evaluateCustomExpression(customStopExpression, responseBody))
            return "CUSTOM_EXPRESSION: " + customStopExpression;

        return null;
    }

    private boolean evaluateCustomExpression(String expression, String responseBody) {
        try {
            if (expression.contains("payload.") && expression.contains("==")) {
                String[] parts = expression.split("==");
                if (parts.length == 2) {
                    String fieldPath = parts[0].trim().replace("payload.", "");
                    String expectedValue = parts[1].trim().replaceAll("[\"']", "");
                    String actualValue = ResponseParser.extractCursor(responseBody, fieldPath);
                    if (actualValue == null && "null".equals(expectedValue)) return true;
                    if (actualValue != null && actualValue.equalsIgnoreCase(expectedValue)) return true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to evaluate stop expression '{}': {}", expression, e.getMessage());
        }
        return false;
    }

    private Map<String, String> buildPageBasedParams(int page, int size, String pageParam, String sizeParam) {
        Map<String, String> params = new HashMap<>();
        params.put(pageParam, String.valueOf(page));
        params.put(sizeParam, String.valueOf(size));
        return params;
    }

    private Map<String, String> buildOffsetBasedParams(long offset, int limit, String offsetParam, String limitParam) {
        Map<String, String> params = new HashMap<>();
        params.put(offsetParam, String.valueOf(offset));
        params.put(limitParam, String.valueOf(limit));
        return params;
    }

    private String extractResponseBody(Result<Object, Object> result) {
        if (result == null || result.getOutput() == null) return null;
        Object output = result.getOutput();
        if (output instanceof String s) return s;
        if (output instanceof byte[] bytes) return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (output instanceof java.io.InputStream is) {
            try { return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); }
            catch (Exception e) { LOGGER.error("Failed to read InputStream: {}", e.getMessage()); return null; }
        }
        if (output instanceof org.mule.runtime.api.streaming.bytes.CursorStreamProvider provider) {
            try (java.io.InputStream is = provider.openCursor()) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) { LOGGER.error("Failed to read CursorStreamProvider: {}", e.getMessage()); return null; }
        }
        return output.toString();
    }

    private String extractLinkHeader(Result<Object, Object> result, String headerName) {
        try {
            if (result.getAttributes().isPresent()) {
                Object attrs = result.getAttributes().get();
                var headersMethod = attrs.getClass().getMethod("getHeaders");
                Object headers = headersMethod.invoke(attrs);
                if (headers instanceof Map<?, ?> headersMap) {
                    Object val = headersMap.get(headerName);
                    if (val == null) val = headersMap.get(headerName.toLowerCase());
                    return val != null ? val.toString() : null;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not extract Link header: {}", e.getMessage());
        }
        return null;
    }

    private String buildFinalPayload(boolean flattenPages, boolean includePaginationState,
                                      List<Object> allRecords, List<Object> allPages,
                                      PaginationState state, String strategy) {
        try {
            if (includePaginationState) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("pagesFetched", state.getPagesFetched());
                result.put("totalRecordsFetched", state.getTotalRecordsFetched());
                result.put("totalRecordsReported", state.getTotalRecords());
                result.put("stopReason", state.getStopReason());
                result.put("strategy", strategy);
                result.put("records", flattenPages ? allRecords : allPages);
                return MAPPER.writeValueAsString(result);
            }
            return MAPPER.writeValueAsString(flattenPages ? allRecords : allPages);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize payload: {}", e.getMessage());
            return "[]";
        }
    }
}