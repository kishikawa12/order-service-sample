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
 * The DynamoDB client uses a plain default retry policy (fixed max error retry). This is the
 * naive baseline: under production throttling it retries a fixed number of times with the
 * default backoff and otherwise fails. A migration that only sees the code maps this to a v2
 * default. A migration informed by Dynatrace — which shows this path throttling in production —
 * has the context to configure v2 adaptive retry (client-side rate limiting) instead.
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
