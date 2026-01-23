package kr.co.victoryfairy.support.webfilter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * <pre>
 *     기본적으로 HttpServletRequest의 본문은 한 번만 읽을 수 있기 때문에 요청이나 응답을 캐싱해뒀다가 interceptor에서 사용해야 함
 *     그러기 위해 아래 체이닝 필터가 필요함.
 *
 *     캐싱은 요청/응답이 끝나면 바로 삭제되기 때문에 요청이나 응답이 거대하지 않을경우 큰 이슈는 없어보이나 대량의 트레픽과 거대한 요청/응답 이 있을경우 개선해야 할 것으로 보임
 * </pre>
 */
public class RequestResponseCachingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper((HttpServletRequest) request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(
                (HttpServletResponse) response);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        }
        finally {
            wrappedResponse.copyBodyToResponse(); // 응답 본문 클라이언트로 복사
        }
    }

}