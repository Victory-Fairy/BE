package kr.co.victoryfairy.admin.infrastructure;

import tools.jackson.databind.ObjectMapper;
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
public class AdminAccessTokenFilter extends PathPatternWebFilter {
    private final JwtProperties jwtProperties;

    public AdminAccessTokenFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        this.addIncludePathPatterns("/v2/admin/member/**");
        this.addIncludePathPatterns("/v2/admin/diary/**");
        this.addIncludePathPatterns("/v2/admin/community/**");
        this.addExcludePathPatterns(
                "/",
                "/swagger-ui/**",
                "/swagger/**",
                "/v2/admin/auth/login"
        );
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
