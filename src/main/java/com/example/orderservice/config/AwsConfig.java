package com.example.orderservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.time.Duration;

/**
 * AWS SDK v2 clients.
 *
 * The DynamoDB client uses ADAPTIVE retry mode with increased max attempts to handle the
 * heavy ProvisionedThroughputExceededException rate (~145 throttle errors/hour observed in
 * production via Dynatrace) on the under-provisioned orders table.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public DynamoDbClient dynamoDb() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c -> c
                        .retryPolicy(RetryPolicy.builder(RetryMode.ADAPTIVE)
                                .numRetries(8)
                                .build())
                        .apiCallTimeout(Duration.ofSeconds(10))
                        .apiCallAttemptTimeout(Duration.ofSeconds(4)))
                .build();
    }

    @Bean
    public S3Client s3() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
