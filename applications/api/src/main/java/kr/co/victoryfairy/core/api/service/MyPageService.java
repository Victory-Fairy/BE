package kr.co.victoryfairy.core.api.service;

import kr.co.victoryfairy.core.api.domain.MyPageDomain;

public interface MyPageService {

    /**
     * 마이페이지 - 회원 정보 조회
     * @return
     */
    MyPageDomain.MemberInfoForMyPageResponse findMemberInfoForMyPage();

    /**
     * 마이페이지 - 승요 파워 조회
     * @return
     */
    MyPageDomain.VictoryPowerResponse findVictoryPower(String season);

    /**
     * 마이페이지 - 관람 분석
     * @param season
     * @return
     */
    MyPageDomain.ReportResponse findReport(String season);

    /**
     * 마이페이지 - 회원 탈퇴
     * @param request
     */
    void deleteMember(MyPageDomain.DeleteAccountRequest request);

}
