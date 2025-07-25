package kr.co.victoryfairy.core.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.core.api.domain.MyPageDomain;
import kr.co.victoryfairy.core.api.service.MemberService;
import kr.co.victoryfairy.core.api.service.MyPageService;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.model.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "My Page", description = "미이 페이지")
@RestController
@RequestMapping("/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @Operation(summary = "유저 정보")
    @GetMapping("/member")
    public CustomResponse<MyPageDomain.MemberInfoForMyPageResponse> findMemberInfo() {
        var response = myPageService.findMemberInfoForMyPage();
        return CustomResponse.ok(response);
    }

    @Operation(summary = "승요 레벨")
    @GetMapping("/victory-power")
    public CustomResponse<MyPageDomain.VictoryPowerResponse> findVictoryPower(@RequestParam(required = false) String season) {
        var response = myPageService.findVictoryPower(season);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "관람 분석")
    @GetMapping("/report")
    public CustomResponse<MyPageDomain.ReportResponse> findReport(@RequestParam(required = false) String season) {
        var response = myPageService.findReport(season);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/delete-account")
    public CustomResponse<MessageEnum> deleteMember(@RequestBody MyPageDomain.DeleteAccountRequest request) {
        myPageService.deleteMember(request);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }
}
