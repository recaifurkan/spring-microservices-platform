package com.example.frontend.model;

import java.util.List;

/** Ödemelerim sayfası view-model'i. */
public record PaymentsPageModel(
        List<?> payments,
        int     cartCount,
        long    unreadCount,
        String  error
) implements PageModel {}

