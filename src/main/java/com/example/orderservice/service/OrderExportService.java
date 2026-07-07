package com.example.orderservice.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Produces a CSV export of the orders table.
 */
@Service
public class OrderExportService {

    private final AmazonDynamoDB dynamoDb;
    private final String table;

    public OrderExportService(AmazonDynamoDB dynamoDb, @Value("${orders.table}") String table) {
        this.dynamoDb = dynamoDb;
        this.table = table;
    }

    public String exportCsv() {
        StringBuilder csv = new StringBuilder("orderId,customer,amountCents,status\n");

        Map<String, AttributeValue> lastKey = null;
        do {
            ScanRequest request = new ScanRequest(table).withLimit(100);
            if (lastKey != null) {
                request.setExclusiveStartKey(lastKey);
            }

            ScanResult result = dynamoDb.scan(request);
            for (Map<String, AttributeValue> row : result.getItems()) {
                csv.append(value(row, "orderId")).append(',')
                        .append(value(row, "customer")).append(',')
                        .append(value(row, "amountCents")).append(',')
                        .append(value(row, "status")).append('\n');
            }

            lastKey = result.getLastEvaluatedKey();
        } while (lastKey != null && !lastKey.isEmpty());

        return csv.toString();
    }

    private static String value(Map<String, AttributeValue> row, String field) {
        AttributeValue v = row.get(field);
        if (v == null) {
            return "";
        }
        return v.getS() != null ? v.getS() : (v.getN() != null ? v.getN() : "");
    }
}
