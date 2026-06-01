package com.example.frontend.dto;

/** Sepete ürün ekleme isteği. */
public record AddItemRequest(Long productId, Integer quantity) {}

