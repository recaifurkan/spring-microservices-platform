package com.example.orderservice.dto;

/** Tekli sipariş oluşturma isteği. */
public record CreateOrderRequest(Long productId, Integer quantity) {}

