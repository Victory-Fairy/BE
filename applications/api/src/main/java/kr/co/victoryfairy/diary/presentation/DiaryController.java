package kr.co.victoryfairy.diary.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.diary.presentation.DiaryDomain;
import kr.co.victoryfairy.diary.application.DiaryCommandService;
import kr.co.victoryfairy.diary.application.DiaryQueryService;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.response.CustomResponse;
import kr.co.victoryfairy.member.infrastructure.security.RequestUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Diary", description = "일기")
@RestController
@RequestMapping("/api/diary")
public class DiaryController {

    private final DiaryCommandService commandService;

    private final DiaryQueryService queryService;

    public DiaryController(DiaryCommandService commandService, DiaryQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 작성")
    @PostMapping()
    public CustomResponse<DiaryDomain.WriteResponse> writeDiary(@RequestBody DiaryDomain.WriteRequest request) {
        Long memberId = RequestUtils.getId();
        var response = commandService.writeDiary(memberId, request);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 수정")
    @PatchMapping("/{id}")
    public CustomResponse<MessageEnum> updateDiary(@PathVariable Long id,
            @RequestBody DiaryDomain.UpdateRequest request) {
        commandService.updateDiary(id, request);
        return CustomResponse.ok(MessageEnum.Common.UPDATE);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 삭제")
    @DeleteMapping("/{id}")
    public CustomResponse<MessageEnum> deleteDiary(@PathVariable Long id) {
        commandService.deleteDiary(id);
        return CustomResponse.ok(MessageEnum.Common.DELETE);
    }

    @Operation(summary = "일기 목록")
    @GetMapping("/list")
    public CustomResponse<List<DiaryDomain.ListResponse>> findList(
            @RequestParam @DateTimeFormat(pattern = "yyyyMM") YearMonth date) {
        var response = queryService.findList(date);
        return CustomResponse.ok(response);
    }

    @Operation(summary = "일자별 일기")
    @GetMapping("/daily-list")
    public CustomResponse<List<DiaryDomain.DailyListResponse>> findDailyList(
            @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date) {
        var response = queryService.findDailyList(date);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 상세")
    @GetMapping("/{id}")
    public CustomResponse<DiaryDomain.DiaryDetailResponse> findById(@PathVariable Long id) {
        var response = queryService.findById(id);
        return CustomResponse.ok(response);
    }

}
