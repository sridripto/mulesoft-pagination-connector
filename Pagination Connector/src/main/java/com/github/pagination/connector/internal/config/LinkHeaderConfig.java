package com.github.pagination.connector.internal.config;

import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class LinkHeaderConfig {

    @Parameter
    @Optional(defaultValue = "Link")
    @DisplayName("Link Header Name")
    @Summary("HTTP response header containing the next page URL. Almost always 'Link'. Pagination stops when this header is absent.")
    @Example("Link")
    @Placement(tab = "Request", order = 1)
    private String linkHeaderName;

    @Parameter
    @Optional(defaultValue = "1000")
    @DisplayName("Total Iterations Allowed")
    @Summary("Safety limit. Stops after this many pages no matter what. Set to -1 to disable.")
    @Example("1000")
    @Placement(tab = "Request", order = 2)
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
    @Summary("Dot-path to the total record count in the response. Leave blank if the API does not return a total.")
    @Example("total")
    @Placement(tab = "Response", order = 2)
    private String totalCountFieldPath;

    @Parameter
    @Optional
    @DisplayName("Stop Condition Field")
    @Summary("Dot-path to a boolean field in the payload that signals when to stop. Set this field to true inside the scope using a Transform Message to stop pagination. Example: stopPagination")
    @Example("stopPagination")
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

    @Parameter
    @Optional(defaultValue = "true")
    @DisplayName("Aggregate Results")
    @Summary("true: Collect all records across pages into one output. false: process each page inside the scope individually - output after scope will be a stats summary only.")
    @Placement(tab = "Response", order = 6)
    private boolean aggregate;

    public String getLinkHeaderName() { return linkHeaderName; }
    public int getMaxPages() { return maxPages; }
    public String getDataFieldPath() { return dataFieldPath; }
    public String getTotalCountFieldPath() { return totalCountFieldPath; }
    public String getCustomStopExpression() { return customStopExpression; }
    public boolean isFlattenPages() { return flattenPages; }
    public boolean isIncludePaginationState() { return includePaginationState; }
    public boolean isAggregate() { return aggregate; }
}