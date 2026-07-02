package com.example.orderservice.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A clean, moderate-traffic read path against a well-provisioned table — no throttling.
 * Contrasts with the orders path so Dynatrace shows resilience is needed there but not here.
 */
@Service
public class InventoryService {

    private final AmazonDynamoDB dynamoDb;
    private final String table;

    public InventoryService(AmazonDynamoDB dynamoDb, @Value("${inventory.table}") String table) {
        this.dynamoDb = dynamoDb;
        this.table = table;
    }

    public List<Map<String, String>> listInventory() {
        ScanResult result = dynamoDb.scan(new ScanRequest(table).withLimit(50));
        List<Map<String, String>> items = new ArrayList<>();
        for (Map<String, AttributeValue> row : result.getItems()) {
            Map<String, String> flat = new java.util.HashMap<>();
            row.forEach((k, v) -> flat.put(k, v.getS() != null ? v.getS() : v.getN()));
            items.add(flat);
        }
        return items;
    }
}
