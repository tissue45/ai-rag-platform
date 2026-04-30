package com.astra.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@ConditionalOnProperty(name = "app.ingest.async.enabled", havingValue = "true")
public class AwsSqsConfig {

    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.of(System.getProperty("aws.region", System.getenv().getOrDefault("AWS_REGION", "ap-northeast-2"))))
                .build();
    }
}
