package kr.co.victoryfairy.support.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.victoryfairy.support.utils.SlackUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * <pre>
 *     400 이상의 status가 나갈 때 slack으로 url, curl, response를 담아서 보내는 기능을 하는 interceptor
 * </pre>
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class CurlCommandErrorInterceptor implements HandlerInterceptor {

    private final SlackUtils slackUtils;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {

        String uri = request.getRequestURI();
        if (uri.contains("/swagger") || uri.contains("/v3/api-docs")) {
            return;
        }

        if (response instanceof ContentCachingResponseWrapper && request instanceof ContentCachingRequestWrapper) {
            try {
                ContentCachingRequestWrapper wrappedRequest = (ContentCachingRequestWrapper) request;
                ContentCachingResponseWrapper wrappedResponse = (ContentCachingResponseWrapper) response;

                String contentType = response.getContentType();
                /*
                 * if (contentType == null || !contentType.contains("application/json")) {
                 * return; }
                 */

                String responseBody = this.getResponseBody(wrappedResponse);
                if (this.isErrorResponse(responseBody)) {
                    String requestBody = this.getRequestBody(wrappedRequest);
                    this.generateCurlCommand(wrappedRequest, requestBody, responseBody);
                }
            }
            catch (Exception e) {
                log.error("Error in CurlCommandErrorInterceptor: ", e);
            }
        }
    }

    private void generateCurlCommand(HttpServletRequest request, String requestBody, String responseBody) {
        var curlCommand = new StringBuilder("curl -X ");
        curlCommand.append(request.getMethod()).append(" '").append(request.getRequestURL());

        // Query parameters
        var queryString = request.getQueryString();
        if (queryString != null) {
            curlCommand.append("?").append(queryString);
        }
        curlCommand.append("'");

        // Headers
        var headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            curlCommand.append(" -H '").append(headerName).append(": ").append(headerValue).append("'");
        }

        // Body
        if (requestBody != null && !requestBody.isEmpty()) {
            curlCommand.append(" --data '").append(requestBody.replace("'", "\\'")).append("'");
        }

        // Slack으로 전송
        slackUtils.sendSlackCurl(String.valueOf(request.getRequestURL()), String.valueOf(curlCommand), responseBody);

        log.info("Generated curl command: {}", curlCommand);
    }

    private boolean isErrorResponse(String responseBody) {
        if (responseBody == null) {
            return false;
        }
        try {
            var responseMap = new ObjectMapper().readValue(responseBody, Map.class);
            if (responseMap.containsKey("status")) {
                int status = (int) responseMap.get("status");
                return status == 500; // 에러로 간주할 상태 코드
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        try {
            byte[] content = request.getContentAsByteArray();
            if (content.length > 0) {
                return new String(content, request.getCharacterEncoding());
            }
        }
        catch (UnsupportedEncodingException e) {
            log.error("Error reading request body: ", e);
        }
        return null;
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return null;
    }

}
