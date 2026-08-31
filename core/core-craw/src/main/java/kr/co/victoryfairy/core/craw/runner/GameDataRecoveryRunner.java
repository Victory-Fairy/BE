package kr.co.victoryfairy.core.craw.runner;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.common.service.GameRecordDomainService;
import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.redis.handler.RedisHandler;
import kr.co.victoryfairy.storage.db.core.repository.GameMatchCustomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "game-recovery", name = "enabled", havingValue = "true")
public class GameDataRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GameDataRecoveryRunner.class);

    private static final DateTimeFormatter CACHE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");

    private final LocalDate from;

    private final LocalDate to;

    private final boolean dryRun;

    private final CrawService crawService;

    private final GameMatchCustomRepository gameMatchCustomRepository;

    private final GameRecordDomainService gameRecordDomainService;

    private final RedisHandler redisHandler;

    public GameDataRecoveryRunner(@Value("${game-recovery.from:}") String from,
            @Value("${game-recovery.to:}") String to, @Value("${game-recovery.days-ago:0}") int daysAgo,
            @Value("${game-recovery.dry-run:true}") boolean dryRun,
            @Qualifier("crawServiceImpl") CrawService crawService, GameMatchCustomRepository gameMatchCustomRepository,
            GameRecordDomainService gameRecordDomainService, RedisHandler redisHandler) {
        var range = resolveDateRange(from, to, daysAgo, LocalDate.now(KOREA));
        this.from = range.from();
        this.to = range.to();
        if (this.from.isAfter(this.to)) {
            throw new IllegalArgumentException("game-recovery.from must not be after game-recovery.to");
        }
        this.dryRun = dryRun;
        this.crawService = crawService;
        this.gameMatchCustomRepository = gameMatchCustomRepository;
        this.gameRecordDomainService = gameRecordDomainService;
        this.redisHandler = redisHandler;
    }

    static DateRange resolveDateRange(String from, String to, int daysAgo, LocalDate today) {
        boolean hasFrom = from != null && !from.isBlank();
        boolean hasTo = to != null && !to.isBlank();
        if (hasFrom != hasTo) {
            throw new IllegalArgumentException("game-recovery.from and game-recovery.to must be supplied together");
        }
        if (hasFrom) {
            return new DateRange(LocalDate.parse(from), LocalDate.parse(to));
        }
        if (daysAgo < 0) {
            throw new IllegalArgumentException("game-recovery.days-ago must not be negative");
        }
        LocalDate date = today.minusDays(daysAgo);
        return new DateRange(date, date);
    }

    record DateRange(LocalDate from, LocalDate to) {
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Game data recovery {} ~ {}, dryRun={}", from, to, dryRun);
        if (dryRun) {
            return;
        }

        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            crawService.crawMatchListByMonth(String.valueOf(month.getYear()),
                    String.format("%02d", month.getMonthValue()));
        }

        int details = 0;
        int diaryRecords = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (var match : gameMatchCustomRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO)) {
                if (match.getStatus() != MatchEnum.MatchStatus.END
                        && match.getStatus() != MatchEnum.MatchStatus.CANCELED) {
                    continue;
                }
                if (match.getStatus() == MatchEnum.MatchStatus.END
                        && !Boolean.TRUE.equals(match.getIsMatchInfoCraw())) {
                    crawService.crawMatchDetailById(match.getId());
                    details++;
                }
                diaryRecords += gameRecordDomainService.recover(match);
            }
            redisHandler.deleteHash(date.format(CACHE_DATE) + "_match_list");
        }
        log.info("Game data recovery completed: details={}, diaryRecords={}", details, diaryRecords);
    }

}
