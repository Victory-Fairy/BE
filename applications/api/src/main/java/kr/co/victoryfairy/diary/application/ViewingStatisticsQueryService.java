package kr.co.victoryfairy.diary.application;

import java.time.LocalDate;
import kr.co.victoryfairy.diary.domain.GameRecordStore;
import kr.co.victoryfairy.diary.domain.ViewingRecordReader;
import kr.co.victoryfairy.diary.domain.ViewingStatistics;
import kr.co.victoryfairy.diary.presentation.ViewingStatisticsDomain;
import kr.co.victoryfairy.member.domain.MemberStore;
import kr.co.victoryfairy.member.infrastructure.security.CurrentRequest;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.MessageEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ViewingStatisticsQueryService {
    private final MemberStore members;
    private final GameRecordStore gameRecords;
    private final ViewingRecordReader viewingRecords;

    public ViewingStatisticsDomain.VictoryPowerResponse findVictoryPower(String season) {
        var id = CurrentRequest.getId();
        if (id == null) return new ViewingStatisticsDomain.VictoryPowerResponse(null, null);
        var member = members.findMember(id).orElseThrow(() -> new CustomException(MessageEnum.Data.FAIL_NO_RESULT));
        var result = ViewingStatistics.power(viewingRecords.findByMemberAndSeason(member.id(), season(season)));
        return new ViewingStatisticsDomain.VictoryPowerResponse(result.level(), result.power());
    }

    public ViewingStatisticsDomain.ReportResponse findReport(String season) {
        var id = CurrentRequest.getId();
        if (id == null) throw new CustomException(MessageEnum.Auth.FAIL_EXPIRE_AUTH);
        var records = gameRecords.findByMemberAndSeasonOrdered(id, season(season));
        if (records.isEmpty()) {
            if (!members.memberExists(id)) throw new CustomException(MessageEnum.Data.FAIL_NO_RESULT);
            return new ViewingStatisticsDomain.ReportResponse(null, null, null);
        }
        var report = ViewingStatistics.report(records);
        return new ViewingStatisticsDomain.ReportResponse(view(report.stadium()), view(report.home()),
                new ViewingStatisticsDomain.ViewStatisticsDto(report.statistics().winTeam(), report.statistics().lossTeam(),
                        report.statistics().stadium(), report.statistics().winningStreak(),
                        report.statistics().homeWinAvg(), report.statistics().stadiumWinAvg()));
    }

    private static ViewingStatisticsDomain.ViewTypeDto view(ViewingStatistics.ViewType value) {
        return value == null ? null : new ViewingStatisticsDomain.ViewTypeDto(value.winAvg(), value.win(), value.lose(),
                value.draw(), value.cancel());
    }

    private static String season(String value) {
        return StringUtils.hasText(value) ? value : String.valueOf(LocalDate.now().getYear());
    }
}
