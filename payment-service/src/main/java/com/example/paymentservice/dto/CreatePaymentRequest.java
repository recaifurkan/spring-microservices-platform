package com.example.paymentservice.dto;

import java.math.BigDecimal;

/** Doğrudan ödeme oluşturma isteği. */
public record CreatePaymentRequest(Long orderId, BigDecimal amount, String paymentMethod) {}

