package com.example.frontend.model;

/** Ödeme sonuç sayfası view-model'i. */
public record PaymentResultPageModel(
        String status,
        String paymentId,
        String threeDsType,
        int    cartCount,
        long   unreadCount
) implements PageModel {}

