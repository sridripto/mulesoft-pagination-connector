package com.github.pagination.connector.internal.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagination.connector.internal.config.PaginationConfig;
import com.github.pagination.connector.internal.model.PaginationState;
import com.github.pagination.connector.internal.model.PaginationStrategy;
import com.github.pagination.connector.internal.util.PaginationStateManager;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

public class PaginationScopeOperations {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaginationScopeOperations.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @DisplayName("Paginate")
    @Summary("Executes the inner chain repeatedly across all pages, collecting results until a stop condition is met.")
    @org.mule.runtime.extension.api.annotation.param.MediaType(value = ANY, strict = false)
    public void paginate(Chain operations,
                         @ParameterGroup(name = "Pagination Configuration") PaginationConfig config,
                         CompletionCallback<Object, Void> callback) {

        PaginationStateManager stateManager = new PaginationStateManager(config);
        PaginationState state = stateManager.initialize();
        List<Object> allRecords = new ArrayList<>();
        List<Object> allPages = new ArrayList<>();

        LOGGER.info("Starting pagination. Strategy={}, PageSize={}", config.getStrategy(), config.getPageSize());
        executeNextPage(operations, config, stateManager, state, allRecords, allPages, callback);
    }

    private void executeNextPage(Chain operations,
                                 PaginationConfig config,
                                 PaginationStateManager stateManager,
                                 PaginationState state,
                                 List<Object> allRecords,
                                 List<Object> allPages,
                                 CompletionCallback<Object, Void> callback) {

        LOGGER.info("Fetching page {} | params: {}", state.getPagesFetched() + 1, state.getNextRequestParams());

        operations.process(
                state.getNextRequestParams(),
                state.getNextRequestParams(),
                result -> {
                    try {
                        String responseBody = extractResponseBody(result);
                        String linkHeader = extractLinkHeader(result, config.getLinkHeaderName());

                        List<Object> pageRecords = ResponseParser.extractRecords(responseBody, config.getDataFieldPath());
                        LOGGER.info("Page {} returned {} records", state.getPagesFetched() + 1, pageRecords.size());

                        if (state.getPagesFetched() == 0 && pageRecords.isEmpty() && config.isFailOnEmptyFirstPage()) {
                            callback.error(new ModuleException(
                                    PaginationErrors.EMPTY_FIRST_PAGE,
                                    new IllegalStateException("First page returned no records")));
                            return;
                        }

                        state.addPage(responseBody);
                        allPages.add(responseBody);
                        allRecords.addAll(pageRecords);

                        stateManager.evaluateAndAdvance(state, responseBody, linkHeader, pageRecords);

                        if (!state.isDone() && config.getCustomStopExpression() != null
                                && !config.getCustomStopExpression().isBlank()) {
                            if (evaluateCustomExpression(config.getCustomStopExpression(), responseBody, state)) {
                                state.markDone("CUSTOM_EXPRESSION: " + config.getCustomStopExpression());
                            }
                        }

                        if (state.isDone()) {
                            LOGGER.info("Pagination complete. Pages={}, Records={}, Reason={}",
                                    state.getPagesFetched(), state.getTotalRecordsFetched(), state.getStopReason());

                            callback.success(Result.<Object, Void>builder()
                                    .output(buildFinalPayload(config, allRecords, allPages, state))
                                    .mediaType(MediaType.APPLICATION_JSON)
                                    .build());
                        } else {
                            executeNextPage(operations, config, stateManager, state, allRecords, allPages, callback);
                        }

                    } catch (Exception e) {
                        LOGGER.error("Error on page {}: {}", state.getPagesFetched() + 1, e.getMessage(), e);
                        callback.error(new ModuleException(PaginationErrors.PROCESSING_ERROR, e));
                    }
                },
                (error, result) -> {
                    LOGGER.error("Chain failed on page {}: {}", state.getPagesFetched() + 1, error.getMessage());
                    callback.error(new ModuleException(
                            PaginationErrors.CHAIN_ERROR,
                            new RuntimeException("Chain error on page " + (state.getPagesFetched() + 1) + ": " + error.getMessage())));
                }
        );
    }

    private String extractResponseBody(Result<Object, Object> result) {
        if (result == null || result.getOutput() == null) return null;
        Object output = result.getOutput();
        if (output instanceof String s) return s;
        if (output instanceof byte[] bytes) return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        if (output instanceof java.io.InputStream is) {
            try {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.error("Failed to read InputStream: {}", e.getMessage());
                return null;
            }
        }
        if (output instanceof org.mule.runtime.api.streaming.bytes.CursorStreamProvider provider) {
            try (java.io.InputStream is = provider.openCursor()) {
                return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                LOGGER.error("Failed to read CursorStreamProvider: {}", e.getMessage());
                return null;
            }
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

    private boolean evaluateCustomExpression(String expression, String responseBody, PaginationState state) {
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

    private String buildFinalPayload(PaginationConfig config, List<Object> allRecords,
                                     List<Object> allPages, PaginationState state) {
        try {
            if (config.isIncludePaginationState()) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("pagesFetched", state.getPagesFetched());
                result.put("totalRecordsFetched", state.getTotalRecordsFetched());
                result.put("totalRecordsReported", state.getTotalRecords());
                result.put("stopReason", state.getStopReason());
                result.put("strategy", config.getStrategy().name());
                result.put("records", config.isFlattenPages() ? allRecords : allPages);
                return MAPPER.writeValueAsString(result);
            }
            return MAPPER.writeValueAsString(config.isFlattenPages() ? allRecords : allPages);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize final payload to JSON: {}", e.getMessage());
            return "[]";
        }
    }
}