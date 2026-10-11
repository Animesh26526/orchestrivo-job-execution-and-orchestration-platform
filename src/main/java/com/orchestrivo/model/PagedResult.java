package com.orchestrivo.model;

import java.util.Collections;
import java.util.List;

public class PagedResult<T> {
    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;

    public PagedResult(List<T> items, int page, int pageSize, int totalCount) {
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
        this.page = page;
        this.pageSize = pageSize;
        this.totalCount = totalCount;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalCount / pageSize) : 0;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getTotalItems() {
        return totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasPrevious() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < totalPages;
    }
}
