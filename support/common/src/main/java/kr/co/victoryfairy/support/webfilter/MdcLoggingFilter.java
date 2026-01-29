package kr.co.victoryfairy.support.webfilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.victoryfairy.logging.context.LogContext;
import kr.co.victoryfairy.logging.context.LogContextHolder;
import kr.co.victoryfairy.logging.sql.SqlLoggingHolder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청별 MDC 컨텍스트 및 LogContext 설정 필터
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();
        String requestId = generateRequestId();

        try {
            // MDC 설정
            MDC.put(MDC_REQUEST_ID, requestId);

            // LogContext 설정 (레이어 로깅용)
            LogContextHolder.set(new LogContext(requestId));

            // 요청 시작 로깅
            log.info("[REQUEST] {} {} from {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    getClientIp(request));

            // 응답 헤더에 requestId 추가 (디버깅용)
            response.setHeader("X-Request-Id", requestId);

            filterChain.doFilter(request, response);

        }
        finally {
            long duration = System.currentTimeMillis() - startTime;

            // 요청 완료 로깅
            log.info("[RESPONSE] {} {} - {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);

            // 정리
            MDC.clear();
            LogContextHolder.clear();
            SqlLoggingHolder.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 정적 리소스, swagger 제외
        return path.contains("/swagger")
                || path.contains("/api-docs")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".ico")
                || path.endsWith(".png")
                || path.endsWith(".jpg")
                || path.endsWith(".html");
    }

    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있을 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

}
