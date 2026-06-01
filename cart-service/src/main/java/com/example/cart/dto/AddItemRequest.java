package com.example.cart.dto;

/** Sepete ürün ekleme isteği. */
public record AddItemRequest(Long productId, Integer quantity) {}

