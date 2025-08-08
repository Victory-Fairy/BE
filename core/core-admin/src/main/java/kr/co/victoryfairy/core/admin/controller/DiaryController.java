package kr.co.victoryfairy.core.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.admin.domain.DiaryDomain;
import kr.co.victoryfairy.core.admin.service.DiaryService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Diary", description = "일기")
@RestController
@RequestMapping("/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "일기 목록 불러오기")
    @GetMapping("/list")
    public CustomResponse<List<DiaryDomain.DiaryListResponse>> findAll(@Validated DiaryDomain.DiaryListRequest request) {
        var result = diaryService.findAll(request);
        return CustomResponse.ok(result.getContents(), result.getTotal());
    }
}
