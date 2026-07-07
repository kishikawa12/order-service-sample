package com.example.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * AWS SDK v2 clients.
 *
 * The DynamoDB client uses a plain default retry policy (fixed max error retry, default backoff).
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public DynamoDbClient dynamoDb() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c -> c.retryPolicy(
                        RetryPolicy.builder().numRetries(3).build()))
                .build();
    }

    @Bean
    public S3Client s3() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
