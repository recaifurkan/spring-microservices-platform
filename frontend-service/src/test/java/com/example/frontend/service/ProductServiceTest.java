package com.example.frontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private RestTemplate authRestTemplate;
    private RestTemplate publicRestTemplate;
    private ProductService service;

    @BeforeEach
    void setUp() {
        authRestTemplate = mock(RestTemplate.class);
        publicRestTemplate = mock(RestTemplate.class);
        service = new ProductService(authRestTemplate, publicRestTemplate);
        ReflectionTestUtils.setField(service, "gatewayUrl", "http://gw");
    }

    @Test
    void givenFeaturedProductsWhenRequestedThenFiltersNullValues() {
        when(publicRestTemplate.getForObject("http://gw/api/products/featured", List.class)).thenReturn(Arrays.asList(Map.of("id", 1), null));

        assertThat(service.getFeaturedProducts()).hasSize(1);
    }

    @Test
    void givenTopRatedWhenRequestedThenUsesLimitAndFiltersNulls() {
        when(publicRestTemplate.getForObject("http://gw/api/products/top-rated?limit=4", List.class))
                .thenReturn(Arrays.asList(Map.of("id", 1), null));

        assertThat(service.getTopRatedProducts(4)).hasSize(1);
    }

    @Test
    void givenNewestWhenRequestedThenUsesLimit() {
        when(publicRestTemplate.getForObject("http://gw/api/products/newest?limit=2", List.class))
                .thenReturn(List.of(Map.of("id", 1)));

        assertThat(service.getNewestProducts(2)).hasSize(1);
    }

    @Test
    void givenSuggestionsWhenRequestedThenEncodesQuery() {
        when(publicRestTemplate.getForObject("http://gw/api/products/suggestions?q=phone+case", List.class))
                .thenReturn(List.of(Map.of("id", 1)));

        assertThat(service.getSuggestions("phone case")).hasSize(1);
    }

    @Test
    void givenSearchCriteriaWhenRequestedThenBuildsQueryString() {
        when(authRestTemplate.getForObject("http://gw/api/products?search=phone&category=electronics&sort=price&minPrice=10&maxPrice=99&", List.class))
                .thenReturn(List.of(Map.of("id", 1)));

        assertThat(service.searchProducts("phone", "electronics", "price", "10", "99")).hasSize(1);
    }

    @Test
    void givenProductIdWhenRequestedThenReturnsMap() {
        when(authRestTemplate.getForObject("http://gw/api/products/7", Map.class)).thenReturn(Map.of("id", 7));
        assertThat(service.getProduct(7L).get("id")).isEqualTo(7);
    }

    @Test
    void givenRelatedProductsWhenRequestedThenUsesIdAndLimit() {
        when(authRestTemplate.getForObject("http://gw/api/products/7/related?limit=3", List.class))
                .thenReturn(List.of(Map.of("id", 8)));
        assertThat(service.getRelatedProducts(7L, 3)).hasSize(1);
    }

    @Test
    void givenFallbackWhenInvokedThenReturnsSafeDefaults() {
        assertThat(service.emptyList(new RuntimeException("x"))).isEmpty();
        assertThat(service.emptyListWithInt(1, new RuntimeException("x"))).isEmpty();
        assertThat(service.emptyListWithString("q", new RuntimeException("x"))).isEmpty();
        assertThat(service.emptyListWithLongAndInt(1L, 2, new RuntimeException("x"))).isEmpty();
        assertThat(service.nullMapWithLong(1L, new RuntimeException("x"))).isNull();
    }
}

