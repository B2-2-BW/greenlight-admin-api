package com.winten.greenlight.admin.support.security;

import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@RequiredArgsConstructor
public class CookieOriginFilter extends OncePerRequestFilter {
    private final CorsConfigurationSource corsConfigurationSource;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!"POST".equals(request.getMethod())) {
            return true;
        }
        if ("/users/refresh".equals(request.getServletPath())) {
            return false;
        }
        if (!"/users/logout".equals(request.getServletPath()) || request.getCookies() == null) {
            return true;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())
                    && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var configuration = corsConfigurationSource.getCorsConfiguration(request);
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null || configuration == null || configuration.checkOrigin(origin) == null) {
            if (exceptionResolver.resolveException(request, response, null, CoreException.of(ErrorType.FORBIDDEN)) == null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }
        filterChain.doFilter(request, response);
    }
}
