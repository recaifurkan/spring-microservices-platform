package com.example.cart.client;

import com.example.cart.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** JWT propagation → JwtFeignInterceptorConfig (common). */
@FeignClient(name = "product-service", fallbackFactory = ProductServiceClientFallback.class)
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable Long id);
}
