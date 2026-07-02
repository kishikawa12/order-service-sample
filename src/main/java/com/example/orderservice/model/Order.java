package com.example.orderservice.model;

/** An order record persisted to DynamoDB. */
public record Order(String orderId, String customer, long amountCents, String status) {
}
