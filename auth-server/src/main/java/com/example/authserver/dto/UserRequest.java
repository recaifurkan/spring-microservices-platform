package com.example.authserver.dto;

import java.util.Set;

/**
 * Yeni kullanıcı oluşturma / güncelleme isteği.
 * grants örnekleri: SCOPE_read, SCOPE_write, ROLE_ADMIN, ROLE_MODERATOR
 */
public record UserRequest(
        String username,
        String password,
        String email,
        String fullName,
        Boolean enabled,
        Set<String> grants) {}
