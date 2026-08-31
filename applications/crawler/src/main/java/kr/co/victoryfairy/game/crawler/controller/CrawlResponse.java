package kr.co.victoryfairy.game.crawler.controller;

public record CrawlResponse(int status, String message) {

    static CrawlResponse completed() {
        return new CrawlResponse(200, "요청이 완료되었습니다.");
    }
}
