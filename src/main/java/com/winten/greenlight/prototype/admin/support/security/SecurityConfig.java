package com.winten.greenlight.prototype.admin.support.security;

import com.winten.greenlight.prototype.admin.support.error.JwtAuthenticationFilter;
import com.winten.greenlight.prototype.admin.support.util.JwtUtil;
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
                .requestMatchers("/error")
                .requestMatchers(HttpMethod.GET, "/favicon.ico")
                .requestMatchers(HttpMethod.POST, "/users/login")
                .requestMatchers("/swagger-ui/**")
                .requestMatchers("/api-docs/**");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                    .requestMatchers("/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.POST, "/users/login").permitAll()
                    .requestMatchers("/swagger-ui/**").permitAll()
                    .requestMatchers("/api-docs/**").permitAll()
                    .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(authenticationEntryPoint)
            );

        return http.build();
    }
}