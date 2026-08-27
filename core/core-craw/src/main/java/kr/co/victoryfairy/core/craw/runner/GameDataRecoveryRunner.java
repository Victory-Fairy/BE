package kr.co.victoryfairy.core.craw.runner;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.core.craw.service.CrawService;
import kr.co.victoryfairy.core.craw.service.DiaryResultRecoveryService;
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

    private final LocalDate from;

    private final LocalDate to;

    private final boolean dryRun;

    private final CrawService crawService;

    private final GameMatchCustomRepository gameMatchCustomRepository;

    private final DiaryResultRecoveryService diaryResultRecoveryService;

    private final RedisHandler redisHandler;

    public GameDataRecoveryRunner(@Value("${game-recovery.from}") String from,
            @Value("${game-recovery.to}") String to,
            @Value("${game-recovery.dry-run:true}") boolean dryRun,
            @Qualifier("crawServiceImpl") CrawService crawService,
            GameMatchCustomRepository gameMatchCustomRepository,
            DiaryResultRecoveryService diaryResultRecoveryService, RedisHandler redisHandler) {
        this.from = LocalDate.parse(from);
        this.to = LocalDate.parse(to);
        if (this.from.isAfter(this.to)) {
            throw new IllegalArgumentException("game-recovery.from must not be after game-recovery.to");
        }
        this.dryRun = dryRun;
        this.crawService = crawService;
        this.gameMatchCustomRepository = gameMatchCustomRepository;
        this.diaryResultRecoveryService = diaryResultRecoveryService;
        this.redisHandler = redisHandler;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Game data recovery {} ~ {}, dryRun={}", from, to, dryRun);
        if (dryRun) {
            return;
        }

        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            crawService.crawMatchListByMonth(String.valueOf(month.getYear()), String.format("%02d", month.getMonthValue()));
        }

        int details = 0;
        int diaryRecords = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            for (var match : gameMatchCustomRepository.findByMatchAt(date, MatchEnum.LeagueType.KBO)) {
                if (match.getStatus() != MatchEnum.MatchStatus.END) {
                    continue;
                }
                if (!Boolean.TRUE.equals(match.getIsMatchInfoCraw())) {
                    crawService.crawMatchDetailById(match.getId());
                    details++;
                }
                diaryRecords += diaryResultRecoveryService.recover(match.getId());
            }
            redisHandler.deleteHash(date.format(CACHE_DATE) + "_match_list");
        }
        log.info("Game data recovery completed: details={}, diaryRecords={}", details, diaryRecords);
    }

}
