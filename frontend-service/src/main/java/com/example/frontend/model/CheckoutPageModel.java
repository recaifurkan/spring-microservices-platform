package com.example.frontend.model;

import java.util.List;

/** Ödeme adımı sayfası view-model'i. */
public record CheckoutPageModel(
        List<?> items,
        Object  total,
        int     cartCount,
        long    unreadCount,
        String  error
) implements PageModel {}

