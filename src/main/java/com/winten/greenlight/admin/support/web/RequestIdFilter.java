package com.winten.greenlight.admin.support.web;

import com.winten.greenlight.admin.support.util.RequestScopeUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Request-ID";
    public static final String SOURCE_PATH_HEADER_NAME = "X-Admin-Source-Path";
    private static final int REQUEST_ID_MAX_LENGTH = 64;
    private static final int SOURCE_PATH_MAX_LENGTH = 1000;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = normalize(request.getHeader(HEADER_NAME));
        request.setAttribute(RequestScopeUtil.REQUEST_ID_ATTRIBUTE, requestId);
        request.setAttribute(
                RequestScopeUtil.SOURCE_PATH_ATTRIBUTE,
                resolveSourcePath(request.getHeader(SOURCE_PATH_HEADER_NAME), request.getRequestURI())
        );
        response.setHeader(HEADER_NAME, requestId);
        filterChain.doFilter(request, response);
    }

    private String normalize(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > REQUEST_ID_MAX_LENGTH) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }

    private String resolveSourcePath(String requestedSourcePath, String requestUri) {
        if (requestedSourcePath != null) {
            String normalized = requestedSourcePath.trim();
            if (!normalized.isBlank()
                    && normalized.length() <= SOURCE_PATH_MAX_LENGTH
                    && normalized.startsWith("/")
                    && !normalized.startsWith("//")
                    && normalized.indexOf('\r') < 0
                    && normalized.indexOf('\n') < 0) {
                return normalized;
            }
        }
        if (requestUri == null || requestUri.isBlank()) return null;
        return requestUri.length() <= SOURCE_PATH_MAX_LENGTH
                ? requestUri
                : requestUri.substring(0, SOURCE_PATH_MAX_LENGTH);
    }
}
