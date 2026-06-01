package com.example.paymentservice.client;

import com.example.paymentservice.dto.AcsInitRequest;
import com.example.paymentservice.dto.AcsInitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AcsServiceClientFallback implements FallbackFactory<AcsServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(AcsServiceClientFallback.class);

    @Override
    public AcsServiceClient create(Throwable cause) {
        log.warn("[AcsServiceClient] Circuit breaker devrede: {}", cause.getMessage());
        return request -> {
            log.warn("[AcsServiceClient] init({}) fallback – ACS servisi yanıt vermiyor", request.paymentId());
            return null;
        };
    }
}

