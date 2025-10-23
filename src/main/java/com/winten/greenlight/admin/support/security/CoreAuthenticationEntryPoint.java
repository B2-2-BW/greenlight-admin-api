package com.winten.greenlight.admin.support.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorResponse;
import com.winten.greenlight.admin.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CoreAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 커스텀 에러 응답 생성
        var exception = CoreException.of(ErrorType.UNAUTHORIZED, authException.getMessage());
        var errorResponse = new ErrorResponse(exception);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}