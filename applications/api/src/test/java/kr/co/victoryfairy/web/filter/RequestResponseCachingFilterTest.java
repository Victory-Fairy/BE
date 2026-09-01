package kr.co.victoryfairy.web.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;

class RequestResponseCachingFilterTest {

    @Test
    void limitsCachedRequestBodyTo64KiB() throws Exception {
        byte[] requestBody = new byte[64 * 1024 + 1];
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(requestBody);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<ContentCachingRequestWrapper> wrappedRequest = new AtomicReference<>();

        new RequestResponseCachingFilter().doFilter(request, response, (filteredRequest, filteredResponse) -> {
            filteredRequest.getInputStream().readAllBytes();
            wrappedRequest.set((ContentCachingRequestWrapper) filteredRequest);
        });

        assertThat(wrappedRequest.get().getContentAsByteArray()).hasSize(64 * 1024);
    }

}
