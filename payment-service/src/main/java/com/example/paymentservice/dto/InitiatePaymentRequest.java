package com.example.paymentservice.dto;

import java.math.BigDecimal;

/** 3DS ödeme başlatma isteği. */
public record InitiatePaymentRequest(Long orderId, BigDecimal amount, String paymentMethod) {}

