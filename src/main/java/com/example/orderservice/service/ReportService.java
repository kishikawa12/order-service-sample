package com.example.orderservice.service;

import com.amazonaws.services.s3.AmazonS3;
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

    private final AmazonS3 s3;
    private final String bucket;

    public ReportService(AmazonS3 s3, @Value("${reports.bucket}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    public String exportNightly() {
        String key = "reports/nightly-" + Instant.now().toEpochMilli() + ".txt";
        String body = "Nightly order report generated at " + Instant.now();
        s3.putObject(bucket, key, body);
        return key;
    }
}
