package com.example.frontend.model;

import java.util.List;

/** Bildirimler sayfası view-model'i. */
public record NotificationsPageModel(
        List<?> notifications,
        long    unreadCount,
        int     cartCount,
        String  error
) implements PageModel {}

