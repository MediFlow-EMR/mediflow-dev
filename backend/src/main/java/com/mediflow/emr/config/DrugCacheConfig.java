package com.mediflow.emr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

@Configuration
public class DrugCacheConfig {
    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "drug-cache.enabled", havingValue = "true")
    S3Client drugCacheS3Client(@Value("${drug-cache.region:ap-northeast-2}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                // Production EC2 role only; no permanent access keys are required.
                .credentialsProvider(InstanceProfileCredentialsProvider.create())
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(1))
                        .socketTimeout(Duration.ofSeconds(2)))
                .overrideConfiguration(config -> config
                        .apiCallAttemptTimeout(Duration.ofSeconds(2))
                        .apiCallTimeout(Duration.ofSeconds(3)))
                .build();
    }
}
