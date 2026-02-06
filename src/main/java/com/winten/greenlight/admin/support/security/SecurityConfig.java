package com.winten.greenlight.admin.support.security;

import tools.jackson.databind.json.JsonMapper;
import com.winten.greenlight.admin.support.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CoreAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtUtil jwtUtil;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(HttpMethod.OPTIONS)
                .requestMatchers(CorsUtils::isPreFlightRequest)
                .requestMatchers(HttpMethod.GET, "/health", "/favicon.ico", "/sites/*")
                .requestMatchers(HttpMethod.POST, "/users/login", "/users/signin")
                .requestMatchers("/error")
                .requestMatchers("/swagger-ui/**")
                .requestMatchers("/api-docs/**")
                .requestMatchers("/action-events/traffic/sse/stream");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JsonMapper jsonMapper) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers(HttpMethod.OPTIONS).permitAll()
                    .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                    .requestMatchers(HttpMethod.GET, "/health", "/favicon.ico", "/sites/*").permitAll()
                    .requestMatchers(HttpMethod.POST, "/users/login", "/users/signin").permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers("/action-events/traffic/sse/stream").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/**")
                        .hasAnyAuthority(Permission.PERM_READ.name())
                    .requestMatchers(HttpMethod.POST, "/**")
                        .hasAnyAuthority(Permission.PERM_WRITE.name())
                    .requestMatchers(HttpMethod.PUT, "/**")
                        .hasAnyAuthority(Permission.PERM_WRITE.name())
                    .requestMatchers(HttpMethod.DELETE, "/**")
                        .hasAnyAuthority(Permission.PERM_WRITE.name())
                    .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, jsonMapper), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
            ;

        return http.build();
    }
}