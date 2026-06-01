package com.example.frontend.model;

import java.util.Map;

/** Profil sayfası view-model'i. */
public record ProfilePageModel(
        Map<?, ?> profile,
        String    preferredUsername,
        String    email,
        int       cartCount,
        long      unreadCount,
        String    error
) implements PageModel {}

