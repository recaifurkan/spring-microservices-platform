package com.example.frontend.dto;

import java.math.BigDecimal;

/** Ödeme başlatma isteği. */
public record InitiatePaymentRequest(Long orderId, String amount, String paymentMethod) {}

