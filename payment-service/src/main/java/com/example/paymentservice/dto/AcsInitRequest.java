package com.example.paymentservice.dto;

import java.math.BigDecimal;

/** ACS servisine gönderilen init isteği. */
public record AcsInitRequest(
        Long paymentId,
        String paymentCallbackUrl,
        String frontendCallbackUrl,
        String amount,
        String merchantName) {}

