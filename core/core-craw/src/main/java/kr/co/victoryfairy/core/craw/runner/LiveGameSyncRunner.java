package kr.co.victoryfairy.core.craw.runner;

import kr.co.victoryfairy.core.craw.service.LiveGameSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "live-game", name = "enabled", havingValue = "true")
public class LiveGameSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LiveGameSyncRunner.class);
    private final LiveGameSyncService liveGameSyncService;

    public LiveGameSyncRunner(LiveGameSyncService liveGameSyncService) {
        this.liveGameSyncService = liveGameSyncService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Live game sync completed: {} matches", liveGameSyncService.sync());
    }

}
