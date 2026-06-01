package com.example.frontend.model;

import java.util.List;

/** Ürün listeleme sayfası view-model'i. */
public record ProductsPageModel(
        List<?> products,
        String  search,
        String  category,
        String  sort,
        String  minPrice,
        String  maxPrice,
        int     cartCount,
        long    unreadCount,
        String  error,
        String  success
) implements PageModel {}

