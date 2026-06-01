package com.example.orderservice.dto;

import java.math.BigDecimal;

/** product-service /api/products/{id} yanıtı için local DTO. */
public record ProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String category) {}

