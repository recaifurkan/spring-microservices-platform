package com.example.common.feign;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Feign çağrılarında JWT token'ını downstream servislere ileten interceptor'ı aktif eder.
 *
 * <p>Feign kullanan her servisin main class'ına veya herhangi bir {@code @Configuration}'a ekle:
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableFeignClients
 * @EnableJwtFeignInterceptor
 * public class OrderServiceApplication { ... }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(JwtFeignInterceptorConfig.class)
public @interface EnableJwtFeignInterceptor {
}

