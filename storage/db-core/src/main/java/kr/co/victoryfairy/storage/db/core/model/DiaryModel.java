package kr.co.victoryfairy.storage.db.core.model;

import io.dodn.springboot.core.enums.MatchEnum;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface DiaryModel {

    record ListRequest(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    record DailyListRequest(
            Long memberId,
            LocalDate date
    ) {}

    record DiaryListRequest(
            LocalDate date,
            MatchEnum.MatchStatus status,
            Integer page,
            Integer size
    ) {}

    @Getter
    class DiaryDto {
        Long id;
        Long teamId;
        String content;

        LocalDateTime matchAt;
        MatchEnum.ResultType resultType;

        String shortName;
        String fullName;

        MatchEnum.MatchStatus matchStatus;

        Long awayTeamId;
        String awayTeamName;
        Short awayScore;

        Long homeTeamId;
        String homeTeamName;
        Short homeScore;

        LocalDateTime createdAt;
        LocalDateTime updatedAt;


        public DiaryDto() {}
        public DiaryDto(Long id, LocalDateTime matchAt, MatchEnum.ResultType resultType) {
            this.id = id;
            this.matchAt = matchAt;
            this.resultType = resultType;
        }

        public DiaryDto(Long id, Long teamId, String content, LocalDateTime matchAt, MatchEnum.ResultType resultType,
                        String shortName, String fullName, MatchEnum.MatchStatus matchStatus,
                        Long awayTeamId, String awayTeamName, Short awayScore,
                        Long homeTeamId, String homeTeamName, Short homeScore,
                        LocalDateTime createdAt) {
            this.id = id;
            this.teamId = teamId;
            this.content = content;
            this.matchAt = matchAt;
            this.resultType = resultType;
            this.shortName = shortName;
            this.fullName = fullName;
            this.matchStatus = matchStatus;
            this.awayTeamId = awayTeamId;
            this.awayTeamName = awayTeamName;
            this.awayScore = awayScore;
            this.homeTeamId = homeTeamId;
            this.homeTeamName = homeTeamName;
            this.homeScore = homeScore;
            this.createdAt = createdAt;
        }
    }

    @Getter
    class DiaryListResponse {
        private Long id;
        private Long teamId;
        private String teamName;
        private String content;

        private Long memberId;
        private String nickNm;

        private LocalDateTime matchAt;
        private MatchEnum.MatchStatus status;
    }

}
