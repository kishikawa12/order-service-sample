package com.example.orderservice.model;

/** Payload for creating an order. */
public record OrderRequest(String customer, long amountCents) {
}
