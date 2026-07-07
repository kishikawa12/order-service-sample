package com.example.orderservice.web;

import com.example.orderservice.service.OrderExportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CSV export of orders. */
@RestController
@RequestMapping("/exports")
public class OrderExportController {

    private final OrderExportService export;

    public OrderExportController(OrderExportService export) {
        this.export = export;
    }

    @GetMapping(value = "/orders", produces = "text/csv")
    public ResponseEntity<String> orders() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(export.exportCsv());
    }
}
