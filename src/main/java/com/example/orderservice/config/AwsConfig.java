package com.example.orderservice.config;

import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS SDK v2 clients.
 *
 * The DynamoDB client uses ADAPTIVE retry mode to handle throttling
 * (ProvisionedThroughputExceededException) by dynamically adjusting request rates.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public DynamoDbClient dynamoDb() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .overrideConfiguration(c -> c.retryStrategy(RetryMode.ADAPTIVE))
                .build();
    }
}
