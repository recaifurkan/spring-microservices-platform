package com.example.acs.dto;

/** payment-service'ten gelen ACS init isteği. */
public record AcsInitRequest(
        Long paymentId,
        String paymentCallbackUrl,
        String frontendCallbackUrl,
        String amount,
        String merchantName) {}

