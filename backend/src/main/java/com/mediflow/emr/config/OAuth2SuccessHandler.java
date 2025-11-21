package com.mediflow.emr.config;


import com.mediflow.emr.util.AppProperties;
import com.mediflow.emr.util.CookieUtil;
import com.mediflow.emr.util.JwtTokenProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * OAuth2 로그인 성공 시 실행되는 핸들러.
 *
 * 동작 순서
 * 1) 인증 주체(OAuth2User)에서 식별자(subject)를 가져온다
 * 2) Access/Refresh JWT를 생성한다
 * 3) 두 토큰을 HTTP-Only 쿠키에 담아 응답 헤더(Set-Cookie)로 내려준다
 * 4) 프론트엔드 애플리케이션 URL로 안전하게 리다이렉트한다
 *
 * 보안 포인트
 * - 토큰을 URL 쿼리로 노출하지 않고, HTTP-Only 쿠키로만 전달한다 → XSS로부터 보호
 * - SameSite/Secure/Domain 등 쿠키 옵션은 CookieUtil에서 일괄 적용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final CookieUtil cookieUtil;
    private final AppProperties appProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // 1) OAuth2 인증 주체에서 고유 식별자(subject) 획득
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String subject = String.valueOf(oAuth2User.getName());

        // 2) Access/Refresh 토큰 생성 (클레임은 최소화)
        String accessToken = jwtTokenProvider.createAccessToken(subject, Map.of());
        String refreshToken = jwtTokenProvider.createRefreshToken(subject);

        // 3) 토큰을 HTTP-Only 쿠키로 설정 (URL 노출 금지)
        cookieUtil.addAccessTokenCookie(response, accessToken);
        cookieUtil.addRefreshTokenCookie(response, refreshToken);

        // 4) 프론트엔드 애플리케이션으로 리다이렉트
        String redirectUrl = appProperties.getOauth2SuccessRedirectUrl();
        response.sendRedirect(redirectUrl);
    }
}
