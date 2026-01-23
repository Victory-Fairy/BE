package kr.co.victoryfairy.core.batch;

import kr.co.victoryfairy.core.batch.service.BatchService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScoreJob {

    private final BatchService batchService;

    public ScoreJob(BatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${batch.check.score}", zone = "Asia/Seoul")
    public void checkScore() {
        batchService.batchScore();
    }

    // @Scheduled(cron = "${batch.check.info}", zone = "Asia/Seoul")
    public void checkInfo() {
        batchService.batchMatchInfo();
    }

    // @Scheduled(fixedDelay = 60000)
    public void checkEvent() {
        batchService.checkEvent();
    }

    // @Scheduled(cron = "${batch.check.match}", zone = "Asia/Seoul")
    public void checkMatchList() {
        batchService.checkMatchList();
    }

    // @Scheduled(cron = "${batch.check.file}", zone = "Asia/Seoul")
    public void checkMissFile() {
        batchService.checkMissFile();
    }

}
