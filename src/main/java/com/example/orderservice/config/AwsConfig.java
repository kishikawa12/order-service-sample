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
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean(destroyMethod = "close")
    public DynamoDbClient dynamoDb() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c -> c.retryStrategy(RetryMode.ADAPTIVE))
                .build();
    }

    @Bean(destroyMethod = "close")
    public S3Client s3() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}
