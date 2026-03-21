package com.github.pagination.connector.internal.model;

public enum StopCondition {
    EMPTY_RESPONSE,
    TOTAL_COUNT,
    NULL_CURSOR,
    NO_NEXT_LINK,
    CUSTOM_EXPRESSION,
    MAX_PAGES
}
