package com.example.discoveryserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryServerApplicationTest {

    @Test
    void givenApplicationClassWhenInspectedThenItIsAnnotatedAsSpringBootApplication() {
        assertThat(DiscoveryServerApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
    }
}

