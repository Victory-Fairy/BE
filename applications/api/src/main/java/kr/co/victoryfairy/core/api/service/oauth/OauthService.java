package kr.co.victoryfairy.core.api.service.oauth;

import kr.co.victoryfairy.core.api.domain.MemberDomain;

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
