package com.example.frontend.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String CB = "productService";

    private final RestTemplate authRestTemplate;
    private final RestTemplate publicRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public ProductService(@Qualifier("authRestTemplate")  RestTemplate authRestTemplate,
                          @Qualifier("publicRestTemplate") RestTemplate publicRestTemplate) {
        this.authRestTemplate   = authRestTemplate;
        this.publicRestTemplate = publicRestTemplate;
    }

    // ── public (no auth) ─────────────────────────────────────────────────────

    @CircuitBreaker(name = CB, fallbackMethod = "emptyList")
    @Retry(name = CB)
    public List<?> getFeaturedProducts() {
        List<?> r = publicRestTemplate.getForObject(gatewayUrl + "/api/products/featured", List.class);
        return r != null ? r.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyListWithInt")
    @Retry(name = CB)
    public List<?> getTopRatedProducts(int limit) {
        List<?> r = publicRestTemplate.getForObject(gatewayUrl + "/api/products/top-rated?limit=" + limit, List.class);
        return r != null ? r.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyListWithInt")
    @Retry(name = CB)
    public List<?> getNewestProducts(int limit) {
        List<?> r = publicRestTemplate.getForObject(gatewayUrl + "/api/products/newest?limit=" + limit, List.class);
        return r != null ? r.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyListWithString")
    @Retry(name = CB)
    public List<?> getSuggestions(String q) {
        String encoded = URLEncoder.encode(q, StandardCharsets.UTF_8);
        List<?> r = publicRestTemplate.getForObject(gatewayUrl + "/api/products/suggestions?q=" + encoded, List.class);
        return r != null ? r.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
    }

    // ── auth ─────────────────────────────────────────────────────────────────

    @CircuitBreaker(name = CB, fallbackMethod = "emptyList")
    @Retry(name = CB)
    public List<?> searchProducts(String search, String category, String sort, String minPrice, String maxPrice) {
        try {
            StringBuilder uri = new StringBuilder(gatewayUrl + "/api/products?");
            if (search   != null && !search.isBlank())   uri.append("search=").append(URLEncoder.encode(search, "UTF-8")).append("&");
            if (category != null && !category.isBlank()) uri.append("category=").append(URLEncoder.encode(category, "UTF-8")).append("&");
            if (sort     != null && !sort.isBlank())     uri.append("sort=").append(sort).append("&");
            if (minPrice != null && !minPrice.isBlank()) uri.append("minPrice=").append(minPrice).append("&");
            if (maxPrice != null && !maxPrice.isBlank()) uri.append("maxPrice=").append(maxPrice).append("&");
            List<?> r = authRestTemplate.getForObject(uri.toString(), List.class);
            return r != null ? r.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
        } catch (java.io.UnsupportedEncodingException e) {
            return List.of();
        }
    }

    @CircuitBreaker(name = CB, fallbackMethod = "nullMapWithLong")
    @Retry(name = CB)
    public Map<?, ?> getProduct(Long id) {
        return authRestTemplate.getForObject(gatewayUrl + "/api/products/" + id, Map.class);
    }

    @CircuitBreaker(name = CB, fallbackMethod = "emptyListWithLongAndInt")
    @Retry(name = CB)
    public List<?> getRelatedProducts(Long id, int limit) {
        List<?> r = authRestTemplate.getForObject(gatewayUrl + "/api/products/" + id + "/related?limit=" + limit, List.class);
        return r != null ? r.stream().filter(Objects::nonNull).collect(Collectors.toList()) : List.of();
    }

    // ── fallbacks ─────────────────────────────────────────────────────────────

    public List<?> emptyList(Throwable t) {
        log.warn("[productService] fallback: {}", t.getMessage());
        return List.of();
    }

    public List<?> emptyListWithInt(int limit, Throwable t) {
        log.warn("[productService] fallback limit={}: {}", limit, t.getMessage());
        return List.of();
    }

    public List<?> emptyListWithString(String q, Throwable t) {
        log.warn("[productService] fallback q={}: {}", q, t.getMessage());
        return List.of();
    }

    public List<?> emptyListWithLongAndInt(Long id, int limit, Throwable t) {
        log.warn("[productService] fallback id={}: {}", id, t.getMessage());
        return List.of();
    }

    public Map<?, ?> nullMapWithLong(Long id, Throwable t) {
        log.warn("[productService] fallback id={}: {}", id, t.getMessage());
        return null;
    }
}

