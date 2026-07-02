package com.example.orderservice.config;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.AdaptiveRetryStrategy;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS SDK v2 clients.
 *
 * The DynamoDB client uses ADAPTIVE retry mode which includes client-side rate limiting.
 * Under production throttling (ProvisionedThroughputExceededException on the under-provisioned
 * orders table), adaptive retry smooths traffic by dynamically adjusting the send rate rather
 * than just retrying with exponential backoff. This is informed by Dynatrace observability of
 * the hot write path's throttling behavior.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public DynamoDbClient dynamoDb() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c -> c.retryStrategy(AdaptiveRetryStrategy.builder()
                        .maxAttempts(4)
                        .build()))
                .build();
    }

    @Bean
    public S3Client s3() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
