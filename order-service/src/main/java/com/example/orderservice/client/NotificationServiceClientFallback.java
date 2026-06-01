package com.example.orderservice.client;

import com.example.orderservice.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class NotificationServiceClientFallback implements FallbackFactory<NotificationServiceClient> {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClientFallback.class);

    @Override
    public NotificationServiceClient create(Throwable cause) {
        log.warn("[NotificationServiceClient] Circuit breaker devrede: {}", cause.getMessage());
        return request -> log.warn("[NotificationServiceClient] send({}) fallback – bildirim gönderilemedi", request);
    }
}

