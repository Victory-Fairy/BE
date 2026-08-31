package kr.co.victoryfairy.member.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.co.victoryfairy.member.presentation.MyPageDomain;
import kr.co.victoryfairy.member.application.MemberWithdrawalService;
import kr.co.victoryfairy.member.application.MyPageQueryService;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.response.CustomResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "My Page", description = "미이 페이지")
@RestController
@RequestMapping("/api/my-page")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageQueryService queryService;

    private final MemberWithdrawalService withdrawalService;

    @Operation(summary = "유저 정보")
    @GetMapping("/member")
    public CustomResponse<MyPageDomain.MemberInfoForMyPageResponse> findMemberInfo() {
        var response = queryService.findMemberInfoForMyPage();
        return CustomResponse.ok(response);
    }

    @Operation(summary = "승요 레벨")
    @GetMapping("/victory-power")
    public CustomResponse<MyPageDomain.VictoryPowerResponse> findVictoryPower(
            @RequestParam(required = false) String season) {
        var response = queryService.findVictoryPower(season);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "관람 분석")
    @GetMapping("/report")
    public CustomResponse<MyPageDomain.ReportResponse> findReport(@RequestParam(required = false) String season) {
        var response = queryService.findReport(season);
        return CustomResponse.ok(response);
    }

    @SecurityRequirement(name = "accessToken")
    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/delete-account")
    public CustomResponse<MessageEnum> deleteMember(@RequestBody MyPageDomain.DeleteAccountRequest request) {
        withdrawalService.deleteMember(request);
        return CustomResponse.ok(MessageEnum.Common.REQUEST);
    }

}
