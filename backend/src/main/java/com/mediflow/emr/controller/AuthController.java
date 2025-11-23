package com.mediflow.emr.controller;

import com.mediflow.emr.util.CookieUtil;
import com.mediflow.emr.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * OAuth2 인증 관련 API 컨트롤러
 * - JWT 토큰 관리 (갱신/로그아웃)
 * - Google, Kakao 소셜 로그인 지원
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;

    /**
     * Refresh Token을 검증하고 새 Access Token을 발급
     *
     * 동작 순서
     * 1) 요청 쿠키에서 Refresh Token을 조회
     * 2) 토큰 유효성 검증(서명/만료)
     * 3) 유효하면 동일 subject로 새로운 Access Token 생성 후 쿠키로 반환
     * 4) 실패하면 401 Unauthorized 반환(쿠키 변경 없음)
     *
     * @param request  HTTP 요청
     * @param response HTTP 응답
     * @return 204 No Content 또는 401 Unauthorized
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        log.debug("[AuthController] /api/auth/refresh called, remoteAddr={}", request.getRemoteAddr());

        // 1) 쿠키에서 Refresh Token 조회
        String refreshToken = cookieUtil.readRefreshToken(request);

        // 2) 토큰 유효성 검증
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("[AuthController] Refresh token missing or invalid");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 3) 유효한 토큰이면 동일 subject로 새 Access Token 생성
        Claims claims = jwtTokenProvider.getClaims(refreshToken);
        String subject = claims.getSubject();

        // 4) 새 Access Token 생성 및 쿠키로 설정
        String newAccessToken = jwtTokenProvider.createAccessToken(subject, Map.of());
        cookieUtil.addAccessTokenCookie(response, newAccessToken);

        log.info("[AuthController] Access token reissued for subject={}", subject);
        return ResponseEntity.noContent().build();
    }

    /**
     * 로그아웃 처리
     * - Access Token 및 Refresh Token 쿠키를 삭제
     *
     * @param response HTTP 응답
     * @return 204 No Content
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        cookieUtil.deleteAccessTokenCookie(response);
        cookieUtil.deleteRefreshTokenCookie(response);

        log.info("[AuthController] Logged out: cleared auth cookies");
        return ResponseEntity.noContent().build();
    }

}
