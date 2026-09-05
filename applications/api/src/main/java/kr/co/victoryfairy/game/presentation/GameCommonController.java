package kr.co.victoryfairy.game.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import kr.co.victoryfairy.game.application.CommonQueryService;
import kr.co.victoryfairy.game.domain.MatchEnum;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Common", description = "공통")
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class GameCommonController {
    private final CommonQueryService commonService;

    @Operation(summary = "팀 전체 목록 불러오기", description = "KBO 팀 목록 조회")
    @GetMapping("/team")
    public CustomResponse<List<CommonDomain.TeamListResponse>> findAll() {
        return CustomResponse.ok(commonService.findAll(MatchEnum.LeagueType.KBO));
    }

    @Operation(summary = "좌석 정보 불러오기")
    @GetMapping("/seat/{id}")
    public CustomResponse<List<CommonDomain.SeatListResponse>> findSeat(@PathVariable Long id,
            @RequestParam String season) {
        return CustomResponse.ok(commonService.findSeat(id, season));
    }
}
