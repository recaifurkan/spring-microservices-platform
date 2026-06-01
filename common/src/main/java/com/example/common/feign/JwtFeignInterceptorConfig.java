package com.example.common.feign;

import feign.Logger;
import feign.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.Executors;

/**
 * Feign çağrılarında JWT token'ını downstream servislere iletir.
 *
 * <p>Kullanım: {@code @EnableJwtFeignInterceptor}
 *
 * <p>SecurityContext propagation: Resilience4j TimeLimiter Feign çağrılarını
 * ayrı thread'de koşturmak ister; inline executor ile çağrı mevcut thread'de
 * koşturulur — SecurityContext ve OTel trace-id korunur.
 */
@Configuration
@Slf4j
public class JwtFeignInterceptorConfig {

    /**
     * Feign: method + url + headers + body + response — INFO seviyesinde loglanır.
     * YAML tarafında ilgili package DEBUG'a çekilmelidir:
     * logging.level.com.example: DEBUG
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor jwtPropagationInterceptor(
            ObjectProvider<Tracer> tracerProvider,
            ObjectProvider<Propagator> propagatorProvider) {
        return template -> {
            String traceId = MDC.get("traceId");
            try {
                // ── 0. OTel traceparent / tracestate injection ─────────────────────────
                // RestTemplateBuilder does this automatically with ObservationClientHttpRequestInterceptor.
                // In Feign, MicrometerObservationCapability only creates spans; it does not inject headers.
                // Therefore we inject them manually with the Propagator.
                Tracer tracer = tracerProvider.getIfAvailable();
                Propagator propagator = propagatorProvider.getIfAvailable();
                if (tracer != null && propagator != null) {
                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        propagator.inject(currentSpan.context(), template,
                                (t, key, value) -> t.header(key, value));
                        log.debug("[FeignJWT] traceparent injected → traceId={}", traceId);
                    } else {
                        log.warn("[FeignJWT] currentSpan null → traceparent EKLENEMEDİ → traceId={}", traceId);
                    }
                }

                // ── 1. SecurityContextHolder'dan JWT token ─────────────────────────────
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth instanceof JwtAuthenticationToken jwtAuth) {
                    template.header("Authorization", "Bearer " + jwtAuth.getToken().getTokenValue());
                    log.info("[FeignJWT] Authorization header eklendi (SecurityContext) → url={} traceId={} authType={}",
                            template.url(), traceId, auth.getClass().getSimpleName());
                    return;
                }

                // ── 2. Fallback: forward the incoming HTTP request's Authorization header ───
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String authHeader = request.getHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        template.header("Authorization", authHeader);
                        log.info("[FeignJWT] Authorization header eklendi (RequestHeader fallback) → url={} traceId={}",
                                template.url(), traceId);
                        return;
                    }
                }

                log.warn("[FeignJWT] Authorization header EKLENEMEDİ → url={} traceId={} auth={} requestAttrs={}",
                        template.url(), traceId,
                        auth != null ? auth.getClass().getSimpleName() : "null",
                        attrs != null ? "var" : "null (thread farklı olabilir!)");
            } catch (Exception e) {
                log.error("[FeignJWT] JWT propagation failed → url={} traceId={}", template.url(), traceId, e);
            }
        };
    }

    @Bean
    @ConditionalOnClass(Resilience4JCircuitBreakerFactory.class)
    public Customizer<Resilience4JCircuitBreakerFactory> securityContextCircuitBreakerExecutor() {
        return factory -> {
            log.info("[FeignJWT] Resilience4j executor → DelegatingSecurityContextExecutorService");
            factory.configureExecutorService(
                    new DelegatingSecurityContextExecutorService(
                            Executors.newCachedThreadPool(r -> {
                                Thread t = new Thread(r, "feign-cb-sec");
                                t.setDaemon(true);
                                return t;
                            })
                    )
            );
        };
    }
}

