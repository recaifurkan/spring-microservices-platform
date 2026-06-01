package com.example.frontend.model;

import java.util.List;

/** Siparişlerim sayfası view-model'i. */
public record OrdersPageModel(
        List<?> orders,
        int     cartCount,
        long    unreadCount,
        String  error,
        String  success
) implements PageModel {}

