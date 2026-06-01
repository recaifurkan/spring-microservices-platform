package com.example.authserver.dto;

import com.example.authserver.model.AppUser;

import java.util.Set;

/** Kullanıcı yanıt nesnesi — şifre asla dönmez. */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        boolean enabled,
        Set<String> grants) {

    public static UserResponse from(AppUser u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(),
                u.getFullName(), u.isEnabled(), u.getGrants());
    }
}
