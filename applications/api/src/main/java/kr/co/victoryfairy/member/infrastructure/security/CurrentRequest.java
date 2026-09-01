package kr.co.victoryfairy.member.infrastructure.security;

import tools.jackson.databind.ObjectMapper;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class CurrentRequest {

    private CurrentRequest() {
    }

    public static String getRemoteIp() {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return request.getRemoteAddr();
    }

    /**
     * 로그인 id (id (PK))
     * @return
     */
    public static Long getId() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
            .getRequestAttributes();

        if (requestAttributes != null) {
            var request = requestAttributes.getRequest();
            var memberAccount = request.getAttribute("accountByToken");
            if (memberAccount == null) {
                return null;
            }
            var account = new ObjectMapper().convertValue(memberAccount, MemberAccount.class);
            return account.getId();
        }
        return null;
    }

}
