package com.example.notificationservice.dto;

/** Servisler arası bildirim gönderme isteği. */
public record SendNotificationRequest(String userId, String type, String message) {}

