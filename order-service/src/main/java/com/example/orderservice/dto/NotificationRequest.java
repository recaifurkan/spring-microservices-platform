package com.example.orderservice.dto;

/** Bildirim gönderme isteği (NotificationServiceClient için). */
public record NotificationRequest(String userId, String type, String message) {}

