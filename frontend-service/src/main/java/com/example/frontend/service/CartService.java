package com.example.frontend.service;

import com.example.frontend.dto.AddItemRequest;
import com.example.frontend.dto.CartCountResponse;
import com.example.frontend.dto.CartResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);
    private static final String CB = "cartService";

    private final RestTemplate authRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public CartService(RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyCart")
    @Retry(name = CB)
    public CartResponse getCart() {
        return authRestTemplate.getForObject(gatewayUrl + "/api/cart", CartResponse.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "zeroCount")
    @Retry(name = CB)
    public int getCartCount() {
        CartCountResponse r = authRestTemplate.getForObject(gatewayUrl + "/api/cart/count", CartCountResponse.class);
        return r != null ? (int) r.count() : 0;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "addItemFallback")
    @Retry(name = CB)
    public void addItem(AddItemRequest req) {
        authRestTemplate.postForObject(gatewayUrl + "/api/cart/items",
                new HttpEntity<>(req, jsonHeaders()), Map.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "updateItemFallback")
    @Retry(name = CB)
    public void updateItem(Long productId, int quantity) {
        authRestTemplate.exchange(gatewayUrl + "/api/cart/items/" + productId,
                HttpMethod.PUT, new HttpEntity<>(Map.of("quantity", quantity), jsonHeaders()), Void.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "removeItemFallback")
    @Retry(name = CB)
    public void removeItem(Long productId) {
        authRestTemplate.delete(gatewayUrl + "/api/cart/items/" + productId);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "clearFallback")
    @Retry(name = CB)
    public void clearCart() {
        authRestTemplate.delete(gatewayUrl + "/api/cart");
    }

    // ── fallbacks ─────────────────────────────────────────────────────────────

    public CartResponse emptyCart(Throwable t) {
        log.warn("[cartService] getCart fallback: {}", t.getMessage());
        return null;
    }

    public int zeroCount(Throwable t) {
        log.warn("[cartService] getCartCount fallback: {}", t.getMessage());
        return 0;
    }

    public void addItemFallback(AddItemRequest req, Throwable t) {
        log.warn("[cartService] addItem fallback: {}", t.getMessage());
    }

    public void updateItemFallback(Long productId, int quantity, Throwable t) {
        log.warn("[cartService] updateItem fallback: {}", t.getMessage());
    }

    public void removeItemFallback(Long productId, Throwable t) {
        log.warn("[cartService] removeItem fallback: {}", t.getMessage());
    }

    public void clearFallback(Throwable t) {
        log.warn("[cartService] clearCart fallback: {}", t.getMessage());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}

