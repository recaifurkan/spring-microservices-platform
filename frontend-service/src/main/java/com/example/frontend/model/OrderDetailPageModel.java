package com.example.frontend.model;

import java.util.Map;

/** Sipariş detay sayfası view-model'i. */
public record OrderDetailPageModel(
        Map<?, ?> order,
        Map<?, ?> product,
        int       cartCount,
        long      unreadCount,
        String    error
) implements PageModel {}

