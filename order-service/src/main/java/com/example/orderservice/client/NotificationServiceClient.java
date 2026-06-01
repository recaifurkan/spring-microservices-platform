package com.example.orderservice.client;

import com.example.orderservice.dto.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** JWT propagation → JwtFeignInterceptorConfig (common). */
@FeignClient(name = "notification-service", fallbackFactory = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/send")
    void send(@RequestBody NotificationRequest request);
}
