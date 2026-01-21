package kr.co.victoryfairy.core.api.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import io.dodn.springboot.core.enums.MatchEnum;

public interface CommonDomain {

    @Schema(name = "Common.TeamListResponse")
    record TeamListResponse(
            @Schema(description = "team id")
            Long id,

            @Schema(description = "팀명")
            String name,

            @Schema(description = "라벨")
            String label,

            @Schema(description = "리그 타입 (KBO, WBC, MLB)")
            MatchEnum.LeagueType league,

            @Schema(description = "WBC 국가 코드 (KOR, JPN, USA 등)")
            String countryCode
    ) {}

    record SeatListResponse(
            @Schema(description = "seat id")
            Long id,

            @Schema(description = "좌석명")
            String name
    ) {}
}
