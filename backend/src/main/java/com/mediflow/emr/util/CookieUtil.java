package com.mediflow.emr.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 쿠키 생성 및 관리를 위한 유틸리티 클래스
 * - JWT 토큰을 쿠키에 저장하고 삭제하는 기능 제공
 * - 보안 플래그(HttpOnly, Secure, SameSite, Domain, Path)를 일관되게 적용
 * - SameSite=None일 경우, 브라우저 정책상 Secure=true가 요구
 * - 개발 환경에서는 보통 http://localhost 이므로 Secure=false로 설정
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final CookieProperties props;

    /**
     * Access Token 쿠키를 추가
     * @param response 응답 객체
     * @param token   액세스 토큰 값
     */
    public void addAccessTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = buildCookie(props.getAccessTokenName(), token, props.getAccessMaxAge());
        setCookieHeader(response, cookie);
    }

    /**
     * Refresh Token 쿠키를 추가
     * @param response 응답 객체
     * @param token    리프레시 토큰 값
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = buildCookie(props.getRefreshTokenName(), token, props.getRefreshMaxAge());
        setCookieHeader(response, cookie);
    }

    /**
     * 요청에서 특정 이름의 쿠키 값을 조회
     * @param request 요청 객체
     * @param name    쿠키 이름
     * @return 쿠키 값 또는 null (존재하지 않을 경우)
     */
    public String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 요청에서 Access Token 쿠키 값을 조회
     */
    public String readAccessToken(HttpServletRequest request) {
        return readCookie(request, props.getAccessTokenName());
    }

    /**
     * 요청에서 Refresh Token 쿠키 값을 조회
     */
    public String readRefreshToken(HttpServletRequest request) {
        return readCookie(request, props.getRefreshTokenName());
    }


    /**
     * Access Token 쿠키를 삭제
     * @param response 응답 객체
     */
    public void deleteAccessTokenCookie(HttpServletResponse response) {
        deleteCookie(response, props.getAccessTokenName());
    }

    /**
     * Refresh Token 쿠키를 삭제
     * @param response 응답 객체
     */
    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        deleteCookie(response, props.getRefreshTokenName());
    }

    /**
     * 쿠키 삭제를 위한 공통 메서드
     */
    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = buildCookie(name, "", 0);
        setCookieHeader(response, cookie);
    }

    /**
     * 쿠키 빌더 메서드
     * - 공통 속성 설정
     * - maxAgeSeconds: 쿠키 수명 설정 (0이면 즉시 삭제)
     */
    private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(props.isHttpOnly())
                .secure(props.isSecure())
                .domain(props.getDomain())
                .path("/")
                .sameSite(props.getSameSite())
                .maxAge(maxAgeSeconds)
                .build();
    }

    /**
     * 쿠키 헤더 설정
     * @param response 응답 객체
     * @param cookie  설정할 쿠키
     */
    private void setCookieHeader(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
}
