package com.winten.greenlight.admin.support.web;

import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {
    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void reusesValidRequestIdAndReturnsItInResponse() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/sites/site-a");
        request.addHeader(RequestIdFilter.HEADER_NAME, "request-123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(RequestScopeUtil.REQUEST_ID_ATTRIBUTE)).isEqualTo("request-123");
        assertThat(request.getAttribute(RequestScopeUtil.SOURCE_PATH_ATTRIBUTE)).isEqualTo("/api/sites/site-a");
        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo("request-123");
    }

    @Test
    void prefersValidAdminSourcePathHeader() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/users/user-a/status");
        request.addHeader(RequestIdFilter.SOURCE_PATH_HEADER_NAME, " /users/user-a ");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(RequestScopeUtil.SOURCE_PATH_ATTRIBUTE))
                .isEqualTo("/users/user-a");
    }

    @Test
    void rejectsExternalSourcePathAndFallsBackToRequestUri() throws Exception {
        var request = new MockHttpServletRequest();
        request.setRequestURI("/api/users/user-a/status");
        request.addHeader(RequestIdFilter.SOURCE_PATH_HEADER_NAME, "https://example.com/users/user-a");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(RequestScopeUtil.SOURCE_PATH_ATTRIBUTE))
                .isEqualTo("/api/users/user-a/status");
    }

    @Test
    void replacesOversizedRequestId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER_NAME, "x".repeat(65));
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER_NAME))
                .isNotBlank()
                .hasSizeLessThanOrEqualTo(64)
                .isNotEqualTo("x".repeat(65));
    }
}
