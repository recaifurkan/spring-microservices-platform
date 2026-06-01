package com.example.frontend.dto;

/** Sipariş satırı (from-cart için). */
public record OrderItemRequest(Long productId, Integer quantity) {}

