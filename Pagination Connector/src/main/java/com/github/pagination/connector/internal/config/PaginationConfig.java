package com.github.pagination.connector.internal.config;

import com.github.pagination.connector.internal.model.PaginationStrategy;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Example;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

public class PaginationConfig {

    @Parameter
    @DisplayName("Pagination Strategy")
    @Summary("PAGE_BASED: uses page+size | OFFSET_BASED: uses offset+limit | CURSOR_BASED: uses cursor token | LINK_HEADER: follows Link response header")
    @Placement(tab = "Request", order = 1)
    private PaginationStrategy strategy;

    @Parameter
    @Optional(defaultValue = "10")
    @DisplayName("Records Per Page")
    @Summary("How many records to request per page. This is the numeric value sent to the API (e.g. 10, 50, 100).")
    @Example("10")
    @Placement(tab = "Request", order = 2)
    private int pageSize;

    @Parameter
    @Optional(defaultValue = "0")
    @DisplayName("Start From")
    @Summary("Where to begin on the first request. PAGE_BASED: first page number (e.g. 1 or 0). OFFSET_BASED: starting record position (e.g. 0). CURSOR_BASED/LINK_HEADER: leave as 0.")
    @Example("0")
    @Placement(tab = "Request", order = 3)
    private long startValue;

    @Parameter
    @Optional(defaultValue = "page")
    @DisplayName("Page/Offset/Cursor Param Name")
    @Summary("The query parameter name your API uses for navigation. PAGE_BASED: the page number param (e.g. page, pageNo, p). OFFSET_BASED: the offset param (e.g. offset, skip, start). CURSOR_BASED: the cursor param (e.g. cursor, after, next_token). LINK_HEADER: not used.")
    @Example("page")
    @Placement(tab = "Request", order = 4)
    private String firstParamName;

    @Parameter
    @Optional(defaultValue = "size")
    @DisplayName("Page Size Param Name (API Key)")
    @Summary("The query parameter name your API uses for the page size / record limit. This is the name (key), not the value. PAGE_BASED: e.g. size, pageSize, per_page. OFFSET_BASED/CURSOR_BASED: e.g. limit, count, per_page. LINK_HEADER: not used.")
    @Example("size")
    @Placement(tab = "Request", order = 5)
    private String secondParamName;

    @Parameter
    @Optional(defaultValue = "1000")
    @DisplayName("Max Pages")
    @Summary("Safety limit. Stops pagination after this many pages regardless of other conditions. Prevents infinite loops. Set to -1 to disable.")
    @Example("1000")
    @Placement(tab = "Request", order = 6)
    private int maxPages;

    @Parameter
    @Optional
    @DisplayName("Response Records Path")
    @Summary("Dot-path to the records array inside the response body. Example: response is {data: [...], total: 100} then use 'data'. Nested: {result: {items: [...]}} then use 'result.items'. Leave blank if the response body itself is the array.")
    @Example("data")
    @Placement(tab = "Response", order = 1)
    private String dataFieldPath;

    @Parameter
    @Optional
    @DisplayName("Total Records Path")
    @Summary("Dot-path to the total record count in the response. Used to stop when all records are fetched. Example: response is {data: [...], total: 500} then use 'total'. Nested: {meta: {total: 500}} then use 'meta.total'. Leave blank if the API does not return a total.")
    @Example("total")
    @Placement(tab = "Response", order = 2)
    private String totalCountFieldPath;

    @Parameter
    @Optional
    @DisplayName("Next Page Token Path")
    @Summary("CURSOR_BASED only. Dot-path to the next page token in the response body. Example: response is {data: [...], next_cursor: 'abc123'} then use 'next_cursor'. Pagination stops automatically when this field is null or missing.")
    @Example("next_cursor")
    @Placement(tab = "Response", order = 3)
    private String cursorResponseFieldPath;

    @Parameter
    @Optional(defaultValue = "Link")
    @DisplayName("Next Page Header")
    @Summary("LINK_HEADER only. The HTTP response header that contains the next page URL. Almost always 'Link'. Pagination stops automatically when this header is absent from the response.")
    @Example("Link")
    @Placement(tab = "Response", order = 4)
    private String linkHeaderName;

    @Parameter
    @Optional
    @DisplayName("Stop When")
    @Summary("Stop pagination when this field condition is true. Use dot-notation on payload. Examples: payload.hasMore == false | payload.isLastPage == true | payload.nextPage == null. Leave blank to rely on other stop conditions only.")
    @Example("payload.hasMore == false")
    @Placement(tab = "Response", order = 5)
    private String customStopExpression;

    @Parameter
    @Optional(defaultValue = "true")
    @DisplayName("Flatten Output")
    @Summary("true: output is one flat list of all records combined from every page (recommended). false: output is a list where each entry is the raw response body of one page.")
    @Placement(tab = "Response", order = 6)
    private boolean flattenPages;

    @Parameter
    @Optional(defaultValue = "false")
    @DisplayName("Include Stats")
    @Summary("If true, wraps the output with pagination stats: how many pages were fetched, total records collected, and why pagination stopped.")
    @Placement(tab = "Response", order = 7)
    private boolean includePaginationState;

    public PaginationStrategy getStrategy() { return strategy; }
    public int getPageSize() { return pageSize; }
    public int getStartPage() { return (int) startValue; }
    public long getStartOffset() { return startValue; }
    public String getPageParamName() { return firstParamName; }
    public String getPageSizeParamName() { return secondParamName; }
    public String getOffsetParamName() { return firstParamName; }
    public String getLimitParamName() { return secondParamName; }
    public String getCursorRequestParamName() { return firstParamName; }
    public String getDataFieldPath() { return dataFieldPath; }
    public String getTotalCountFieldPath() { return totalCountFieldPath; }
    public String getCursorResponseFieldPath() { return cursorResponseFieldPath; }
    public String getLinkHeaderName() { return linkHeaderName; }
    public int getMaxPages() { return maxPages; }
    public long getMaxTotalRecords() { return -1; }
    public String getCustomStopExpression() { return customStopExpression; }
    public boolean isFailOnEmptyFirstPage() { return false; }
    public boolean isIncludePaginationState() { return includePaginationState; }
    public boolean isFlattenPages() { return flattenPages; }
}