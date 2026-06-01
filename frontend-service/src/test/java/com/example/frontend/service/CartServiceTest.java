package com.example.frontend.service;

import com.example.frontend.dto.AddItemRequest;
import com.example.frontend.dto.CartCountResponse;
import com.example.frontend.dto.CartResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceTest {

    private RestTemplate authRestTemplate;
    private CartService service;

    @BeforeEach
    void setUp() {
        authRestTemplate = mock(RestTemplate.class);
        service = new CartService(authRestTemplate);
        ReflectionTestUtils.setField(service, "gatewayUrl", "http://gw");
    }

    @Test
    void givenCartWhenRequestedThenReturnsCartAndCount() {
        CartResponse response = new CartResponse(List.of(), new BigDecimal("12.34"), 2);
        when(authRestTemplate.getForObject("http://gw/api/cart", CartResponse.class)).thenReturn(response);
        when(authRestTemplate.getForObject("http://gw/api/cart/count", CartCountResponse.class)).thenReturn(new CartCountResponse(2));

        assertThat(service.getCart()).isEqualTo(response);
        assertThat(service.getCartCount()).isEqualTo(2);
    }

    @Test
    void givenMutationCallsWhenInvokedThenUsesJsonRequests() {
        service.addItem(new AddItemRequest(1L, 2));
        service.updateItem(1L, 3);
        service.removeItem(1L);
        service.clearCart();

        verify(authRestTemplate).postForObject(eq("http://gw/api/cart/items"), any(HttpEntity.class), eq(java.util.Map.class));
        verify(authRestTemplate).exchange(eq("http://gw/api/cart/items/1"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));
        verify(authRestTemplate).delete("http://gw/api/cart/items/1");
        verify(authRestTemplate).delete("http://gw/api/cart");
    }

    @Test
    void givenFallbacksWhenCalledThenReturnSafeValues() {
        assertThat(service.emptyCart(new RuntimeException("x"))).isNull();
        assertThat(service.zeroCount(new RuntimeException("x"))).isZero();
    }
}

