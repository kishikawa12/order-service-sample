package com.example.orderservice.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Cold path — a nightly report export to S3. Called about once a day, so Dynatrace shows it as
 * low-traffic and low-risk. A telemetry-informed migration deprioritizes it relative to the hot
 * order path.
 */
@Service
public class ReportService {

    private final S3Client s3;
    private final String bucket;

    public ReportService(S3Client s3, @Value("${reports.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String exportNightly() {
        String key = "reports/nightly-" + Instant.now().toEpochMilli() + ".txt";
        String body = "Nightly order report generated at " + Instant.now();
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build(),
                RequestBody.fromString(body));
        return key;
    }
}
