package com.example.orderservice;
import com.example.common.feign.EnableJwtFeignInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication @EnableFeignClients @EnableJwtFeignInterceptor
public class OrderServiceApplication { public static void main(String[] args) { SpringApplication.run(OrderServiceApplication.class, args); } }

