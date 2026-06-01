package com.example.userservice.dto;

/** Kullanıcı profili oluşturma/güncelleme isteği. */
public record UpsertProfileRequest(
        String username,
        String email,
        String fullName,
        String bio,
        String avatarUrl) {}

