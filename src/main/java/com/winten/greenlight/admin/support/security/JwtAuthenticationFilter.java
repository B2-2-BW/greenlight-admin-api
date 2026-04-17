package com.winten.greenlight.admin.support.security;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorResponse;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final JsonMapper jsonMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromHeader(request);
            if (validateToken(token)) {
                // UserService를 통해 token에서 사용자 정보 추출
                CurrentUser currentUser = jwtUtil.getCurrentUserFromToken(token);

                // Spring Security Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                currentUser, null, getAuthorities(currentUser.getUserRole())
                        );
                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (CoreException e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            var errorResponse = new ErrorResponse(e);
            response.getWriter().write(jsonMapper.writeValueAsString(errorResponse));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(UserRole role) {
        return switch (role) {
            case USER -> List.of(new SimpleGrantedAuthority(Permission.PERM_READ.name()));
            case SITE_ADMIN -> List.of(
                    new SimpleGrantedAuthority(Permission.PERM_READ.name()),
                    new SimpleGrantedAuthority(Permission.PERM_WRITE.name())
            );
            case SUPER -> List.of(
                    new SimpleGrantedAuthority(Permission.PERM_READ.name()),
                    new SimpleGrantedAuthority(Permission.PERM_WRITE.name()),
                    new SimpleGrantedAuthority(Permission.PERM_SUPER.name())
            );
            default -> List.of(); // GUEST는 보통 인증 객체 자체가 없게 처리(permitAll)
        };
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        if (token == null) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "인증정보를 찾을 수 없습니다.");
        }

        try {
            jwtUtil.extractUserId(token);
            return true;
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("인증정보를 찾을 수 없습니다.");
        } catch (ExpiredJwtException | MalformedJwtException e) {
            throw new BadCredentialsException("유효하지 않은 인증정보입니다.");
        } catch (Exception e) {
            throw new BadCredentialsException("알 수 없는 이유로 인증에 실패하였습니다.");
        }
    }

}