package kr.co.victoryfairy.core.api.webfilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.victoryfairy.support.constant.MessageEnum;
import kr.co.victoryfairy.support.exception.CustomException;
import kr.co.victoryfairy.support.model.CustomResponse;
import kr.co.victoryfairy.support.properties.JwtProperties;
import kr.co.victoryfairy.support.utils.AccessTokenUtils;
import kr.co.victoryfairy.support.webfilter.PathPatternWebFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class AccessTokenPublicWebFilter extends PathPatternWebFilter {

    private final JwtProperties jwtProperties;

    public AccessTokenPublicWebFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.addIncludePathPatterns("/v2/api/match/list");
        this.addIncludePathPatterns("/v2/api/diary/list");
        this.addIncludePathPatterns("/v2/api/diary/daily-list");
        this.addIncludePathPatterns("/v2/api/member/match-today");
        this.addIncludePathPatterns("/v2/api/my-page/member");
        this.addIncludePathPatterns("/v2/api/my-page/victory-power");
        this.addIncludePathPatterns("/match/list");
        this.addIncludePathPatterns("/diary/list");
        this.addIncludePathPatterns("/diary/daily-list");
        this.addIncludePathPatterns("/member/match-today");
        this.addIncludePathPatterns("/my-page/member");
        this.addIncludePathPatterns("/my-page/victory-power");
    }

    @Override
    public void filterMatched(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String accessToken = AccessTokenUtils.getAccessToken(request);
            if (StringUtils.hasText(accessToken)) {
                AccessTokenUtils.checkAccessToken(accessToken, jwtProperties, request);
            }
        } catch (Exception e) {
            ObjectMapper objectMapper = new ObjectMapper();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            CustomException customException = e instanceof CustomException ? ((CustomException) e) : new CustomException().of(MessageEnum.Common.REQUEST_FAIL);
            response.setStatus(customException.getHttpStatus().value());
            var customResponse = CustomResponse.<String>builder()
                    .status(customException.getStatusEnum().getStatus())
                    .errorMsg(customException.getMessage()) // 예외 메시지 대신 고정 메시지 사용
                    .build();
            response.getWriter().write(objectMapper.writeValueAsString(customResponse));

            return;
        }
        filterChain.doFilter(request, response);
    }
}
