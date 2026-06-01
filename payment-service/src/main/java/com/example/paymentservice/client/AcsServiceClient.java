package com.example.paymentservice.client;

import com.example.paymentservice.dto.AcsInitRequest;
import com.example.paymentservice.dto.AcsInitResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Connects to ACS-service through Feign + Eureka (lb://).
 * Calls the ACS simulation service for the 3DS Challenge flow.
 */
@FeignClient(name = "acs-service", fallbackFactory = AcsServiceClientFallback.class)
public interface AcsServiceClient {

    @PostMapping("/acs/init")
    AcsInitResponse init(@RequestBody AcsInitRequest request);
}
