package com.example.common.feign;

import feign.Logger;
import feign.RequestTemplate;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtFeignInterceptorConfigTest {

    private final JwtFeignInterceptorConfig config = new JwtFeignInterceptorConfig();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void givenNoSpecialContextWhenLoggerLevelRequestedThenReturnsFull() {
        assertThat(config.feignLoggerLevel()).isEqualTo(Logger.Level.FULL);
    }

    @Test
    void givenJwtAuthenticationWhenInterceptThenAddsAuthorizationAndTraceHeaders() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        Propagator propagator = mock(Propagator.class);
        RequestTemplate template = new RequestTemplate();
        template.uri("/downstream");

        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(mock(io.micrometer.tracing.TraceContext.class));
        doAnswer(inv -> {
            Propagator.Setter<RequestTemplate> setter = inv.getArgument(2);
            setter.set(inv.getArgument(1), "traceparent", "00-abc-123-01");
            return null;
        }).when(propagator).inject(any(), any(), any());

        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(
                        org.springframework.security.oauth2.jwt.Jwt.withTokenValue("jwt-token")
                                .header("alg", "none")
                                .subject("user-1")
                                .build()));

        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        ObjectProvider<Propagator> propagatorProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(tracer);
        when(propagatorProvider.getIfAvailable()).thenReturn(propagator);

        config.jwtPropagationInterceptor(tracerProvider, propagatorProvider).apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer jwt-token");
        assertThat(template.headers().get("traceparent")).containsExactly("00-abc-123-01");
        verify(propagator).inject(any(), any(), any());
    }

    @Test
    void givenIncomingRequestAuthorizationWhenNoJwtAuthenticationThenFallbackHeaderIsForwarded() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer fallback-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, new MockHttpServletResponse()));

        RequestTemplate template = new RequestTemplate();
        template.uri("/downstream");

        ObjectProvider<Tracer> tracerProvider = mock(ObjectProvider.class);
        ObjectProvider<Propagator> propagatorProvider = mock(ObjectProvider.class);
        when(tracerProvider.getIfAvailable()).thenReturn(null);
        when(propagatorProvider.getIfAvailable()).thenReturn(null);

        config.jwtPropagationInterceptor(tracerProvider, propagatorProvider).apply(template);

        assertThat(template.headers().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer fallback-token");
    }

    @Test
    void givenResilienceFactoryWhenConfiguredThenExecutorServiceIsSet() {
        Resilience4JCircuitBreakerFactory factory = mock(Resilience4JCircuitBreakerFactory.class);

        config.securityContextCircuitBreakerExecutor().customize(factory);

        verify(factory).configureExecutorService(any(ExecutorService.class));
    }
}

