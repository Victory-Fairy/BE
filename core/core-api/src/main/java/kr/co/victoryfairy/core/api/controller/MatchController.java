package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.MatchDomain;
import kr.co.victoryfairy.core.api.service.MatchService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Match", description = "경기")
@RestController
@RequestMapping("/match")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "특정 날짜 경기 불러오기")
    @GetMapping("/list")
    public CustomResponse<MatchDomain.MatchListResponse> findList(@RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {
        var response = matchService.findList(date);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "오늘 경기 목록")
    @GetMapping("/today")
    public CustomResponse<MatchDomain.TodayMatchListResponse> findTodayMatch() {
        return CustomResponse.ok(matchService.findTodayMatch());
    }

    @Operation(summary = "경기 상세")
    @GetMapping("/{id}")
    public CustomResponse<MatchDomain.MatchInfoResponse> findById(@PathVariable String id) {
        var response = matchService.findById(id);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "경기 기록")
    @GetMapping("/record/{id}")
    public CustomResponse<MatchDomain.RecordResponse> findRecordById(@PathVariable String id) {
        var response = matchService.findRecordById(id);
        return CustomResponse.ok(response);
    }
}
