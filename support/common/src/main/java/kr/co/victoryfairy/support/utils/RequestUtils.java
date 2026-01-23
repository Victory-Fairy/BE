package kr.co.victoryfairy.support.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.victoryfairy.support.model.oauth.MemberAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;

@Slf4j
public class RequestUtils {

    public static String getRemoteIp() {
        var request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        return request.getRemoteAddr();
    }

    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        }
        catch (Exception e) {
            return "Unknown Host";
        }
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
            log.info("request >>>> {}", request);
            var memberAccount = request.getAttribute("accountByToken");
            if (memberAccount == null) {
                return null;
            }
            log.info("memberAccount >>>> {}", memberAccount);
            var account = new ObjectMapper().convertValue(memberAccount, MemberAccount.class);
            log.info("account >>>> {}", account);
            return account.getId();
        }
        return null;
    }

}