package com.example.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FrontendServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FrontendServiceApplication.class, args);
    }
}
