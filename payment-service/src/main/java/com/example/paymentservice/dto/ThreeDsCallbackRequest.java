package com.example.paymentservice.dto;

/** ACS'den gelen 3DS sonuç callback isteği. */
public record ThreeDsCallbackRequest(String acsTransactionId, String status) {}

