package com.example.footprint.config;

import com.example.footprint.dto.CommonDtos.ApiError;
import com.example.footprint.security.JwtAuthenticationFilter;
import java.time.Clock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {
    @Bean Clock clock() { return Clock.systemUTC(); }
    @Bean PasswordEncoder passwordEncoder() { return new Argon2PasswordEncoder(16, 32, 1, 19456, 2); }
    @Bean FilterRegistrationBean<JwtAuthenticationFilter> disableServletRegistration(JwtAuthenticationFilter filter) {
        var bean = new FilterRegistrationBean<>(filter); bean.setEnabled(false); return bean;
    }
    @Bean SecurityFilterChain security(HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper json) throws Exception {
        return http.csrf(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/signup", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/policies", "/api/policies/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/files/profiles/**").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401); res.setContentType("application/json"); res.setCharacterEncoding("UTF-8");
                    json.writeValue(res.getOutputStream(), new ApiError("AUTHENTICATION_REQUIRED", "로그인이 필요합니다."));
                })
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403); res.setContentType("application/json"); res.setCharacterEncoding("UTF-8");
                    json.writeValue(res.getOutputStream(), new ApiError("FORBIDDEN", "접근 권한이 없습니다."));
                }))
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
    }
}
