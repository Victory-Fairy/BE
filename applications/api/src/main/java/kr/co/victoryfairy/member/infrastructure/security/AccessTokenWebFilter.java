package kr.co.victoryfairy.member.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.victoryfairy.web.response.MessageEnum;
import kr.co.victoryfairy.web.error.CustomException;
import kr.co.victoryfairy.web.response.CustomResponse;
import kr.co.victoryfairy.member.infrastructure.security.JwtProperties;
import kr.co.victoryfairy.member.infrastructure.security.AccessTokenCodec;
import kr.co.victoryfairy.web.filter.PathPatternWebFilter;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccessTokenWebFilter extends PathPatternWebFilter {
    private final JwtProperties jwtProperties;

    public AccessTokenWebFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.addIncludePathPatterns("/v2/api/member/**");
        this.addIncludePathPatterns("/v2/api/my-page/**");
        this.addIncludePathPatterns("/v2/api/diary/**");
        this.addExcludePathPatterns("/", "/swagger-ui/**", "/swagger/**", "/v2/api/member/auth-path",
                "/v2/api/member/login", "/v2/api/member/refresh-token", "/v2/api/match/list",
                "/v2/api/diary/list", "/v2/api/diary/daily-list", "/v2/api/member/match-today",
                "/v2/api/my-page/member", "/v2/api/my-page/victory-power");
    }

    @Override
    public void filterMatched(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            AccessTokenCodec.checkToken(request, jwtProperties);
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
