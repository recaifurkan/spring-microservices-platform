package com.example.orderservice.client;

import com.example.orderservice.dto.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductStockClientFallback implements FallbackFactory<ProductStockClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductStockClientFallback.class);

    @Override
    public ProductStockClient create(Throwable cause) {
        log.warn("[ProductStockClient] Circuit breaker devrede: {}", cause.getMessage());
        return new ProductStockClient() {
            @Override
            public ProductResponse decreaseStock(Long id, int quantity) {
                log.warn("[ProductStockClient] decreaseStock({}, {}) fallback", id, quantity);
                return null;
            }

            @Override
            public ProductResponse increaseStock(Long id, int quantity) {
                log.warn("[ProductStockClient] increaseStock({}, {}) fallback", id, quantity);
                return null;
            }
        };
    }
}

