package com.mediflow.emr.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jwt")
// application.yml에서 jwt관련 프로퍼티 값을 읽어오는 클래스
public class JwtProperties {
    private String secret; // application.yml: jwt.secret // JWT 서명에 사용되는 비밀 키
    private long accessExpiration; // application.yml: jwt.access-expiration // 액세스 토큰 만료 시간 (밀리초 단위)
    private long refreshExpiration; // application.yml: jwt.refresh-expiration // 리프레시 토큰 만료 시간 (밀리초 단위)
}
