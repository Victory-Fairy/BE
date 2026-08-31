package kr.co.victoryfairy.member.infrastructure.oauth;

import kr.co.victoryfairy.member.presentation.MemberDomain;

public interface OauthService {

    /**
     * sns 인증 주소 반환
     * @return
     */
    String initSnsAuthPath(String redirectUrl);

    /**
     * sns정보를 객체로 파싱
     * @param request
     * @return
     */
    MemberDomain.MemberSns parseSnsInfo(MemberDomain.MemberLoginRequest request);

}
