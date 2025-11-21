package com.mediflow.emr.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * 웹 보안 구성을 담당
 * Spring Security를 활용하여 HTTP 보안 체인을 설정하며, 주로 JWT 기반 인증 시스템을 지원하기 위해 구성
 * CORS 설정, CSRF 비활성화, 세션 관리 정책, 엔드포인트 권한 정책 등을 정의
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 애플리케이션의 보안 필터 체인을 구성
     *
     * 이 메서드는 다음과 같은 주요 보안 정책을 정의
     * - 특정 오리진에 대한 CORS 허용
     * - CSRF 보호 비활성화 (JWT 사용)
     * - 무상태 세션 관리 적용 (서버가 세션 상태를 저장하지 않음)
     * - 기본 로그인 및 HTTP 기본 인증 메커니즘 비활성화
     * - H2 콘솔 사용을 위한 동일 출처 iframe 접근 허용
     * - 엔드포인트 권한 규칙 정의
     * - 인증되지 않은 접근에 대해 401 상태 반환
     *
     * @param http 보안 필터 체인을 구성하기 위한 HttpSecurity 객체
     * @return 구성된 SecurityFilterChain 인스턴스
     * @throws Exception 보안 필터 체인 구성 중 오류 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http
            , JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                // 프론트엔드 도메인에서의 요청을 허용하는 CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // CSRF 보호 비활성화 (JWT 사용 시 필요 없음)
                .csrf(csrf -> csrf.disable())
                // 세션을 서버에 저장하지 않는 무상태 정책
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 기본 로그인 폼 및 HTTP 기본 인증 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // H2 콘솔 사용을 위한 동일 출처 iframe 접근 허용
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // 엔드포인트별 권한 정책 설정
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/health", "/", "/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                )
                // OAuth2 로그인 설정
                .oauth2Login(oauth -> {})
                // 인증되지 않은 접근에 대해 401 상태 코드 반환
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(401))
                )
                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

    /**
     * CORS 구성 소스 정의
     * - 특정 오리진에서의 요청 허용
     * - 허용된 HTTP 메서드 및 헤더 설정
     * - 자격 증명(쿠키, 인증 헤더 등) 포함 허용
     * 운영 배포 시, 실제 프론트엔드 도메인(예: https://app.example.com)을 추가
     * @return CORS 구성 소스
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
