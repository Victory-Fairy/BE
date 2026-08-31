package kr.co.victoryfairy.common.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.common.presentation.CommonDomain;
import kr.co.victoryfairy.common.application.CommonQueryService;
import io.dodn.springboot.core.enums.MatchEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Common", description = "공통")
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final CommonQueryService commonService;

    @GetMapping("/health")
    public CustomResponse<Boolean> healthCheck() {
        return CustomResponse.ok(true);
    }

    @Operation(summary = "팀 전체 목록 불러오기", description = "KBO 팀 목록 조회")
    @GetMapping("/team")
    public CustomResponse<List<CommonDomain.TeamListResponse>> findAll() {
        var response = commonService.findAll(MatchEnum.LeagueType.KBO);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "좌석 정보 불러오기")
    @GetMapping("/seat/{id}")
    public CustomResponse<List<CommonDomain.SeatListResponse>> findSeat(@PathVariable Long id,
            @RequestParam String season) {
        var response = commonService.findSeat(id, season);
        return CustomResponse.ok(response);
    }

}
