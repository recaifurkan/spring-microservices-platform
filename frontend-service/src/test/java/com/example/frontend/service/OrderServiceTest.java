package com.example.frontend.service;

import com.example.frontend.dto.CreateFromCartRequest;
import com.example.frontend.dto.OrderItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private RestTemplate authRestTemplate;
    private OrderService service;

    @BeforeEach
    void setUp() {
        authRestTemplate = mock(RestTemplate.class);
        service = new OrderService(authRestTemplate);
        ReflectionTestUtils.setField(service, "gatewayUrl", "http://gw");
    }

    @Test
    void givenOrdersWhenRequestedThenHandlesNotFoundAsEmptyList() {
        when(authRestTemplate.getForObject("http://gw/api/orders", List.class))
                .thenThrow(HttpClientErrorException.create(org.springframework.http.HttpStatus.NOT_FOUND, "NF", org.springframework.http.HttpHeaders.EMPTY, new byte[0], null));

        assertThat(service.getOrders()).isEmpty();
    }

    @Test
    void givenOrderWhenRequestedThenReturnsMap() {
        when(authRestTemplate.getForObject("http://gw/api/orders/5", Map.class)).thenReturn(Map.of("id", 5));
        assertThat(service.getOrder(5L).get("id")).isEqualTo(5);
    }

    @Test
    void givenCreateFromCartWhenCalledThenPostsJsonBody() {
        CreateFromCartRequest req = new CreateFromCartRequest(List.of(new OrderItemRequest(1L, 2)));
        when(authRestTemplate.postForObject(eq("http://gw/api/orders/from-cart"), any(HttpEntity.class), eq(List.class)))
                .thenReturn(List.of(Map.of("id", 7)));

        assertThat(service.createFromCart(req)).hasSize(1);
        verify(authRestTemplate).postForObject(eq("http://gw/api/orders/from-cart"), any(HttpEntity.class), eq(List.class));
    }

    @Test
    void givenCreateAndCancelWhenCalledThenSendsMutatingRequests() {
        service.createOrder(1L, 2);
        service.cancelOrder(9L);

        verify(authRestTemplate).postForObject(eq("http://gw/api/orders"), any(HttpEntity.class), eq(Map.class));
        verify(authRestTemplate).exchange(eq("http://gw/api/orders/9/cancel"), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void givenFallbacksWhenCalledThenReturnSafeValues() {
        assertThat(service.emptyList(new RuntimeException("x"))).isEmpty();
        assertThat(service.nullMapWithLong(1L, new RuntimeException("x"))).isNull();
        assertThat(service.emptyListWithRequest(new CreateFromCartRequest(List.of()), new RuntimeException("x"))).isEmpty();
    }
}

