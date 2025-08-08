package kr.co.victoryfairy.support.model;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResult<T> {

    private final List<T> contents;
    private final long total;

    public PageResult(List<T> content, long total) {
        this.contents = content;
        this.total = total;
    }
}