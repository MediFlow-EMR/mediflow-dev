package com.mediflow.emr.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cookie")
// application.yml에서 cookie관련 프로퍼티 값을 읽어오는 클래스
public class CookieProperties {
    private String domain; // application-dev.yml: cookie.domain
    private boolean secure;
    private boolean httpOnly; // 자바스크립트에서 쿠키 접근 불가, XSS 공격 방어
    private String accessTokenName;
    private String refreshTokenName;
    private int accessMaxAge; // seconds
    private int refreshMaxAge; // seconds
}
