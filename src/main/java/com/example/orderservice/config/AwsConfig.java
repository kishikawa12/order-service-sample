package com.example.orderservice.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AWS SDK v1 clients.
 *
 * The DynamoDB client uses a plain default retry policy (fixed max error retry, default backoff).
 */
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public AmazonDynamoDB dynamoDb() {
        ClientConfiguration clientConfig = new ClientConfiguration()
                .withMaxErrorRetry(3)
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicy());

        return AmazonDynamoDBClientBuilder.standard()
                .withRegion(region)
                .withClientConfiguration(clientConfig)
                .build();
    }

    @Bean
    public AmazonS3 s3() {
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
    }
}
