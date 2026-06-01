package com.example.cart.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductServiceClientFallbackTest {

    @Test
    void givenFailureWhenFallbackCreatedThenReturnsNullClientResult() {
        ProductServiceClientFallback fallback = new ProductServiceClientFallback();

        assertThat(fallback.create(new RuntimeException("boom")).getProduct(10L)).isNull();
    }
}

