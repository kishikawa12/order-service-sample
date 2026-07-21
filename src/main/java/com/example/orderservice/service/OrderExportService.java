package com.example.orderservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Map;

/**
 * Produces a CSV export of the orders table.
 */
@Service
public class OrderExportService {

    private final DynamoDbClient dynamoDb;
    private final String table;

    public OrderExportService(DynamoDbClient dynamoDb, @Value("${orders.table}") String table) {
        this.dynamoDb = dynamoDb;
        this.table = table;
    }

    public String exportCsv() {
        StringBuilder csv = new StringBuilder("orderId,customer,amountCents,status\n");

        Map<String, AttributeValue> lastKey = null;
        do {
            ScanRequest.Builder requestBuilder = ScanRequest.builder().tableName(table).limit(100);
            if (lastKey != null) {
                requestBuilder.exclusiveStartKey(lastKey);
            }

            ScanResponse result = dynamoDb.scan(requestBuilder.build());
            for (Map<String, AttributeValue> row : result.items()) {
                csv.append(value(row, "orderId")).append(',')
                        .append(value(row, "customer")).append(',')
                        .append(value(row, "amountCents")).append(',')
                        .append(value(row, "status")).append('\n');
            }

            lastKey = result.lastEvaluatedKey();
        } while (lastKey != null && !lastKey.isEmpty());

        return csv.toString();
    }

    private static String value(Map<String, AttributeValue> row, String field) {
        AttributeValue v = row.get(field);
        if (v == null) {
            return "";
        }
        return v.s() != null ? v.s() : (v.n() != null ? v.n() : "");
    }
}
