package com.example.authserver.dto;

/** Herkese açık kayıt endpoint'i için istek nesnesi. */
public record RegisterRequest(
        String username,
        String password,
        String email,
        String fullName) {}

