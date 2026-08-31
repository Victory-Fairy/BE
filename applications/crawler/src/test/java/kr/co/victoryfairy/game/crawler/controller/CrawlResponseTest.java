package kr.co.victoryfairy.game.crawler.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CrawlResponseTest {

    @Test
    void preservesTheExistingCompletionResponse() {
        CrawlResponse response = CrawlResponse.completed();

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.message()).isEqualTo("요청이 완료되었습니다.");
    }
}
