package com.example.orderservice.web;

import com.example.orderservice.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Cold path — nightly export, ~once a day. */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @PostMapping("/nightly")
    public ResponseEntity<Map<String, String>> nightly() {
        String key = reports.exportNightly();
        return ResponseEntity.ok(Map.of("report", key));
    }
}
