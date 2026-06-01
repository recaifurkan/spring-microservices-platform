package com.example.frontend.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate bean tanımları.
 *
 * RestTemplateBuilder kullanılarak oluşturulan her bean,
 * Spring Boot'un ObservationClientHttpRequestInterceptor'ını otomatik alır.
 * Bu interceptor:
 *   1. Her giden HTTP isteğini için child-span oluşturur.
 *   2. W3C 'traceparent' başlığını downstream servislere iletir
 *      → uçtan uca distributed tracing çalışır.
 */
@Configuration
public class WebClientConfig {

    /**
     * Kimlik doğrulama gerektiren servis çağrıları için.
     * BearerTokenInterceptor OAuth2 access_token'ı Authorization header'ına ekler.
     */
    @Bean("authRestTemplate")
    RestTemplate authRestTemplate(RestTemplateBuilder builder,
                                  BearerTokenInterceptor bearerTokenInterceptor) {
        return builder
                .additionalInterceptors(bearerTokenInterceptor)
                .build();
    }

    /**
     * Public endpoint çağrıları için (kayıt, ürün listesi, kategori cache vb.).
     * Auth header yok; trace propagation aktif.
     */
    @Bean("publicRestTemplate")
    RestTemplate publicRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
