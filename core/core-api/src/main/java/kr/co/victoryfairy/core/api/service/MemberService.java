package kr.co.victoryfairy.core.api.service;

import io.dodn.springboot.core.enums.MemberEnum;
import kr.co.victoryfairy.core.api.domain.MemberDomain;

public interface MemberService {

    /**
     * sns 별 인증 주소 불러오기
     * @param snsType
     * @return
     */
    MemberDomain.MemberOauthPathResponse getOauthPath(MemberEnum.SnsType snsType, String redirectUrl);

    /**
     * sns 로그인
     * @param request
     * @return
     */
    MemberDomain.MemberLoginResponse login(MemberDomain.MemberLoginRequest request);

    /**
     * 관심 팀 등록/수정
     * @param request
     */
    void updateTeam(MemberDomain.MemberTeamUpdateRequest request);

    /**
     * 선점한 닉네임 조회
     * @return
     */
    MemberDomain.MemberCheckNickNameResponse checkNick();

    /**
     * 닉네임 중복 검사
     * @param nickNm
     * @return
     */
    MemberDomain.MemberCheckNickDuplicateResponse checkNickNmDuplicate(String nickNm);

    /**
     * 멤버 프로필 사진 수정
     * @param request
     */
    void updateMemberProfile(MemberDomain.MemberProfileUpdateRequest request);

    /**
     * 멤버 닉네임 수정
     * @param request
     */
    void updateMemberNickNm(MemberDomain.MemberNickNmUpdateRequest request);

    /**
     * 집관 승률
     * @return
     */
    MemberDomain.MemberHomeWinRateResponse findHomeWinRate();

}
