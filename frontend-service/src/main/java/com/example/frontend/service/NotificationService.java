package com.example.frontend.service;

import com.example.frontend.dto.UnreadCountResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private static final String CB = "notificationService";

    private final RestTemplate authRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public NotificationService(RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyList")
    @Retry(name = CB)
    public List<?> getNotifications() {
        List<?> r = authRestTemplate.getForObject(gatewayUrl + "/api/notifications", List.class);
        return r != null ? r : List.of();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "zeroCount")
    @Retry(name = CB)
    public long getUnreadCount() {
        UnreadCountResponse r = authRestTemplate.getForObject(
                gatewayUrl + "/api/notifications/unread-count", UnreadCountResponse.class);
        return r != null ? r.count() : 0L;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "markAllReadFallback")
    @Retry(name = CB)
    public void markAllRead() {
        authRestTemplate.exchange(gatewayUrl + "/api/notifications/read-all",
                HttpMethod.PUT, new HttpEntity<>(null), Void.class);
    }

    // ── fallbacks ─────────────────────────────────────────────────────────────

    public List<?> emptyList(Throwable t) {
        log.warn("[notificationService] getNotifications fallback: {}", t.getMessage());
        return List.of();
    }

    public long zeroCount(Throwable t) {
        log.warn("[notificationService] getUnreadCount fallback: {}", t.getMessage());
        return 0L;
    }

    public void markAllReadFallback(Throwable t) {
        log.warn("[notificationService] markAllRead fallback: {}", t.getMessage());
    }
}

