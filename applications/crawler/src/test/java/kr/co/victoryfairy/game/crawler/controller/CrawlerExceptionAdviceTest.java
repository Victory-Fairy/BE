package kr.co.victoryfairy.game.crawler.controller;

import static org.assertj.core.api.Assertions.assertThat;

import kr.co.victoryfairy.game.crawler.service.CrawlRequestException;
import org.junit.jupiter.api.Test;

class CrawlerExceptionAdviceTest {

    private final CrawlerExceptionAdvice advice = new CrawlerExceptionAdvice();

    @Test
    void preservesBadRequestShapeForMissingCrawlerData() {
        var response = advice.handle(new CrawlRequestException("해당 데이터가 존재하지 않습니다."));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(
                CrawlResponse.failed(400, "해당 데이터가 존재하지 않습니다.", null));
    }

    @Test
    void preservesGenericFailureShape() {
        var response = advice.handle(new IllegalStateException("crawl failed"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isEqualTo(
                CrawlResponse.failed(500, "요청이 실패 하였습니다.", "crawl failed"));
    }
}
