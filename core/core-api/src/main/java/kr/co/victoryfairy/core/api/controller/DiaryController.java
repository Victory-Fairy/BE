package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.DiaryDomain;
import kr.co.victoryfairy.core.api.service.DiaryService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Diary", description = "일기")
@RestController
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;

    public DiaryController(DiaryService diaryService) {
        this.diaryService = diaryService;
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 작성")
    @PostMapping()
    public CustomResponse<DiaryDomain.WriteResponse> writeDiary(@RequestBody DiaryDomain.WriteRequest request){
        var response = diaryService.writeDiary(request);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 수정")
    @PatchMapping("/{id}")
    public CustomResponse<MessageEnum> updateDiary(@PathVariable Long id, @RequestBody DiaryDomain.UpdateRequest request){
        diaryService.updateDiary(id, request);
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 삭제")
    @DeleteMapping("/{id}")
    public CustomResponse<MessageEnum> deleteDiary(@PathVariable Long id){
        diaryService.deleteDiary(id);
        return CustomResponse.ok(MessageEnum.Common.DELETE);
    }

    @Operation(summary = "일기 목록")
    @GetMapping("/list")
    public CustomResponse<List<DiaryDomain.ListResponse>> findList(@RequestParam @DateTimeFormat(pattern = "yyyyMM") YearMonth date) {
        var response = diaryService.findList(date);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "일자별 일기")
    @GetMapping("/daily-list")
    public CustomResponse<List<DiaryDomain.DailyListResponse>> findDailyList(@RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {
        var response = diaryService.findDailyList(date);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 상세")
    @GetMapping("/{id}")
    public CustomResponse<DiaryDomain.DiaryDetailResponse> findById(@PathVariable Long id) {
        var response = diaryService.findById(id);
        return CustomResponse.ok(response);
    }
}
