package com.example.orderservice.dto;

import java.util.List;

/** Sepetten toplu sipariş oluşturma isteği. */
public record CreateFromCartRequest(List<CreateOrderRequest> items) {}

