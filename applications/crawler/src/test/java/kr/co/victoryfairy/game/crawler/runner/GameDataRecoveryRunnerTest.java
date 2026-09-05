package kr.co.victoryfairy.game.crawler.runner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.game.domain.GameMatch;
import kr.co.victoryfairy.game.domain.GameMatchRepository;
import kr.co.victoryfairy.diary.application.GameRecordDomainService;
import kr.co.victoryfairy.game.crawler.service.KboGameCrawler;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.ApplicationArguments;

class GameDataRecoveryRunnerTest {

    @Test
    void resolvesRelativeRecoveryDateInKoreanCalendar() {
        var range = GameDataRecoveryRunner.resolveDateRange("", "", 1, LocalDate.of(2026, 8, 28));

        assertThat(range.from()).isEqualTo(LocalDate.of(2026, 8, 27));
        assertThat(range.to()).isEqualTo(LocalDate.of(2026, 8, 27));
    }

    @Test
    void recoversOnlyMissingEndedMatchDetailsAndClearsDailyCache() throws Exception {
        KboGameCrawler crawler = Mockito.mock(KboGameCrawler.class);
        GameMatchRepository customRepository = Mockito.mock(GameMatchRepository.class);
        GameRecordDomainService diaryRecovery = Mockito.mock(GameRecordDomainService.class);
        RedisHandler redis = Mockito.mock(RedisHandler.class);
        GameMatch ended = match("20260819KTLG0", MatchEnum.MatchStatus.END);
        GameMatch canceled = match("20260819WOSK0", MatchEnum.MatchStatus.CANCELED);
        LocalDate date = LocalDate.of(2026, 8, 19);
        when(customRepository.findByDate(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(ended, canceled));
        new GameDataRecoveryRunner("2026-08-19", "2026-08-19", 0, false, crawler, customRepository, diaryRecovery,
                redis)
            .run(Mockito.mock(ApplicationArguments.class));

        verify(crawler).crawMatchListByMonth("2026", "08");
        verify(crawler).crawMatchDetailById(ended.getId());
        verify(crawler, never()).crawMatchDetailById(canceled.getId());
        verify(diaryRecovery).recover(ended.id());
        verify(diaryRecovery).recover(canceled.id());
        verify(redis).deleteHash("20260819_match_list");
    }

    @Test
    void dryRunDoesNotChangeAnything() throws Exception {
        KboGameCrawler crawler = Mockito.mock(KboGameCrawler.class);
        GameMatchRepository customRepository = Mockito.mock(GameMatchRepository.class);
        GameRecordDomainService diaryRecovery = Mockito.mock(GameRecordDomainService.class);
        RedisHandler redis = Mockito.mock(RedisHandler.class);

        new GameDataRecoveryRunner("2026-08-19", "2026-08-27", 0, true, crawler, customRepository, diaryRecovery, redis)
            .run(Mockito.mock(ApplicationArguments.class));

        verify(crawler, never()).crawMatchListByMonth(Mockito.anyString(), Mockito.anyString());
        verify(redis, never()).deleteHash(Mockito.anyString());
    }

    private GameMatch match(String id, MatchEnum.MatchStatus status) {
        return new GameMatch(id, MatchEnum.LeagueType.KBO, null, null, "2026", null, null, null, null, null, null, null,
                null, status, null, false, false, true, null, null);
    }

}
