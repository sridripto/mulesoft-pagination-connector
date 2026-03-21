package com.github.pagination.connector.internal.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class PaginationState {

    private int currentPage;
    private long currentOffset;
    private int pagesFetched;
    private long totalRecordsFetched;
    private Long totalRecords;

    private String nextCursor;
    private String nextUrl;

    private final List<Object> collectedPages;

    private boolean done;
    private String stopReason;

    private Map<String, String> nextRequestParams;

    public PaginationState() {
        this.currentPage = 1;
        this.currentOffset = 0;
        this.pagesFetched = 0;
        this.totalRecordsFetched = 0;
        this.totalRecords = null;
        this.nextCursor = null;
        this.nextUrl = null;
        this.collectedPages = new ArrayList<>();
        this.done = false;
        this.stopReason = null;
        this.nextRequestParams = Collections.emptyMap();
    }

    public int getCurrentPage() { return currentPage; }
    public long getCurrentOffset() { return currentOffset; }
    public int getPagesFetched() { return pagesFetched; }
    public long getTotalRecordsFetched() { return totalRecordsFetched; }
    public Long getTotalRecords() { return totalRecords; }
    public String getNextCursor() { return nextCursor; }
    public String getNextUrl() { return nextUrl; }
    public List<Object> getCollectedPages() { return Collections.unmodifiableList(collectedPages); }
    public boolean isDone() { return done; }
    public String getStopReason() { return stopReason; }
    public Map<String, String> getNextRequestParams() { return nextRequestParams; }

    public void incrementPage() { this.currentPage++; }
    public void incrementOffset(int pageSize) { this.currentOffset += pageSize; }
    public void incrementPagesFetched() { this.pagesFetched++; }
    public void addRecordsFetched(long count) { this.totalRecordsFetched += count; }
    public void setTotalRecords(Long totalRecords) { this.totalRecords = totalRecords; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
    public void setNextUrl(String nextUrl) { this.nextUrl = nextUrl; }
    public void setNextRequestParams(Map<String, String> params) { this.nextRequestParams = params; }

    public void addPage(Object pagePayload) {
        this.collectedPages.add(pagePayload);
    }

    public void markDone(String reason) {
        this.done = true;
        this.stopReason = reason;
    }

    @Override
    public String toString() {
        return "PaginationState{" +
                "currentPage=" + currentPage +
                ", currentOffset=" + currentOffset +
                ", pagesFetched=" + pagesFetched +
                ", totalRecordsFetched=" + totalRecordsFetched +
                ", totalRecords=" + totalRecords +
                ", nextCursor='" + nextCursor + '\'' +
                ", nextUrl='" + nextUrl + '\'' +
                ", done=" + done +
                ", stopReason='" + stopReason + '\'' +
                '}';
    }
}
