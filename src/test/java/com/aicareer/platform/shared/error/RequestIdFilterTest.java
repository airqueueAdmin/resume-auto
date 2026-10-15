package com.aicareer.platform.shared.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void preservesSafeRequestId() throws Exception {
        MockHttpServletRequest request = request("request-123:abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("request-123:abc");
        assertThat(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)).isEqualTo("request-123:abc");
    }

    @Test
    void replacesUnsafeOrOversizedRequestId() throws Exception {
        MockHttpServletRequest request = request("request\nwith-injection");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(requestId).isEqualTo(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE));
        assertThat(UUID.fromString(requestId)).isNotNull();
    }

    private MockHttpServletRequest request(String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, requestId);
        return request;
    }
}
