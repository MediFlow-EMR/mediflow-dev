package com.mediflow.emr.config;

import com.mediflow.emr.util.CookieProperties;
import com.mediflow.emr.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 *  JWT 인증 필터
 *  - 모든 HTTP 요청에 대해 한 번씩 실행되는 필터
 *  - 요청에서 JWT 액세스 토큰을 추출하고 검증
 *  - 유효한 토큰이 있으면 인증 컨텍스트에 사용자 정보를 설정
 *  - 토큰은 Authorization 헤더 Bearer 토큰 또는 HTTP-Only 쿠키 (ACCESS_TOKEN)에서 추출
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieProperties cookieProperties;

    /**
     * HTTP 요청을 필터링하여 JWT 토큰을 검증하고 인증 컨텍스트를 설정
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 필터 체인
     * @throws ServletException 서블릿 예외 발생 시
     * @throws IOException      입출력 예외 발생 시
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰이 존재하고 유효하며, 아직 인증 정보가 설정되지 않은 경우
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            Claims claims = jwtTokenProvider.getClaims(token);
            String subject = claims.getSubject(); // 토큰의 주체(예: 사용자 ID)

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    subject,
                    null,
                    Collections.emptyList()
            );
            ((UsernamePasswordAuthenticationToken) authentication)
                    .setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 인증 세부 정보 설정

            SecurityContextHolder.getContext().setAuthentication(authentication); // 인증 정보 설정
        }

        filterChain.doFilter(request, response); // 다음 필터로 요청 전달
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출
     * - 우선적으로 Authorization 헤더의 Bearer 토큰을 확인
     * - 헤더에 토큰이 없으면 쿠키에서 ACCESS_TOKEN 값을 확인
     *
     * @param request HTTP 요청
     * @return 추출된 JWT 토큰 또는 null
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieProperties.getAccessTokenName().equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}