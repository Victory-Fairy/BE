package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.FreeDiaryDomain;
import kr.co.victoryfairy.core.api.service.FreeDiaryService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "FreeDiary", description = "자유 일기")
@RestController
@RequestMapping("/free-diary")
@RequiredArgsConstructor
public class FreeDiaryController {

    private final FreeDiaryService freeDiaryService;

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "자유 일기 작성")
    @PostMapping
    public CustomResponse<FreeDiaryDomain.WriteResponse> write(@RequestBody FreeDiaryDomain.WriteRequest request) {
        var response = freeDiaryService.write(request);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "자유 일기 수정")
    @PatchMapping("/{id}")
    public CustomResponse<MessageEnum> update(@PathVariable Long id, @RequestBody FreeDiaryDomain.UpdateRequest request) {
        freeDiaryService.update(id, request);
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "자유 일기 삭제")
    @DeleteMapping("/{id}")
    public CustomResponse<MessageEnum> delete(@PathVariable Long id) {
        freeDiaryService.delete(id);
        return CustomResponse.ok(MessageEnum.Common.DELETE);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "자유 일기 상세")
    @GetMapping("/{id}")
    public CustomResponse<FreeDiaryDomain.DetailResponse> findById(@PathVariable Long id) {
        var response = freeDiaryService.findById(id);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "자유 일기 월별 목록")
    @GetMapping("/list")
    public CustomResponse<List<FreeDiaryDomain.ListResponse>> findList(
            @RequestParam @DateTimeFormat(pattern = "yyyyMM") YearMonth date) {
        var response = freeDiaryService.findList(date);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "자유 일기 일자별 목록")
    @GetMapping("/daily-list")
    public CustomResponse<List<FreeDiaryDomain.DailyListResponse>> findDailyList(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {
        var response = freeDiaryService.findDailyList(date);
        return CustomResponse.ok(response);
    }
}