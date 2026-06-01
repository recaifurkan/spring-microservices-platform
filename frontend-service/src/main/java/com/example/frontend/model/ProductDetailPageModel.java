package com.example.frontend.model;

import java.util.List;
import java.util.Map;

/** Ürün detay sayfası view-model'i. */
public record ProductDetailPageModel(
        Map<?, ?> product,
        List<?>   related,
        int       cartCount,
        long      unreadCount,
        String    error
) implements PageModel {}

