package com.example.orderservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A clean, moderate-traffic read path against a well-provisioned table — no throttling.
 */
@Service
public class InventoryService {

    private final DynamoDbClient dynamoDb;
    private final String table;

    public InventoryService(DynamoDbClient dynamoDb, @Value("${inventory.table}") String table) {
        this.dynamoDb = dynamoDb;
        this.table = table;
    }

    public List<Map<String, String>> listInventory() {
        ScanResponse result = dynamoDb.scan(ScanRequest.builder().tableName(table).limit(50).build());
        List<Map<String, String>> items = new ArrayList<>();
        for (Map<String, AttributeValue> row : result.items()) {
            Map<String, String> flat = new HashMap<>();
            row.forEach((k, v) -> flat.put(k, v.s() != null ? v.s() : v.n()));
            items.add(flat);
        }
        return items;
    }
}
