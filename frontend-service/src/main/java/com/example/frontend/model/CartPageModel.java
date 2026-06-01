package com.example.frontend.model;

import java.util.List;

/** Sepet sayfası view-model'i. */
public record CartPageModel(
        List<?> items,
        Object  total,
        int     cartCount,
        long    unreadCount,
        String  error,
        String  success
) implements PageModel {}

