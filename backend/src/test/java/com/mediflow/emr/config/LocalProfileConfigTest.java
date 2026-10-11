package com.mediflow.emr.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Configuration;
import static org.junit.jupiter.api.Assertions.*;

class LocalProfileConfigTest {
    @Configuration(proxyBeanMethods = false)
    static class ConfigurationProbe {}

    @Test void devDefaultsToValidationAsWell() {
        try (var context = new SpringApplicationBuilder(ConfigurationProbe.class)
                .web(WebApplicationType.NONE).run("--spring.profiles.active=dev", "--spring.main.banner-mode=off")) {
            assertEquals("validate", context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto"));
        }
    }
    @Test void localProfileNeverCreatesSchemaOrSeedsDemoData() {
        // Load actual ConfigData precedence, without starting JPA or connecting to a DB.
        try (var context = new SpringApplicationBuilder(ConfigurationProbe.class)
                .web(WebApplicationType.NONE).run("--spring.profiles.active=local", "--spring.main.banner-mode=off")) {
            var env = context.getEnvironment();
            assertEquals("validate", env.getProperty("spring.jpa.hibernate.ddl-auto"));
            assertEquals("false", env.getProperty("spring.flyway.enabled"));
            assertArrayEquals(new String[]{"local"}, env.getActiveProfiles());
            assertEquals("false", env.getProperty("cookie.secure"));
            assertEquals("Lax", env.getProperty("cookie.same-site"));
            assertEquals("", env.getProperty("cookie.domain"));
        }
    }
}
