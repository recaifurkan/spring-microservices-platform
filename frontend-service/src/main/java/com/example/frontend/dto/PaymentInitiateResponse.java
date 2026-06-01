package com.example.frontend.dto;

import java.math.BigDecimal;

/**
 * payment-service /api/payments/initiate yanıtı.
 * type: FRICTIONLESS | CHALLENGE
 */
public record PaymentInitiateResponse(
        Long paymentId,
        String transactionId,
        BigDecimal amount,
        String type,
        String status,
        String acsTransactionId,
        String acsUrl) {}

