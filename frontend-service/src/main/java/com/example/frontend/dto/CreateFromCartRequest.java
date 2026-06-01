package com.example.frontend.dto;

import java.util.List;

/** Sepetten sipariş oluşturma isteği. */
public record CreateFromCartRequest(List<OrderItemRequest> items) {}

