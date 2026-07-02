package com.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Order service demo. A Spring Boot REST app on AWS SDK v1 (DynamoDB + S3).
 *
 * OneAgent auto-instruments each REST endpoint as a distinct service request, so Dynatrace
 * reports per-endpoint traffic and error rates with no custom entry-point configuration.
 * The hot write path (POST /orders) throttles in production against an under-provisioned
 * DynamoDB table — the signal a Dynatrace-informed AWS Transform migration reacts to by
 * configuring v2 client resilience (adaptive retry) rather than a naive default mapping.
 */
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
