package kr.co.victoryfairy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.game.crawler.controller.CrawController;
import kr.co.victoryfairy.game.crawler.controller.WbcCrawController;
import kr.co.victoryfairy.game.crawler.service.KboGameCrawler;
import kr.co.victoryfairy.game.crawler.service.KboLiveGameCrawler;
import kr.co.victoryfairy.game.crawler.service.LiveGameSyncService;
import kr.co.victoryfairy.game.crawler.service.MatchScheduleSyncService;
import kr.co.victoryfairy.game.crawler.service.WbcGameCrawler;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.game.domain.GameRecordRepository;
import kr.co.victoryfairy.game.domain.StadiumReader;
import kr.co.victoryfairy.game.domain.TeamReader;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

class CrawlerContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(CrawlerComponents.class, Collaborators.class);

    @Test
    void wiresCrawlerComponentsWithoutRunningCrawls() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(KboGameCrawler.class)
                .hasSingleBean(KboLiveGameCrawler.class)
                .hasSingleBean(LiveGameSyncService.class)
                .hasSingleBean(MatchScheduleSyncService.class)
                .hasSingleBean(WbcGameCrawler.class)
                .hasSingleBean(CrawController.class)
                .hasSingleBean(WbcCrawController.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("kr.co.victoryfairy.game.crawler")
    static class CrawlerComponents {
    }

    @Configuration(proxyBeanMethods = false)
    static class Collaborators {

        @Bean
        TeamReader teamReader() {
            return mock(TeamReader.class);
        }

        @Bean
        StadiumReader stadiumReader() {
            return mock(StadiumReader.class);
        }

        @Bean
        GameMatchRepository gameMatchRepository() {
            return mock(GameMatchRepository.class);
        }

        @Bean
        GameRecordRepository gameRecordRepository() {
            return mock(GameRecordRepository.class);
        }

        @Bean
        GameRecordDomainService gameRecordDomainService() {
            return mock(GameRecordDomainService.class);
        }

        @Bean
        RedisHandler redisHandler() {
            return mock(RedisHandler.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

    }

}
