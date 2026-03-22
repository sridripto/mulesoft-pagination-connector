package com.github.pagination.connector.internal.config;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class PageBasedConfig {

    @Parameter
    @Optional(defaultValue = "1")
    @DisplayName("Start Page")
    @Summary("First page number to request. Use 1 for 1-based APIs, 0 for 0-based APIs.")
    @Example("1")
    @Placement(tab = "Request", order = 1)
    private int startPage;

    @Parameter
    @Optional(defaultValue = "10")
    @DisplayName("Records Per Page")
    @Summary("How many records to fetch per request.")
    @Example("10")
    @Placement(tab = "Request", order = 2)
    private int pageSize;

    @Parameter
    @Optional(defaultValue = "page")
    @DisplayName("Page Number queryParam Name")
    @Summary("Query param name for the page number. Examples: page, pageNo, p.")
    @Example("page")
    @Placement(tab = "Request", order = 3)
    private String pageParamName;

    @Parameter
    @Optional(defaultValue = "size")
    @DisplayName("Page Size queryParam Name")
    @Summary("Query param name for page size. Examples: size, pageSize, per_page.")
    @Example("size")
    @Placement(tab = "Request", order = 4)
    private String pageSizeParamName;

    @Parameter
    @Optional(defaultValue = "1000")
    @DisplayName("Total Iterations Allowed")
    @Summary("Safety limit. Stops after this many pages no matter what. Set to -1 to disable.")
    @Example("1000")
    @Placement(tab = "Request", order = 5)
    private int maxPages;

    @Parameter
    @Optional
    @DisplayName("Path to Array of Records")
    @Summary("Dot-path to the records array in the response. Leave blank if the response itself is the array. Example: data, items, result.records")
    @Example("data")
    @Placement(tab = "Response", order = 1)
    private String dataFieldPath;

    @Parameter
    @Optional
    @DisplayName("Path to Total Records Count")
    @Summary("Dot-path to the total record count in the response. Used to stop when all records are fetched. Example: total, field.total. Leave blank if the API does not return a total.")
    @Example("total")
    @Placement(tab = "Response", order = 2)
    private String totalCountFieldPath;

    @Parameter
    @Optional
    @DisplayName("Stop Condition")
    @Summary("Stop pagination when this condition is true. Example: payload.hasMore == false")
    @Example("payload.hasMore == false")
    @Placement(tab = "Response", order = 3)
    private String customStopExpression;

    @Parameter
    @Optional(defaultValue = "true")
    @DisplayName("Flatten Output")
    @Summary("true: one flat list of all records. false: list of raw page responses.")
    @Placement(tab = "Response", order = 4)
    private boolean flattenPages;

    @Parameter
    @Optional(defaultValue = "false")
    @DisplayName("Include Stats")
    @Summary("If true, wraps output with pages fetched, total records, and stop reason.")
    @Placement(tab = "Response", order = 5)
    private boolean includePaginationState;

    public int getStartPage() { return startPage; }
    public int getPageSize() { return pageSize; }
    public String getPageParamName() { return pageParamName; }
    public String getPageSizeParamName() { return pageSizeParamName; }
    public int getMaxPages() { return maxPages; }
    public String getDataFieldPath() { return dataFieldPath; }
    public String getTotalCountFieldPath() { return totalCountFieldPath; }
    public String getCustomStopExpression() { return customStopExpression; }
    public boolean isFlattenPages() { return flattenPages; }
    public boolean isIncludePaginationState() { return includePaginationState; }
}