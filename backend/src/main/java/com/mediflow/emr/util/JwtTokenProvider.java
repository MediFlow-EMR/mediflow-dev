package com.mediflow.emr.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT 토큰 생성 및 검증 유틸리티 클래스
 * - jjwt 라이브러리 사용
 * - HMAC-SHA 알고리즘 기반 서명
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties; // JWT 설정 프로퍼티

    private SecretKey key; // 서명 키

    /**
     * 초기화 메서드: 애플리케이션 시작 시 시크릿 키 초기화.
     */
    @PostConstruct
    public void init() {
        // Base64로 인코딩된 키를 디코딩해 SecretKey 생성
        this.key = toSecretKey(jwtProperties.getSecret());
    }

    /**
     * Base64 인코딩된 시크릿을 SecretKey 객체로 변환.
     * @param base64Secret
     * @return SecretKey
     */
    private SecretKey toSecretKey(String base64Secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /**
     * JWT 토큰 생성.
     * @param subject 토큰 주체(예: 사용자 ID)
     * @param claims 커스텀 클레임 ((Access Token에 최소한으로 포함, 예: 사용자 권한)
     * @param expirationMillis 만료 시간 (밀리초)
     * @return JWT 토큰 문자열
     */
    private String createToken(String subject, Map<String, Object> claims, long expirationMillis) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 서명 검증 후 Claims 파싱.
     * @param token JWT 토큰
     * @return Claims
     */
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }


    /**
     * Access Token 생성.
     * 클라이언트 표시/권한 확인용 최소 정보만 클레임에 포함 권장.
     * @param subject
     * @param claims
     * @return
     */
    public String createAccessToken(String subject, Map<String, Object> claims) {
        return createToken(subject, claims, jwtProperties.getAccessExpiration());
    }

    /**
     * Refresh Token 생성.
     * 클레임 없이 주체 정보만 포함.
     * 커스텀 클레임을 넣지 않는 이유
     * - 보안: 리프레시 토큰은 장기간 보관되므로 민감한 정보를 포함하지 않는 것이 좋음.
     * - 단순성: 리프레시 토큰은 주로 액세스 토큰 갱신에 사용되므로 최소한의 정보만 필요.
     * @param subject
     * @return
     */
    public String createRefreshToken(String subject) {
        return createToken(subject, Map.of(), jwtProperties.getRefreshExpiration());
    }

    /**
     * 토큰 유효성 검사.
     * @param token JWT 토큰
     * @return 유효하면 true
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰에서 Claims 추출.
     * @param token JWT 토큰
     * @return Claims
     */
    public Claims getClaims(String token) {
        return parseClaims(token);
    }
}
