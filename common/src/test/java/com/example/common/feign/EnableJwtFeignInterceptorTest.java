package com.example.common.feign;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class EnableJwtFeignInterceptorTest {

    @EnableJwtFeignInterceptor
    static class EnabledApp {}

    @Test
    void givenAnnotationWhenContextStartsThenInterceptorConfigIsImported() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EnabledApp.class)) {
            assertThat(context.getBean(JwtFeignInterceptorConfig.class)).isNotNull();
        }
    }
}

