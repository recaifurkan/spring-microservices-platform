package com.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global filter that adds JWT claims to downstream headers.
 * NOTE: Spring WebFlux request headers are read-only,
 * so a mutable headers copy is created with ServerHttpRequestDecorator.
 */
@Component
public class JwtHeaderPropagationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtHeaderPropagationFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap(ctx -> {
                if (ctx.getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
                    Jwt jwt = jwtAuth.getToken();

                    // Mutable headers copy — the original request headers are read-only
                    HttpHeaders mutableHeaders = new HttpHeaders();
                    mutableHeaders.addAll(exchange.getRequest().getHeaders());
                    mutableHeaders.set("X-User-Id",   jwt.getSubject());
                    mutableHeaders.set("X-User-Name", jwt.getSubject());
                    mutableHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.getTokenValue());
                    if (jwt.getClaimAsString("email") != null)
                        mutableHeaders.set("X-User-Email", jwt.getClaimAsString("email"));

                    ServerHttpRequest decorated = new ServerHttpRequestDecorator(exchange.getRequest()) {
                        @Override
                        public HttpHeaders getHeaders() {
                            return mutableHeaders;
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).build());
                }
                return chain.filter(exchange);
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() { return -100; }
}
