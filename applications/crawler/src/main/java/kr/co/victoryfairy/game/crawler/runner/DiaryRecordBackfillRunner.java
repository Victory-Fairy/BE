package kr.co.victoryfairy.game.crawler.runner;

import kr.co.victoryfairy.common.service.GameRecordDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "diary-record-backfill", name = "enabled", havingValue = "true")
public class DiaryRecordBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiaryRecordBackfillRunner.class);

    private final GameRecordDomainService gameRecordDomainService;

    public DiaryRecordBackfillRunner(GameRecordDomainService gameRecordDomainService) {
        this.gameRecordDomainService = gameRecordDomainService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int recovered = gameRecordDomainService.recoverAllTerminal();
        log.info("Diary game record backfill completed: recovered={}", recovered);
    }

}
