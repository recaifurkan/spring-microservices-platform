package com.example.frontend.service;

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
public class UserProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserProfileService.class);
    private static final String CB = "userProfileService";

    private final RestTemplate authRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public UserProfileService(RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyProfile")
    @Retry(name = CB)
    public Map<?, ?> getProfile() {
        return authRestTemplate.getForObject(gatewayUrl + "/api/users/profile", Map.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "updateProfileFallback")
    @Retry(name = CB)
    public void updateProfile(Map<String, Object> data) {
        authRestTemplate.postForObject(gatewayUrl + "/api/users/profile",
                new HttpEntity<>(data, jsonHeaders()), Map.class);
    }

    // ── fallbacks ─────────────────────────────────────────────────────────────

    public Map<?, ?> emptyProfile(Throwable t) {
        log.warn("[userProfileService] getProfile fallback: {}", t.getMessage());
        return Map.of();
    }

    public void updateProfileFallback(Map<String, Object> data, Throwable t) {
        log.warn("[userProfileService] updateProfile fallback: {}", t.getMessage());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}

