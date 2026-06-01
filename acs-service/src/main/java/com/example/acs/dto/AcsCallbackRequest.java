package com.example.acs.dto;

/** payment-service'e gönderilen 3DS sonuç callback'i. */
public record AcsCallbackRequest(String acsTransactionId, String status) {}

