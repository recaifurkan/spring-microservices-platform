package com.example.frontend.service;

import com.example.frontend.dto.InitiatePaymentRequest;
import com.example.frontend.dto.PaymentInitiateResponse;
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

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final String CB = "paymentService";

    private final RestTemplate authRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public PaymentService(RestTemplate authRestTemplate) {
        this.authRestTemplate = authRestTemplate;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyList")
    @Retry(name = CB)
    public List<?> getPayments() {
        try {
            List<?> r = authRestTemplate.getForObject(gatewayUrl + "/api/payments", List.class);
            return r != null ? r : List.of();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) return List.of();
            throw e;
        }
    }

    @CircuitBreaker(name = CB, fallbackMethod = "nullPayment")
    @Retry(name = CB)
    public PaymentInitiateResponse initiatePayment(InitiatePaymentRequest req) {
        return authRestTemplate.postForObject(gatewayUrl + "/api/payments/initiate",
                new HttpEntity<>(req, jsonHeaders()), PaymentInitiateResponse.class);
    }

    // ── fallbacks ─────────────────────────────────────────────────────────────

    public List<?> emptyList(Throwable t) {
        log.warn("[paymentService] getPayments fallback: {}", t.getMessage());
        return List.of();
    }

    public PaymentInitiateResponse nullPayment(InitiatePaymentRequest req, Throwable t) {
        log.warn("[paymentService] initiatePayment fallback: {}", t.getMessage());
        return null;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}

