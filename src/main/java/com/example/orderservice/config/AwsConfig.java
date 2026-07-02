package com.example.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS SDK v2 clients.
 *
 * The DynamoDB client uses ADAPTIVE_V2 retry mode (client-side rate limiting) because
 * Dynatrace production telemetry shows the hot order-write path regularly hits
 * ProvisionedThroughputExceededException under load. Adaptive retry dynamically throttles
 * the request rate to avoid overwhelming DynamoDB provisioned capacity.
 *
 * The S3 client uses the standard retry mode — Dynatrace shows no throttling on the
 * cold report-export path.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public DynamoDbClient dynamoDb() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c -> c.retryStrategy(RetryMode.ADAPTIVE_V2))
                .build();
    }

    @Bean
    public S3Client s3() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
