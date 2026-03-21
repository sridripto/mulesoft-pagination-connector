package com.github.pagination.connector.internal.model;

import java.util.List;
import java.util.Map;


public class PaginationResult {

    private final List<Object> records;

    private final int pagesFetched;

    private final long totalRecordsFetched;

    private final Long totalRecordsReported;

    private final String stopReason;

    private final boolean truncated;

    private final String strategy;

    private final Map<String, String> lastRequestParams;

    public PaginationResult(List<Object> records,
                            int pagesFetched,
                            long totalRecordsFetched,
                            Long totalRecordsReported,
                            String stopReason,
                            String strategy,
                            Map<String, String> lastRequestParams) {
        this.records = records;
        this.pagesFetched = pagesFetched;
        this.totalRecordsFetched = totalRecordsFetched;
        this.totalRecordsReported = totalRecordsReported;
        this.stopReason = stopReason;
        this.truncated = stopReason != null &&
                (stopReason.startsWith("MAX_PAGES") || stopReason.startsWith("MAX_RECORDS"));
        this.strategy = strategy;
        this.lastRequestParams = lastRequestParams;
    }

    public List<Object> getRecords() { return records; }
    public int getPagesFetched() { return pagesFetched; }
    public long getTotalRecordsFetched() { return totalRecordsFetched; }
    public Long getTotalRecordsReported() { return totalRecordsReported; }
    public String getStopReason() { return stopReason; }
    public boolean isTruncated() { return truncated; }
    public String getStrategy() { return strategy; }
    public Map<String, String> getLastRequestParams() { return lastRequestParams; }
}
