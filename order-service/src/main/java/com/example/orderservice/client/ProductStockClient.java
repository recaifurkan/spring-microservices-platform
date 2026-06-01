package com.example.orderservice.client;

import com.example.orderservice.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Stock operations. JWT propagation → JwtFeignInterceptorConfig (common). */
@FeignClient(name = "product-service-stock",
             url = "${product.service.url:http://localhost:8084}",
             fallbackFactory = ProductStockClientFallback.class)
public interface ProductStockClient {

    @PatchMapping("/api/products/{id}/stock/decrease")
    ProductResponse decreaseStock(@PathVariable Long id, @RequestParam int quantity);

    @PatchMapping("/api/products/{id}/stock/increase")
    ProductResponse increaseStock(@PathVariable Long id, @RequestParam int quantity);
}
