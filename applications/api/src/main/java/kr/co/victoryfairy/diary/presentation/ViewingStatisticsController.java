package kr.co.victoryfairy.diary.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.diary.application.ViewingStatisticsQueryService;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "My Page", description = "미이 페이지")
@RestController
@RequestMapping("/api/my-page")
@RequiredArgsConstructor
public class ViewingStatisticsController {
    private final ViewingStatisticsQueryService service;

    @Operation(summary = "승요 레벨")
    @GetMapping("/victory-power")
    public CustomResponse<ViewingStatisticsDomain.VictoryPowerResponse> findVictoryPower(
            @RequestParam(required = false) String season) {
        return CustomResponse.ok(service.findVictoryPower(season));
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "관람 분석")
    @GetMapping("/report")
    public CustomResponse<ViewingStatisticsDomain.ReportResponse> findReport(
            @RequestParam(required = false) String season) {
        return CustomResponse.ok(service.findReport(season));
    }
}
