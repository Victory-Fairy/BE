package kr.co.victoryfairy.admin.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.admin.application.AdminMemberQueryService;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/admin/member")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberQueryService adminMemberQueryService;

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "회원 목록 불러오기")
    @GetMapping("/list")
    public CustomResponse<List<AdminMemberDto.MemberListResponse>> findList(
            @Validated AdminMemberDto.MemberListRequest request) {
        var result = adminMemberQueryService.findList(request);
        return CustomResponse.ok(result.getContents(), result.getTotal());
    }

}
