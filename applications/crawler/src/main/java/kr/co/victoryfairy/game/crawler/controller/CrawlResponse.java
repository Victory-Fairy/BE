package kr.co.victoryfairy.game.crawler.controller;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CrawlResponse(int status, String errorMsg, String message) {

    static CrawlResponse completed() {
        return new CrawlResponse(200, null, "요청이 완료되었습니다.");
    }

    static CrawlResponse failed(int status, String message, String errorMsg) {
        return new CrawlResponse(status, errorMsg, message);
    }
}
