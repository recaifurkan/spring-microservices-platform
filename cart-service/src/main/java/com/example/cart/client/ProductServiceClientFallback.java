package com.example.cart.client;

import com.example.cart.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceClientFallback implements FallbackFactory<ProductServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceClientFallback.class);

    @Override
    public ProductServiceClient create(Throwable cause) {
        log.warn("[ProductServiceClient] Circuit breaker devrede: {}", cause.getMessage());
        return id -> {
            log.warn("[ProductServiceClient] getProduct({}) fallback", id);
            return null;
        };
    }
}

