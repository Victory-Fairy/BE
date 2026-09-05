package kr.co.victoryfairy.shared.domain;

import java.util.List;

public record PageResult<T>(List<T> contents, long total) {

    public List<T> getContents() {
        return contents;
    }

    public long getTotal() {
        return total;
    }
}
