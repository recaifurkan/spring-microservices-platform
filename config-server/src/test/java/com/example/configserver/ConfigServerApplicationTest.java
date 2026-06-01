package com.example.configserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigServerApplicationTest {

    @Test
    void givenApplicationClassWhenInspectedThenItIsAnnotatedAsSpringBootApplication() {
        assertThat(ConfigServerApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }
}

