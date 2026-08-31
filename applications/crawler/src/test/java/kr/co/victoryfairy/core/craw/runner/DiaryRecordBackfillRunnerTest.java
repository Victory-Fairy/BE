package kr.co.victoryfairy.core.craw.runner;

import static org.mockito.Mockito.verify;

import kr.co.victoryfairy.common.service.GameRecordDomainService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.ApplicationArguments;

class DiaryRecordBackfillRunnerTest {

    @Test
    void recoversEveryTerminalUnratedDiary() throws Exception {
        var gameRecordService = Mockito.mock(GameRecordDomainService.class);

        new DiaryRecordBackfillRunner(gameRecordService).run(Mockito.mock(ApplicationArguments.class));

        verify(gameRecordService).recoverAllTerminal();
    }

}
