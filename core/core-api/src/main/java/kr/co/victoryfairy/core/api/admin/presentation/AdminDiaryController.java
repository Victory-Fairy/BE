package kr.co.victoryfairy.core.api.admin.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.admin.application.AdminDiaryQueryService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Diary", description = "일기")
@RestController
@RequestMapping("/admin/diary")
@RequiredArgsConstructor
public class AdminDiaryController {

    private final AdminDiaryQueryService adminDiaryQueryService;

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "일기 목록 불러오기")
    @GetMapping("/list")
    public CustomResponse<List<AdminDiaryDto.DiaryListResponse>> findAll(
            @Validated AdminDiaryDto.DiaryListRequest request) {
        var result = adminDiaryQueryService.findAll(request);
        return CustomResponse.ok(result.getContents(), result.getTotal());
    }

}
