package com.nh.shorturl.dto.response.common;

import org.springframework.data.domain.PageImpl;

import java.io.Serializable;
import java.util.Collection;

public class ResultList<T> implements Serializable {
    private static final long serialVersionUID = -8542432002971006591L;
    private long totalCount;
    private Collection<T> elements;

    public ResultList() {
    }

    public ResultList(long totalCount, Collection<T> elements) {
        this.totalCount = totalCount;
        this.elements = elements;
    }
    public ResultList(Collection<T> elements) {
        this.totalCount = elements.size();
        this.elements = elements;
    }

    public ResultList(PageImpl<T> page) {
        this.totalCount = page.getTotalElements();
        this.elements = page.getContent();
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public Collection<T> getElements() {
        return elements;
    }

    public void setElements(Collection<T> elements) {
        this.elements = elements;
    }
}
