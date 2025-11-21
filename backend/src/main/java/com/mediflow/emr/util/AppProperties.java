package com.mediflow.emr.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String oauth2SuccessRedirectUrl; // application.yml: app.oauth2-success-redirect-url
}
