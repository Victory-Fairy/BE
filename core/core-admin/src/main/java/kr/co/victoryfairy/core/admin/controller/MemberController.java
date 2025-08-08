package kr.co.victoryfairy.core.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.admin.domain.MemberDomain;
import kr.co.victoryfairy.core.admin.service.MemberService;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 목록 불러오기")
    @GetMapping("/list")
    public CustomResponse<List<MemberDomain.MemberListResponse>> findList(@Validated MemberDomain.MemberListRequest request){
        var result = memberService.findList(request);
        return CustomResponse.ok(result.getContents(), result.getTotal());
    }
}
