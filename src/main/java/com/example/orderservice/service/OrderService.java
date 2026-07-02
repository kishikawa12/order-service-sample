package com.example.orderservice.service;

import com.example.orderservice.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The hot path. Writes/reads orders against an under-provisioned DynamoDB table, so putItem
 * throttles under load. This is where production throttling shows up in Dynatrace and where a
 * telemetry-informed migration would tune v2 retry behavior.
 */
@Service
public class OrderService {

    private final DynamoDbClient dynamoDb;
    private final String table;

    public OrderService(DynamoDbClient dynamoDb, @Value("${orders.table}") String table) {
        this.dynamoDb = dynamoDb;
        this.table = table;
    }

    public Order create(String customer, long amountCents) {
        Order order = new Order(UUID.randomUUID().toString(), customer, amountCents, "NEW");

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", AttributeValue.builder().s(order.orderId()).build());
        item.put("customer", AttributeValue.builder().s(order.customer()).build());
        item.put("amountCents", AttributeValue.builder().n(Long.toString(order.amountCents())).build());
        item.put("status", AttributeValue.builder().s(order.status()).build());

        dynamoDb.putItem(PutItemRequest.builder().tableName(table).item(item).build());
        return order;
    }

    public Order get(String orderId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("orderId", AttributeValue.builder().s(orderId).build());

        GetItemResponse result = dynamoDb.getItem(GetItemRequest.builder().tableName(table).key(key).build());
        Map<String, AttributeValue> item = result.item();
        if (item == null || item.isEmpty()) {
            return null;
        }
        return new Order(
                item.get("orderId").s(),
                item.get("customer").s(),
                Long.parseLong(item.get("amountCents").n()),
                item.get("status").s());
    }
}
