package com.example.frontend.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PageModelTest {

    @Test
    void givenRecordModelsWhenConstructedThenExposeValuesAndDefaults() {
        HomePageModel home = new HomePageModel(List.of("f"), List.of("t"), List.of("n"), 1);
        DashboardPageModel dashboard = new DashboardPageModel("name", "mail", 2, 3, List.of("o"), 4L, 5, List.of("r"));
        ProductsPageModel products = new ProductsPageModel(List.of("p"), "q", "cat", "sort", "1", "2", 6, 7L, "err", "ok");
        CartPageModel cart = new CartPageModel(List.of("i"), BigDecimal.TEN, 8, 9L, "err", "ok");
        CheckoutPageModel checkout = new CheckoutPageModel(List.of("i"), BigDecimal.ONE, 10, 11L, null);
        PaymentResultPageModel payment = new PaymentResultPageModel("SUCCESS", "p-1", "FRICTIONLESS", 12, 13L);
        ProfilePageModel profile = new ProfilePageModel(Map.of("k", "v"), "user", "mail", 14, 15L, null);
        NotificationsPageModel notifications = new NotificationsPageModel(List.of("n"), 16L, 17, "err");
        OrdersPageModel orders = new OrdersPageModel(List.of("o"), 18, 19L, "err", "ok");
        ProductDetailPageModel detail = new ProductDetailPageModel(Map.of("id", 1), List.of("r"), 20, 21L, null);
        OrderDetailPageModel orderDetail = new OrderDetailPageModel(Map.of("id", 2), Map.of("name", "x"), 22, 23L, null);

        assertThat(home.cartCount()).isEqualTo(1);
        assertThat(dashboard.unreadCount()).isEqualTo(4L);
        assertThat(products.category()).isEqualTo("cat");
        assertThat(cart.unreadCount()).isEqualTo(9L);
        assertThat(checkout.category()).isNull();
        assertThat(payment.status()).isEqualTo("SUCCESS");
        assertThat(profile.preferredUsername()).isEqualTo("user");
        assertThat(notifications.cartCount()).isEqualTo(17);
        assertThat(orders.success()).isEqualTo("ok");
        assertThat(detail.error()).isNull();
        assertThat(orderDetail.cartCount()).isEqualTo(22);
    }
}

