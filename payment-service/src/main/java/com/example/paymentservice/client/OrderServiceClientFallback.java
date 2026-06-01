package com.example.paymentservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderServiceClientFallback implements FallbackFactory<OrderServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClientFallback.class);

    @Override
    public OrderServiceClient create(Throwable cause) {
        log.warn("[OrderServiceClient] Circuit breaker devrede: {}", cause.getMessage());
        return orderId -> log.warn("[OrderServiceClient] confirmPayment({}) fallback – sipariş onaylanamadı", orderId);
    }
}

