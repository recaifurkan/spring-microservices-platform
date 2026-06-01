package com.example.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/** JWT propagation → JwtFeignInterceptorConfig (common). */
@FeignClient(name = "order-service", fallbackFactory = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @PostMapping("/api/orders/{id}/confirm-payment")
    void confirmPayment(@PathVariable("id") Long orderId);
}
