package com.example.frontend.dto;

/** Kayıt isteği için frontend request nesnesi. */
public record RegisterRequest(String username, String password, String email, String fullName) {}

