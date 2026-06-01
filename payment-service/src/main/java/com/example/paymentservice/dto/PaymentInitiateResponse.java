package com.example.paymentservice.dto;

import java.math.BigDecimal;

/**
 * 3DS ödeme başlatma yanıtı.
 * type: FRICTIONLESS | CHALLENGE
 * status: SUCCESS (frictionless için), null (challenge için)
 * acsTransactionId / acsUrl: sadece CHALLENGE akışında dolu
 */
public record PaymentInitiateResponse(
        Long paymentId,
        String transactionId,
        BigDecimal amount,
        String type,
        String status,
        String acsTransactionId,
        String acsUrl) {}

