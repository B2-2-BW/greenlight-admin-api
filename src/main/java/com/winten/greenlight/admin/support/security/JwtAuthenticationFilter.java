package com.winten.greenlight.admin.support.security;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.admin.domain.user.AccountStatus;
import com.winten.greenlight.admin.domain.user.CachedUserService;
import com.winten.greenlight.admin.domain.user.CurrentUser;
import com.winten.greenlight.admin.domain.user.UserRole;
import com.winten.greenlight.admin.support.error.CoreException;
import com.winten.greenlight.admin.support.error.ErrorResponse;
import com.winten.greenlight.admin.support.error.ErrorType;
import com.winten.greenlight.admin.support.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final JsonMapper jsonMapper;
    private final CachedUserService cachedUserService;
    private final HandlerExceptionResolver exceptionResolver;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        return "POST".equals(request.getMethod()) && switch (request.getServletPath()) {
            case "/users/login", "/users/signin", "/users/refresh", "/users/logout",
                    "/users/password/change-required" -> true;
            default -> false;
        };
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromHeader(request);
            if (validateToken(token)) {
                String userId = jwtUtil.extractUserId(token);
                var user = cachedUserService.getUser(userId);
                if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                    throw CoreException.of(ErrorType.UNAUTHORIZED, "사용할 수 없는 계정입니다.");
                }
                // 초기화 필요 계정은 인증 세션을 허용하지 않는다. (로그인 전 public 변경 API 사용)
                if (Boolean.TRUE.equals(user.getPasswordResetRequired())) {
                    throw CoreException.of(
                            ErrorType.USER_PASSWORD_RESET_REQUIRED,
                            "비밀번호 변경 후 로그인할 수 있습니다."
                    );
                }

                String userSiteId = user.resolveHomeSiteId();
                String requestedSiteId = request.getHeader("X-ADMIN-SITE-ID");
                var accessibleSiteIds = user.resolveSiteIds();
                if (requestedSiteId != null && !requestedSiteId.isBlank()) {
                    requestedSiteId = requestedSiteId.trim();
                    if (user.getUserRole() == UserRole.SUPER) {
                        userSiteId = requestedSiteId;
                    } else if (accessibleSiteIds.contains(requestedSiteId)) {
                        userSiteId = requestedSiteId;
                    } else {
                        throw CoreException.of(ErrorType.FORBIDDEN, "해당 사이트에 대한 권한이 없습니다");
                    }
                }

                CurrentUser currentUser = CurrentUser.builder()
                        .accountId(user.getAccountId())
                        .userId(user.getUserId())
                        .userSiteId(userSiteId)
                        .userRole(user.getUserRole())
                        .accessibleSiteIds(user.getUserRole() == UserRole.SUPER ? List.of() : accessibleSiteIds)
                        .build();

                // Spring Security Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                currentUser, null, getAuthorities(currentUser.getUserRole())
                        );
                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (ExpiredJwtException e) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            boolean access = "access".equals(e.getClaims().get("token_use", String.class));
            var error = new ErrorResponse(org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Unauthorized.", null, LocalDateTime.now(), access ? "ACCESS_TOKEN_EXPIRED" : null);
            response.getWriter().write(jsonMapper.writeValueAsString(error));
            return;
        } catch (CoreException e) {
            if (e.getErrorType().getStatus().is5xxServerError()) {
                if (exceptionResolver.resolveException(request, response, null, e) == null) {
                    response.setStatus(e.getErrorType().getStatus().value());
                }
                return;
            }
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(e.getErrorType().getStatus().value());
            Object detail = e.getDetail() instanceof Throwable ? null : e.getDetail();
            var errorResponse = new ErrorResponse(e.getErrorType().getStatus(),
                    e.getErrorType().getMessage(), detail, LocalDateTime.now());
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
            jwtUtil.parseToken(token, "access");
            return true;
        } catch (ExpiredJwtException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "인증정보를 찾을 수 없습니다.");
        } catch (io.jsonwebtoken.JwtException e) {
            throw CoreException.of(ErrorType.UNAUTHORIZED, "유효하지 않은 인증정보입니다.");
        }
    }

}
