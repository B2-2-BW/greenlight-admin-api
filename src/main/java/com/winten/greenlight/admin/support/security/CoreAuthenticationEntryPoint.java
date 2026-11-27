package com.winten.greenlight.admin.support.security;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.admin.support.error.ErrorResponse;
import com.winten.greenlight.admin.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CoreAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final JsonMapper jsonMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 커스텀 에러 응답 생성
        var errorResponse = new ErrorResponse(ErrorType.UNAUTHORIZED.getStatus(),
                ErrorType.UNAUTHORIZED.getMessage(), null, LocalDateTime.now());

        response.getWriter().write(jsonMapper.writeValueAsString(errorResponse));
    }
}
