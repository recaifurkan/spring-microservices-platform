package com.example.frontend.service;

import com.example.frontend.dto.CreateFromCartRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final String CB = "orderService";

    private final RestTemplate authRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public OrderService(RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyList")
    @Retry(name = CB)
    public List<?> getOrders() {
        try {
            List<?> r = authRestTemplate.getForObject(gatewayUrl + "/api/orders", List.class);
            return r != null ? r : List.of();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) return List.of();
            throw e;
        }
    }

    @CircuitBreaker(name = CB, fallbackMethod = "nullMapWithLong")
    @Retry(name = CB)
    public Map<?, ?> getOrder(Long id) {
        return authRestTemplate.getForObject(gatewayUrl + "/api/orders/" + id, Map.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyListWithRequest")
    @Retry(name = CB)
    public List<?> createFromCart(CreateFromCartRequest req) {
        List<?> r = authRestTemplate.postForObject(gatewayUrl + "/api/orders/from-cart",
                new HttpEntity<>(req, jsonHeaders()), List.class);
        return r != null ? r : List.of();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "createOrderFallback")
    @Retry(name = CB)
    public void createOrder(Long productId, int quantity) {
        authRestTemplate.postForObject(gatewayUrl + "/api/orders",
                new HttpEntity<>(Map.of("productId", productId, "quantity", quantity), jsonHeaders()), Map.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "cancelOrderFallback")
    @Retry(name = CB)
    public void cancelOrder(Long id) {
        authRestTemplate.exchange(gatewayUrl + "/api/orders/" + id + "/cancel",
                HttpMethod.PUT, new HttpEntity<>(null, jsonHeaders()), Map.class);
    }

    // ── fallbacks ─────────────────────────────────────────────────────────────

    public List<?> emptyList(Throwable t) {
        log.warn("[orderService] getOrders fallback: {}", t.getMessage());
        return List.of();
    }

    public Map<?, ?> nullMapWithLong(Long id, Throwable t) {
        log.warn("[orderService] getOrder fallback id={}: {}", id, t.getMessage());
        return null;
    }

    public List<?> emptyListWithRequest(CreateFromCartRequest req, Throwable t) {
        log.warn("[orderService] createFromCart fallback: {}", t.getMessage());
        return List.of();
    }

    public void createOrderFallback(Long productId, int quantity, Throwable t) {
        log.warn("[orderService] createOrder fallback: {}", t.getMessage());
    }

    public void cancelOrderFallback(Long id, Throwable t) {
        log.warn("[orderService] cancelOrder fallback id={}: {}", id, t.getMessage());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}

