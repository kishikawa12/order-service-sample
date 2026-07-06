package com.example.orderservice.service;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.example.orderservice.model.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages order persistence in DynamoDB.
 */
@Service
public class OrderService {

    private final AmazonDynamoDB dynamoDb;
    private final String table;

    public OrderService(AmazonDynamoDB dynamoDb, @Value("${orders.table}") String table) {
        this.dynamoDb = dynamoDb;
        this.table = table;
    }

    public Order create(String customer, long amountCents) {
        Order order = new Order(UUID.randomUUID().toString(), customer, amountCents, "NEW");

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("orderId", new AttributeValue(order.orderId()));
        item.put("customer", new AttributeValue(order.customer()));
        item.put("amountCents", new AttributeValue().withN(Long.toString(order.amountCents())));
        item.put("status", new AttributeValue(order.status()));

        dynamoDb.putItem(new PutItemRequest(table, item));
        return order;
    }

    public Order get(String orderId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("orderId", new AttributeValue(orderId));

        GetItemResult result = dynamoDb.getItem(new GetItemRequest(table, key));
        Map<String, AttributeValue> item = result.getItem();
        if (item == null || item.isEmpty()) {
            return null;
        }
        return new Order(
                item.get("orderId").getS(),
                item.get("customer").getS(),
                Long.parseLong(item.get("amountCents").getN()),
                item.get("status").getS());
    }
}
