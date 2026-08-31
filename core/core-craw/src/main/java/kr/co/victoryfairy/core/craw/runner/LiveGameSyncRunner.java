package kr.co.victoryfairy.core.craw.runner;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import kr.co.victoryfairy.core.craw.service.LiveGameSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "live-game", name = "enabled", havingValue = "true")
public class LiveGameSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LiveGameSyncRunner.class);
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter SYSTEMD_TIME = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss 'Asia/Seoul'");
    private static final String NEXT_RUN_MARKER = "LIVE_GAME_NEXT_AT=";
    private final LiveGameSyncService liveGameSyncService;
    private final String action;

    public LiveGameSyncRunner(LiveGameSyncService liveGameSyncService,
            @Value("${live-game.action:sync}") String action) {
        this.liveGameSyncService = liveGameSyncService;
        this.action = action;
    }

    @Override
    public void run(ApplicationArguments args) {
        var startedAt = LocalDateTime.now(KOREA);
        var gameDate = startedAt.toLocalDate();
        var notBefore = startedAt;

        if ("sync".equals(action)) {
            log.info("Live game sync completed: {} matches", liveGameSyncService.sync(gameDate));
            notBefore = startedAt.withSecond(0).withNano(0).plusMinutes(10);
        }
        else if (!"plan".equals(action)) {
            throw new IllegalArgumentException("Unsupported live-game action: " + action);
        }

        var now = LocalDateTime.now(KOREA);
        var nextRun = liveGameSyncService.nextExecutionAt(gameDate, now, notBefore);
        System.out.println(NEXT_RUN_MARKER + nextRun.map(SYSTEMD_TIME::format).orElse("NONE"));
    }

}
