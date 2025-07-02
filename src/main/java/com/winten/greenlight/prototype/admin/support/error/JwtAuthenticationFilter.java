package com.winten.greenlight.prototype.admin.support.error;

import com.winten.greenlight.prototype.admin.domain.user.CurrentUser;
import com.winten.greenlight.prototype.admin.domain.user.UserRole;
import com.winten.greenlight.prototype.admin.support.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);

        if (token != null && validateToken(token)) {
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
        filterChain.doFilter(request, response);
    }

    private Collection<? extends GrantedAuthority> getAuthorities(UserRole userRole) {
        if (userRole == UserRole.ADMIN) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_CLIENT")
            );
        } else if (userRole == UserRole.CLIENT) {
            return List.of(new SimpleGrantedAuthority("ROLE_CLIENT"));
        }
        // 시스템 에러 (개발이 정상적으로 되었다면 이곳에 도달하면 안됨)
        throw new CoreException(ErrorType.DEFAULT_ERROR, "UserRole cannot be null");

    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            jwtUtil.extractUsername(token);
            return true;
        } catch (ExpiredJwtException | MalformedJwtException e) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 인증정보입니다. ");
        } catch (Exception e) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "알 수 없는 이유로 인증에 실패하였습니다. ");
        }
    }

}