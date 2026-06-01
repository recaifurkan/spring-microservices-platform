package com.example.frontend.model;

import java.util.List;

/** Dashboard sayfası view-model'i. */
public record DashboardPageModel(
        String  userName,
        String  email,
        int     orderCount,
        int     paymentCount,
        List<?> recentOrders,
        long    unreadCount,
        int     cartCount,
        List<?> recommendations
) implements PageModel {}

