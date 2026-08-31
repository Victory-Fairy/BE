package kr.co.victoryfairy.core.craw.runner;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.common.service.GameRecordDomainService;
import kr.co.victoryfairy.core.craw.service.KboGameCrawler;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.storage.db.core.entity.GameMatchEntity;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchCustomRepository;
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
        GameMatchCustomRepository customRepository = Mockito.mock(GameMatchCustomRepository.class);
        GameRecordDomainService diaryRecovery = Mockito.mock(GameRecordDomainService.class);
        RedisHandler redis = Mockito.mock(RedisHandler.class);
        GameMatchEntity ended = GameMatchEntity.builder()
            .id("20260819KTLG0")
            .status(MatchEnum.MatchStatus.END)
            .isMatchInfoCraw(false)
            .build();
        GameMatchEntity canceled = GameMatchEntity.builder()
            .id("20260819WOSK0")
            .status(MatchEnum.MatchStatus.CANCELED)
            .isMatchInfoCraw(false)
            .build();
        LocalDate date = LocalDate.of(2026, 8, 19);
        when(customRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO)).thenReturn(List.of(ended, canceled));
        new GameDataRecoveryRunner("2026-08-19", "2026-08-19", 0, false, crawler, customRepository, diaryRecovery,
                redis)
            .run(Mockito.mock(ApplicationArguments.class));

        verify(crawler).crawMatchListByMonth("2026", "08");
        verify(crawler).crawMatchDetailById(ended.getId());
        verify(crawler, never()).crawMatchDetailById(canceled.getId());
        verify(diaryRecovery).recover(ended);
        verify(diaryRecovery).recover(canceled);
        verify(redis).deleteHash("20260819_match_list");
    }

    @Test
    void dryRunDoesNotChangeAnything() throws Exception {
        KboGameCrawler crawler = Mockito.mock(KboGameCrawler.class);
        GameMatchCustomRepository customRepository = Mockito.mock(GameMatchCustomRepository.class);
        GameRecordDomainService diaryRecovery = Mockito.mock(GameRecordDomainService.class);
        RedisHandler redis = Mockito.mock(RedisHandler.class);

        new GameDataRecoveryRunner("2026-08-19", "2026-08-27", 0, true, crawler, customRepository, diaryRecovery,
                redis)
            .run(Mockito.mock(ApplicationArguments.class));

        verify(crawler, never()).crawMatchListByMonth(Mockito.anyString(), Mockito.anyString());
        verify(redis, never()).deleteHash(Mockito.anyString());
    }

}
